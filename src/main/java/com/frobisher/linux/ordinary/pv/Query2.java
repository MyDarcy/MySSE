package com.frobisher.linux.ordinary.pv;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.security.InvalidKeyException;
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
			// 没有写textrank和plain文档分离的版本。
			// 只是new def了两个函数。
			MySecretKey mySecretKey = initialization.getMySecretKey();

			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
//			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();

			System.out.println("HACTreeIndexBuilding build & encrypt start.");
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

//			String query = "java python go";

				TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(queryList);

				// for-40
				List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.005);
//			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.05);
//					int low = 2;
//					int high = 2;
				int low = 5;
				int high = 25;
				for (int i = low; i <= high; i += low) {
					requestNumberList.add(i);
				}

				// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
				for (int requestNumber : requestNumberList) {
					printDash();
					System.out.println("DocumentSetSize:" + initialization.keywordFrequencyInDocument.size()
							+ "\trequestNumber:" + requestNumber + "\tkeywordNumber:" + keywordNumber
							+ "\tDictionarySize:" + initialization.dict.size());
					SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
					PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
					System.out.println("QUERY:" + query + "\nrequestNumber:" + requestNumber + "\npriorityQueue:" + priorityQueue.size());
					printDash();
					/*	Map<String, Double> nodeScoreMap = new HashMap<>();
						for (HACTreeNode node : priorityQueue) {
							nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
						}
						List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
						String keywordPatternStr = getQueryPattern(query);*/

//					System.out.println("\nrequestNumber:" + requestNumber + "\t" + query);

					// 验证搜索结果是否包含特定的文档。
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

			// mind here, 这里是对textrank目录下的文件进行加密。
			// 而不是原来的明文目录。
			// click to see the source code.
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			// 基于TextRank.
			System.out.println("HACTreeIndexBuilding build & encrypt start...");
			long start = System.currentTimeMillis();
			// build
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndexWithTextRank();
			// encrypt
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
			System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding buildHACTreeIndex & encryptHACTreeIndex end.");

			for (Integer keywordNumber : Arrays.asList(5, 10, 15, 20, 25)) {
				System.out.println("\n\n");
				printDash();
				Random random = new Random();
				Set<String> keywordSet = new HashSet<>(20);
				while (keywordSet.size() < keywordNumber) {
					keywordSet.add(initialization.dict.get(random.nextInt(initialization.dict.size())));
				}

				List<String> queryList = keywordSet.stream().collect(toList());
				String query = keywordSet.stream().collect(joining(" "));
				TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(queryList);
				List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.005);
//			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.05);
//					int low = 2;
//					int high = 2;
				int low = 5;
				int high = 25;
				for (int i = low; i <= high; i += low) {
					requestNumberList.add(i);
				}

				// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
				for (int requestNumber : requestNumberList) {
					System.out.println("QUERY:" + query);
					SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
					PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
					System.out.println("requestNumber:" + requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
					printDash();
//						Map<String, Double> nodeScoreMap = new HashMap<>();
//						for (HACTreeNode node : priorityQueue) {
//							nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//						}
//						List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
////						String keywordPatternStr = getQueryPattern(query);
//						Set<String> querySet = new HashSet<>(queryList);
//						System.out.println("QUERY: " + query);
//						searchResultVerifyTextRank(initialization, filenameList, querySet, nodeScoreMap);
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

	private static void searchResultVerifyTextRank(Initialization initialization, List<String> filenameList, Set<String> querySet, Map<String, Double> nodeScoreMap) throws IOException {
		for (int i = 0; i < filenameList.size(); i++) {
			System.out.println(filenameList.get(i) + "\tscore:" + nodeScoreMap.get(filenameList.get(i)));
			List<String> allLines = Files.readAllLines(new File(initialization.TEXTRANK_WORD_WEIGHT_DIR + Initialization.SEPERATOR + filenameList.get(i)).toPath());
//			String passage = allLines.stream().map(String::toLowerCase).collect(joining("\n"));
			int count = 0;
			for (String line : allLines) {
				String[] array = line.toLowerCase().split("(\\s+|:)");
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
				assert matcher != null;
				assert matcher.group() != null;
				String keyword = matcher.group().toLowerCase();
				/*System.out.println(filenameArray[i] + "\t" + keyword + "\t" + Initialization.keywordFrequencyInDocument.get(filenameArray[i]).get(keyword) + "\t" + "documentNumber\t" + Initialization.numberOfDocumentContainsKeyword.get(keyword));*/
				Map<String, Integer> keywordFrequency = initialization.keywordFrequencyInDocument.get(filenameList.get(i));
				if (keywordFrequency.containsKey(keyword)) {
//					System.out.printf("%-15s\t%-10s%-15s\t%10s\n", keyword,
//							keywordFrequency.get(keyword),
//							"docsNumber", initialization.numberOfDocumentContainsKeyword.get(keyword));
					count++;
				}

				// 单纯的看输出会发现
//				System.out.printf("%-15s\t%-10s\n", keyword,
//						initialization.fileTextRankMap.get(filenameList.get(i)).containsKey(keyword)? initialization.fileTextRankMap.get(keyword) : "提取的关键词文件中不包含 " + keyword);

			}
			System.out.println("count:" + count);
			System.out.println();
		}
	}

	private static double scoreForPruning(HACTreeNode root, Trapdoor trapdoor) {
		/*return root.pruningVector.times(queryVector).get(0, 0);*/
		/*return root.pruningVectorPart1.transpose().times(trapdoor.trapdoorPart1).get(0, 0)
				+ root.pruningVectorPart2.transpose().times(trapdoor.trapdoorPart2).get(0, 0);*/

		double[][] p1 = root.pruningVectorPart1.getArray();
		double[][] p2 = root.pruningVectorPart2.getArray();
		double[][] q1 = trapdoor.trapdoorPart1.getArray();
		double[][] q2 = trapdoor.trapdoorPart2.getArray();
		double sum = 0;
		for (int i = 0; i < p1.length; i++) {
			sum += p1[i][0] * q1[i][0] + p2[i][0] * q2[i][0];
		}
		return sum;
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

		System.out.println("normal");
		test();
//		System.out.println("tf-idf-1000-20");
//		testWithTextRank();

		long end = System.currentTimeMillis();
		long s = (start - end) / 1000;
		long h = s / 3600;
		long m = (s - h * 3600) / 60;
		s = s - h * 3600 - m * 60;
		System.out.println(h + "hours" + m + "minutes" + s + "seconds");
		System.out.println(new Date());
	}

//	public static void main(String[] args) {
//		List<String> stringList = Arrays.asList("aabb ccdd", "a:b", "a   b");
//		for (String line : stringList) {
//			String[] words = line.split("(\\s+|:)");
//			System.out.println(Arrays.toString(words));
//		}
//	}
}
