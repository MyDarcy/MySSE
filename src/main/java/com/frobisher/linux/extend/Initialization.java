package com.frobisher.linux.extend;


import Jama.Matrix;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/*
 * author: darcy
 * date: 2017/11/7 16:53
 * description: 
*/
public class Initialization {

	public int lengthOfDict;
	public List<String> dict;

	// 从文档中提取的关键词的数目
	public int DICTIONARY_SIZE;
	// 添加用于混淆的冗余关键词的数目
	public int DUMMY_KEYWORD_NUMBER = 10;
	public int DOC_NUMBER = 16;

	// 项目目录. 密钥目录. 明文文件目录. 密文文件目录. 40个文件
	public static String BASE = "D:\\MrDarcy\\ForGraduationWorks\\Code\\SSE";
	public static String SECRET_KEY_DIR = BASE + "\\doc\\muse\\extend\\key\\aesKey.dat";
	public String BASE_PLAIN_DIR = BASE + "\\doc\\muse\\extend\\plain";
	public String BASE_ENCRYPTED_DIR = BASE + "\\doc\\muse\\extend\\encrypted";
	public String PLAIN_DIR = BASE_PLAIN_DIR + DOC_NUMBER;
	public String ENCRYPTED_DIR = BASE_ENCRYPTED_DIR + DOC_NUMBER;

	// linux下.
/*	public static *//*final*//* String BASE = "/home/zqhe/data";
	public static *//*final*//* String SECRET_KEY_DIR = BASE + "/doc/muse/extend/key/aesKey.dat";
	public static *//*final*//* String PLAIN_DIR = BASE + "/doc/muse/extend/plain100";
	public static *//*final*//* String ENCRYPTED_DIR = BASE + "/doc/muse/extend/encrypted100";*/

	// 明文文件目录 	密文文件目录. 16个文件
	// doc/splitting/cnn_splitting40_10
	/*public static final String PLAIN_DIR = BASE + "\\doc\\muse\\extend\\plain40";
	public static final String ENCRYPTED_DIR = BASE + "\\doc\\muse\\extend\\encrypted40";*/

	/*public static final String PLAIN_DIR = BASE + "\\doc\\splitting\\cnn_splitting40_10";
	public static final String ENCRYPTED_DIR = BASE + "\\doc\\muse\\extend\\encrypted40";*/

	public static String SEPERATOR;
	// 匹配关键词
	public static final Pattern WORD_PATTERN = Pattern.compile("\\w+");
	public static final Random RANDOM = new Random(System.currentTimeMillis());

	// 加密原语等.
	public static Cipher cipher;
	public static KeyGenerator keyGenerator;
	public static SecretKey secretKey;

	// 包含指定的关键词的文档的数目.
	public Map<String, Integer> numberOfDocumentContainsKeyword = new HashMap<>();
	// 统计所有文档的长度. 这里可以使用list, 即使当前有多少个文档并不知情.
	public Map<String, Integer> fileLength = new HashMap<>();
	// keyword在document中出现的频率. 这里也可以使用Map<Map>来记录有文档中包含的特定的关键词有多少个.
	// public static int[][] keywordFrequency;
	// filename -> {keyword: count}
	public Map<String, Map<String, Integer>> keywordFrequencyInDocument = new HashMap<>();

	public List<String> extendDummyDict;

