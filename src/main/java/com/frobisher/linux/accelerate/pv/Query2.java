package com.frobisher.linux.accelerate.pv;

import com.frobisher.linux.accelerate.DiagonalMatrixUtils;
import com.frobisher.linux.utils.ByteArrayUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/*
 * author: darcy
 * date: 2017/12/19 17:05
 * description: 
*/
public class Query2 {

	public static void test() {
		try {
			Initialization initialization = new Initialization();
			MySecretKey mySecretKey = initialization.getMySecretKey();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
			System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start.");
			long start = System.currentTimeMillis();
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
			System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex end.");
			// System.out.println(root);

			// for-16
			// String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";

			// for-40
      // String query = "clinton broadcasting voice Francis honorary citizenship Democratic Revolution church president conferences";
			String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";
			List<String> queryList = Arrays.asList(query.toLowerCase().split("\\s+"));

			System.out.println("Query2 start generating trapdoor.");
			TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(queryList);

			// for-40
			int requestNumber1 = 15;
			// int requestNumber = 6;

			/*List<Integer> requestNumberList = new ArrayList<>();
			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.01);
			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.1);
			for (int i = low; i <= high; i += low) {
				requestNumberList.add(i);
			}*/

			List<Integer> requestNumberList = Arrays.asList(5, 10, 20, 30, 40, 50);
			for (int requestNumber : requestNumberList) {
				SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
				SearchResult searchResult = searchAlgorithm.search(root, trapdoor, requestNumber);
				PriorityQueue<HACTreeNode> priorityQueue = searchResult.result;
				System.out.println("Query2 priorityQueue.size():" + priorityQueue.size());
				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
				}
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
				String keywordPatternStr = getQueryPattern(query);

				System.out.println("\n requestNumber:" + requestNumber + "\t" + query);

				// 验证搜索结果是否包含特定的文档。
//				searchResultVerify(initialization, filenameList, keywordPatternStr, nodeScoreMap);


				verify(initialization, mySecretKey, searchResult);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
	}

	private static void verify(Initialization initialization, MySecretKey mySecretKey, SearchResult searchResult) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException {
		System.out.println();
		long start = System.currentTimeMillis();
		PriorityQueue<HACTreeNode> result = searchResult.result;

		byte[] resultBytes = new byte[searchResult.result.peek().digest.digest().length];
		while (!result.isEmpty()) {
			HACTreeNode node = result.poll();
			System.out.println(node.fileDescriptor);
			String filename = node.fileDescriptor;
			filename = filename.substring(0, filename.lastIndexOf("."));
			String encryptedFileName = initialization.ENCRYPTED_DIR
					+ Initialization.SEPERATOR + "encrypted_" + filename + ".dat";

			byte[] encryptedBytes = Files.readAllBytes(new File(encryptedFileName).toPath());
			byte[] decryptedBytes = EncryptionUtils.decrypt(encryptedBytes);

			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] keyBytes = mySecretKey.secretKey.getEncoded();

			byte[] bytes = new byte[keyBytes.length + decryptedBytes.length];
			System.arraycopy(keyBytes, 0, bytes, 0, keyBytes.length);
			System.arraycopy(decryptedBytes, 0, bytes, keyBytes.length, decryptedBytes.length);
			messageDigest.update(bytes);

			resultBytes = ByteArrayUtils.xorArrays(resultBytes, messageDigest.digest());
		}
		System.out.println(Arrays.equals(resultBytes, searchResult.byteXorResults));
		System.out.println("verify result:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println();
	}

	public static void testAdjustify() {
		try {
			Initialization initialization = new Initialization();
			// 没有写textrank和plain文档分离的版本。
			// 只是new def了两个函数。
			MySecretKey mySecretKey = initialization.getMySecretKey();

			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
//			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();

			System.out.println("\nHACTreeIndexBuilding build & encrypt start.");
			long start = System.currentTimeMillis();
			// build
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
			// encrypt
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
			System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt end.");

			// for-16
			// String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";

			// for-40
			// String query = "clinton broadcasting voice Francis honorary citizenship Democratic Revolution church president conferences";
			/*String query = "church China hospital performance British" +
					" interview Democratic citizenship broadcasting voice official military";*/
			for (Integer keywordNumber : Arrays.asList(5, 10, 15, 20, 25)) {
				Random random = new Random();
				System.out.println("\n\n");
				Set<String> querySet = new HashSet<>(20);
//					int keywordNumber = 20;
				while (querySet.size() < keywordNumber) {
					String word = initialization.dict.get(random.nextInt(initialization.dict.size()));
					if (initialization.extendDummyDict.indexOf(word) != -1) {
						continue;
					}
					querySet.add(word);
				}
				List<String> queryList = querySet.stream().collect(toList());
				String query = querySet.stream().collect(joining(" "));

				TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(queryList);

				// for-40
				List<Integer> requestNumberList = new ArrayList<>();
//					int low = 2;
//					int high = 2;
				int low = 5;
				int high = 25;
				for (int i = low; i <= high; i += low) {
					requestNumberList.add(i);
				}
				for (int requestNumber : requestNumberList) {
					printDash();
					System.out.println("DocumentSetSize:" + initialization.keywordFrequencyInDocument.size()
							+ "\trequestNumber:" + requestNumber + "\tkeywordNumber:" + keywordNumber
							+ "\tDictionarySize:" + initialization.dict.size());
					SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
					SearchResult searchResult = searchAlgorithm.search(root, trapdoor, requestNumber);
					PriorityQueue<HACTreeNode> priorityQueue = searchResult.result;
					System.out.println("QUERY:" + query + "\nrequestNumber:" + requestNumber + "\npriorityQueue:" + priorityQueue.size());
					printDash();
//						Map<String, Double> nodeScoreMap = new HashMap<>();
//						for (HACTreeNode node : priorityQueue) {
//							nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//						}
//						List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
//						String keywordPatternStr = getQueryPattern(query);
//
//					System.out.println("\nrequestNumber:" + requestNumber + "\t" + query);
//
//					// 验证搜索结果是否包含特定的文档。
//						searchResultVerify(initialization, filenameList, keywordPatternStr, nodeScoreMap);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
	}

	public static void testWithTextRank() {
		try {
			Initialization initialization = new Initialization();
			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			// 基于TextRank.
			System.out.println("\nHACTreeIndexBuilding build & encrypt start.");
			long start = System.currentTimeMillis();
			// build
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndexWithTextRank();
			// encrypt
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
			System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt end.");

			for (Integer keywordNumber : Arrays.asList(5, 10, 15, 20, 25)) {
				for (int iii = 0; iii < 5; iii++) {
					printDash();
					Random random = new Random();
					Set<String> keywordSet = new HashSet<>(20);
					while (keywordSet.size() < keywordNumber) {
						String keyword = initialization.dict.get(random.nextInt(initialization.dict.size()));
						if (initialization.extendDummyDict.indexOf(keyword) != -1) {
							continue;
						}
						keywordSet.add(keyword);
					}

					List<String> queryList = keywordSet.stream().collect(toList());
					String query = keywordSet.stream().collect(joining(" "));

					TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
					Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(queryList);
					List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.005);
//			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.05);
					int low = 2;
					int high = 10;
					for (int i = low; i <= high; i += low) {
						requestNumberList.add(i);
					}

					// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
					for (int requestNumber : requestNumberList) {
						System.out.println("QUERY:" + query);
						SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
						SearchResult searchResult = searchAlgorithm.search(root, trapdoor, requestNumber);
						PriorityQueue<HACTreeNode> priorityQueue = searchResult.result;
						System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
						printDash();
						Map<String, Double> nodeScoreMap = new HashMap<>();
						for (HACTreeNode node : priorityQueue) {
							nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
						}
						List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
//						String keywordPatternStr = getQueryPattern(query);
						Set<String> querySet = new HashSet<>(queryList);
//						System.out.println("QUERY: " + query);
//						searchResultVerifyTextRank(initialization, filenameList, querySet, nodeScoreMap);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
	}

	public static void printDash() {
		System.out.println("------------------------------------------------------------------------------");
	}

	private static void searchResultVerifyTextRank(Initialization initialization, List<String> filenameList,Set<String> querySet, Map<String, Double> nodeScoreMap) throws IOException {
		for (int i = 0; i < filenameList.size(); i++) {
			System.out.println(filenameList.get(i) + "\tscore:" + nodeScoreMap.get(filenameList.get(i)));
			List<String> allLines = Files.readAllLines(new File(initialization.TEXTRANK_WORD_WEIGHT_DIR + Initialization.SEPERATOR + filenameList.get(i)).toPath());
//			String passage = allLines.stream().map(String::toLowerCase).collect(joining("\n"));
			int count = 0;
			for (String line : allLines) {
				String[] array = line.split("(\\s+|:)");
//				Map<String, Double> keywordWeight = initialization.fileTextRankMap.get(filenameList.get(i));
				if (array.length == 2) {
					if (querySet.contains(array[0])) {
						count++;
						System.out.println(line);
					}
				} else {
					for (int j = 0; j < array.length - 1; j++) {
						if (querySet.contains(array[j])) {
							count++;
							System.out.println(array[j] + "\t" + array[array.length - 1]);
						}
					}
				}
			}
			System.out.println("count:" + count);
			System.out.println();
		}
	}

	private static double scoreForPruning(HACTreeNode root, Trapdoor trapdoor) {
		/*return root.pruningVector.times(queryVector).get(0, 0);*/
		return DiagonalMatrixUtils.score(root.pruningVectorPart1, trapdoor.trapdoorPart1) +
				DiagonalMatrixUtils.score(root.pruningVectorPart2, trapdoor.trapdoorPart2);
	}

	private static void searchResultVerify(Initialization initialization, List<String> filenameList, String keywordPatternStr, Map<String, Double> nodeScoreMap) throws IOException {
		System.out.println();

		Pattern keywordPattern = Pattern.compile(keywordPatternStr);
		for (int i = 0; i < filenameList.size(); i++) {
			System.out.println(filenameList.get(i) + "\tscore:" + nodeScoreMap.get(filenameList.get(i)));
			List<String> allLines = Files.readAllLines(new File(initialization.PLAIN_DIR
					+ Initialization.SEPERATOR + filenameList.get(i)).toPath());
			String passage = allLines.stream().map(String::toLowerCase).collect(joining("\n"));

			Matcher matcher = keywordPattern.matcher(passage);
			int count = 0;
			while (matcher.find()) {
				String keyword = matcher.group().toLowerCase();
				/*System.out.println(filenameArray[i] + "\t" + keyword + "\t" + Initialization.keywordFrequencyInDocument.get(filenameArray[i]).get(keyword) + "\t" + "documentNumber\t" + Initialization.numberOfDocumentContainsKeyword.get(keyword));*/
				Map<String, Integer> stringFrequencies = initialization.keywordFrequencyInDocument.get(filenameList.get(i));
				if (stringFrequencies.containsKey(keyword)) {
					System.out.printf("%-15s\t%-10s%-15s\t%10s\n", keyword,
							stringFrequencies.get(keyword),
							"docsNumber", initialization.numberOfDocumentContainsKeyword.get(keyword));
					count++;
				}
			}
			System.out.println("count:" + count);
			System.out.println();
		}
	}

	public static String getQueryPattern(String str) {
		Matcher matcher = Initialization.WORD_PATTERN.matcher(str);
		String result = "";
		while (matcher.find()) {
			result += matcher.group().toLowerCase() + "|";
		}
		return "(" + result.substring(0, result.lastIndexOf('|')) + ")";
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		System.out.println(Query2.class.getName() + " search.");
		System.out.println(new Date());
		long start = System.currentTimeMillis();

//		testAdjustify();
//		testWithTextRank();
		test();

		long end = System.currentTimeMillis();
		long s = (start - end) / 1000;
		long h = s / 3600;
		long m = (s - h * 3600) / 60;
		s = s - h * 3600 - m * 60;
		System.out.println(h + "hours" + m + "minutes" + s + "seconds");
		System.out.println(new Date());
	}
}