	static{
		SEPERATOR = "\\";
		if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
			SEPERATOR = "/";
		}
	}

	// 实例初始化块
	{
		if (!new File(ENCRYPTED_DIR).exists()) {
			new File(ENCRYPTED_DIR).mkdir();
		}
	}

	/**
	 * 主要是为了密钥的生成。
	 */
	static {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			cipher = Cipher.getInstance("AES"); //  DES/CBC/NoPadding
		 /*
		方法getInstance（ ）的参数为字符串类型，指定加密算法的名称。
		可以是 “Blowfish”、“DES”、“DESede”、“HmacMD5”或“HmacSHA1”等。
		其中“DES”是目前最常用的对称加密算法，但安全性较差。针对DES安全性
		的改进产生了能满足当前安全需要的TripleDES算法，即“DESede”。“Blowfish”
		的密钥长度可达448位，安全性很好。“AES”是一种替代DES算法的新算法，
		可提供很好的安全性。
		 */
			keyGenerator = KeyGenerator.getInstance("AES");
		/*
		该步骤一般指定密钥的长度。如果该步骤省略的话，会根据算法自动使用默认的密钥长度。
		指定长度时，若第一步密钥生成器使用的是“DES”算法，则密钥长度必须是56位；
		若是“DESede”，则可以是112或168位，其中112位有效；若是“AES”，可以是
		128, 192或256位；若是“Blowfish”，则可以是32至448之间可以被8整除的数；
		“HmacMD5”和“HmacSHA1”默认的密钥长度都是64个字节。
		 */
			keyGenerator.init(128);

			// 存储密钥的文件存在.
			if (new File(SECRET_KEY_DIR).exists()) {
				ois = new ObjectInputStream(new FileInputStream(SECRET_KEY_DIR));
				try {
					secretKey = (SecretKey) ois.readObject();
					// System.out.println("read");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					ois.close();
				}
				// 没有此文件存储密钥.
			} else {
				// 使用第一步获得的KeyGenerator类型的对象中generateKey()方法可以获得密钥。
				// 其类型为SecretKey类型，可用于以后的加密和解密。
				secretKey = keyGenerator.generateKey();
			}


			// 是否写入密钥到文件中.
			if (!new File(SECRET_KEY_DIR).exists()) {
				System.out.println(new File(SECRET_KEY_DIR).getAbsolutePath());
				oos = new ObjectOutputStream(new FileOutputStream(new File(SECRET_KEY_DIR)));
				oos.writeObject(secretKey);
				System.out.println("write");
			}

			// System.out.println("secretKey:" + secretKey);
			// System.out.println(secretKey.getAlgorithm() + "\t" + secretKey.getEncoded().length);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



	public MySecretKey getMySecretKey() throws IOException {

		File parentFile = new File(PLAIN_DIR);
		// 全局关键词集合.
		HashSet<String> globalDictSet = new HashSet<>();
		Matcher matcher = WORD_PATTERN.matcher("");

		// long start1 = System.currentTimeMillis();
		if (parentFile.exists()) {
			File[] files = parentFile.listFiles();

			for (int i = 0; i < files.length; i++) {
				// 用于计算有多少个文档包含此关键词, 即当前文档的关键词集合.
				Set<String> currentDocumentSet = new HashSet<>();
				List<String> allLines = Files.readAllLines(files[i].toPath());
				// 当前文档中出现关键词的频率.
				Map<String, Integer> currentDocumentKeywordFrequency = new HashMap<>();
				int wordCount = 0;
				for (String line : allLines) {
					//Matcher matcher = WORD_PATTERN.matcher(line);
					// reset要匹配的字符串.
					matcher = matcher.reset(line);
					while (matcher.find()) {
						// 忽略大小写.
						String keyword = matcher.group().toLowerCase();
						wordCount++;
						// 更新当前文档关键词集合.
						currentDocumentSet.add(keyword);
						// 放在后面addAll来更新.
						// setDict.add(keyword);
						// 更新当前文档关键字频率字典.
						if (!currentDocumentKeywordFrequency.containsKey(keyword)) {
							currentDocumentKeywordFrequency.put(keyword, 1);
						} else {
							currentDocumentKeywordFrequency.put(keyword, currentDocumentKeywordFrequency.get(keyword) + 1);
						}
					}
				}
				// 利用当前文档的关键词集合来更新总的字典集合。
				globalDictSet.addAll(currentDocumentSet);
				// 当前文档处理完毕,那么缓存当前文档的长度.
				fileLength.put(files[i].getName(), wordCount);
				// 当前文档中包含关键词的情况. 用于计算TF
				keywordFrequencyInDocument.put(files[i].getName(), currentDocumentKeywordFrequency);

				// 更新有多少个文档包含keyword; 用于计算IDF
				for (String word : currentDocumentSet) {
					if (!numberOfDocumentContainsKeyword.containsKey(word)) {
						numberOfDocumentContainsKeyword.put(word, 1);
					} else {
						numberOfDocumentContainsKeyword.put(word, numberOfDocumentContainsKeyword.get(word) + 1);
					}
				}
			}
		}

		// 统计1000个文档只用了3276ms
		// System.out.println("manage documents time consume:" + (System.currentTimeMillis() - start1));

//		System.out.println("fileLength:" + fileLength);
//		System.out.println("keywordFrequencyInDocument:" + keywordFrequencyInDocument);
//		System.out.println("numberOfDocumentContainsKeyword:" + numberOfDocumentContainsKeyword);

		System.out.println("fileLength.size():" + fileLength.size());
		System.out.println("keywordFrequencyInDocument.size():" + keywordFrequencyInDocument.size());
		System.out.println("numberOfDocumentContainsKeyword.size():" + numberOfDocumentContainsKeyword.size());

		Map<String, Integer> duplicateNumberOfDocumentContainsKeyword = new HashMap<>();
		for (Map.Entry<String, Integer> entry : numberOfDocumentContainsKeyword.entrySet()) {
			duplicateNumberOfDocumentContainsKeyword.put(new String(entry.getKey()), new Integer(entry.getValue()));
		}
		List<Map.Entry<String, Integer>> duplicate = new ArrayList<>(duplicateNumberOfDocumentContainsKeyword.entrySet());
		Collections.sort(duplicate, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				if (Integer.compare(o1.getValue(), o2.getValue()) > 0) {
					return -1;
				} else if (Integer.compare(o1.getValue(), o2.getValue()) == 0) {
					return 0;
				} else {
					return 1;
				}
			}
		});

		/*System.out.println("keyword - documentNumber");
		System.out.println(duplicate);*/

		// 测试关键词频率map中的结果是否和fileLength中数值相等.
		boolean result = testInfo(fileLength, keywordFrequencyInDocument);
		System.out.println("testInfo:" + result);

		List<String> dict = globalDictSet.stream().sorted().collect(toList());

		System.out.println("initialization dict.size():" + dict.size());
		// System.out.println(dict);

		// 初始化字典的长度和字典本身.
		this.lengthOfDict = dict.size();

		this.DICTIONARY_SIZE = lengthOfDict;
		// 拓展字典
		extendDummyDict = generateExtendDictPart(DUMMY_KEYWORD_NUMBER);
		dict.addAll(extendDummyDict);

		// 现在拓展的关键词不在末尾而是按序排在合适的位置.
		dict = dict.stream().sorted().collect(toList());
		this.dict = dict;

		// 问题是p'*q' + p"*q" = p * q, 虽然p拓展到了n+e维度, 但是问题在于
		// q向量中冗余关键词并没有设置相应的位(虽然也是n+e维度， )，那么
		System.out.println("add dummy keywords dict.size():" + this.dict.size());

		/*Arrays.stream(parentFile.listFiles()).map(File::toPath).flatMap(Files::readAllLines).collect()*/

		MySecretKey sk = new MySecretKey();

		BitSet bitSet = new BitSet(DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER);
		Random random = new Random(System.currentTimeMillis());
		for (int i = 0; i < (DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER); i++) {
			if (random.nextBoolean()) {
				bitSet.set(i);
			}
		}
		// 设置了该位， 此BitSet的长度才是 (DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER + 1)的长度.
		bitSet.set(DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER);
		System.out.println("bitSet.length:" + bitSet.length());

		/*Matrix m1 = Matrix.random(lengthOfDict + 1, lengthOfDict + 1);
		Matrix m2 = Matrix.random(lengthOfDict + 1, lengthOfDict + 1);*/

		Matrix m1 = Matrix.random(DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER, DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER);
		Matrix m2 = Matrix.random(DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER, DICTIONARY_SIZE + DUMMY_KEYWORD_NUMBER);
		sk.S = bitSet;
		sk.M1 = m1;
		sk.M2 = m2;
		sk.secretKey = secretKey;
		return sk;
	}

	private static List<String> generateExtendDictPart(int dummyKeywordNumber) {
		Random random = new Random(31);
		List<String> result = new ArrayList<>(dummyKeywordNumber);
		for (int i = 0; i < dummyKeywordNumber; i++) {
			// 生成的字符串的长度是3~10位。
			int length = 3 + random.nextInt(8);
			char[] array = new char[length];
			for (int j = 0; j < length; j++) {
				array[j] = (char) (97 + random.nextInt(26));
			}
			result.add(new String(array));
		}
		System.out.println("generate extend dict part:" + result);
		return result;
	}

	/**
	 * 测试fileLength和 keywordFrequencyInDocument中的信息是否匹配。
	 *
	 * @param fileLength
	 * @param keywordFrequencyInDocument
	 * @return
	 */
	private static boolean testInfo(Map<String, Integer> fileLength, Map<String, Map<String, Integer>> keywordFrequencyInDocument) {
		for (String key : keywordFrequencyInDocument.keySet()) {
			Map<String, Integer> keywordFrequency = keywordFrequencyInDocument.get(key);
			Integer total = keywordFrequency.entrySet().stream().map((Map.Entry<String, Integer> item) -> item.getValue()).reduce(Integer::sum).get();
			if (!total.equals(fileLength.get(key))) {
				return false;
			}
		}
		return true;
	}

	public SecretKey getSecretKey() {
		SecretKey secretKey = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			// 存储密钥的文件存在.
			if (new File(SECRET_KEY_DIR).exists()) {
				ois = new ObjectInputStream(new FileInputStream(SECRET_KEY_DIR));
				try {
					secretKey = (SecretKey) ois.readObject();
					System.out.println("read");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					ois.close();
				}
				// 没有此文件存储密钥.
			} else {
			 /*
			方法getInstance（ ）的参数为字符串类型，指定加密算法的名称。
			可以是 “Blowfish”、“DES”、“DESede”、“HmacMD5”或“HmacSHA1”等。
			其中“DES”是目前最常用的对称加密算法，但安全性较差。针对DES安全性
			的改进产生了能满足当前安全需要的TripleDES算法，即“DESede”。“Blowfish”
			的密钥长度可达448位，安全性很好。“AES”是一种替代DES算法的新算法，
			可提供很好的安全性。
			 */
				KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			/*
			该步骤一般指定密钥的长度。如果该步骤省略的话，会根据算法自动使用默认的密钥长度。
			指定长度时，若第一步密钥生成器使用的是“DES”算法，则密钥长度必须是56位；
			若是“DESede”，则可以是112或168位，其中112位有效；若是“AES”，可以是
			128, 192或256位；若是“Blowfish”，则可以是32至448之间可以被8整除的数；
			“HmacMD5”和“HmacSHA1”默认的密钥长度都是64个字节。
			 */
				keyGenerator.init(128);

				// 使用第一步获得的KeyGenerator类型的对象中generateKey( )方法可以获得密钥。
				// 其类型为SecretKey类型，可用于以后的加密和解密。
				secretKey = keyGenerator.generateKey();
			}

			// 是否写入密钥到文件中.
			if (!new File(SECRET_KEY_DIR).exists()) {
				System.out.println(new File(SECRET_KEY_DIR).getAbsolutePath());
				oos = new ObjectOutputStream(new FileOutputStream(new File(SECRET_KEY_DIR)));
				oos.writeObject(secretKey);
				System.out.println("write");
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return secretKey;
	}

	public static void main(String[] args) throws IOException {

		// test
		/*generateExtendDictPart(10);*/

		// test1
		Initialization initialization = new Initialization();
		MySecretKey mySecretKey = initialization.getMySecretKey();
		System.out.println(mySecretKey);

		long start = System.currentTimeMillis();
		mySecretKey.M1.transpose();
		System.out.println("Matrix transpose time consume:" + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();
		mySecretKey.M1.inverse();
		System.out.println("Matrix reverse time consume:" + (System.currentTimeMillis() - start) + "ms");


		System.out.println();
		System.out.println(Initialization.secretKey);
	}
}

/*
D:\MrDarcy\ForGraduationWorks\Code\SSE\src\main\java>javac com\darcy\Scheme2017MUSE\base\Initialization.java

D:\MrDarcy\ForGraduationWorks\Code\SSE\src\main\java>javac com\darcy\Scheme2017MUSE\base\Initialization.java


# 40
   没有忽略大小写
5906
time cousume:3785ms
MySecretKey{S.length=5906, M1.rank=5905, M2.rank=5905, secretKey=javax.crypto.spec.SecretKeySpec@16172}
Matrix transpose time consume:2944ms
Matrix reverse time consume:667690ms

javax.crypto.spec.SecretKeySpec@16172

  忽略大小写
5469
time cousume:2462ms
MySecretKey{S.length=5469, M1.rank=5468, M2.rank=5468, secretKey=javax.crypto.spec.SecretKeySpec@16172}
Matrix transpose time consume:1196ms
Matrix reverse time consume:522970ms

# 100
*/