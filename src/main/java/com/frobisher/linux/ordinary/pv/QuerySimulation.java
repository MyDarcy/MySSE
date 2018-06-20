package com.frobisher.linux.ordinary.pv;

import Jama.Matrix;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/*
 * author: darcy
 * date: 2017/12/19 17:05
 * description: 
*/
public class QuerySimulation {

	public static void test() {
		try {
			Initialization initialization = new Initialization();
			// 没有写textrank和plain文档分离的版本。
			// 只是new def了两个函数。
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(
					2000, 3000);

			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
//			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
//			// 基于TextRank.
//			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndexWithTextRank();

			System.out.println("\nHACTreeIndexBuilding build & encrypt index start.");
			long start = System.currentTimeMillis();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
//				System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
//				long start = System.currentTimeMillis();
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
//				System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt index end.");

			System.out.println("\nHACTreeIndexBuilding build & encrypt sequential-index start.");
			start = System.currentTimeMillis();
			List<HACTreeNode> sequentialIndex = hacTreeIndexBuilding.buildSequentialIndex();
			sequentialIndex = hacTreeIndexBuilding.encryptSequentialIndex(sequentialIndex);
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt sequential-index end.");

			List<Integer> keywordsIndex = Arrays.asList(
					18, 122, 144, 290, 450, 800, 1200, 2500, 3712, 4817, 5412, 6611, 7810
			);

//			keywordsIndex = Arrays.asList(
//					18, 45, 89, 198, 256, 390, 450, 680, 789, 812, 907, 1222, 1368
//			);

			Random random = new Random(31);
			int queryNumber = 20;
			keywordsIndex = new ArrayList<>(queryNumber);
			int j = 0;
			while (j < queryNumber) {
				int queryIndex = random.nextInt(initialization.simulationDictSize);
				if (!initialization.simulationDummykeywordIndexSet.contains(queryIndex)) {
					if (keywordsIndex.indexOf(queryIndex) == -1) {
						keywordsIndex.add(queryIndex);
						j++;
					}
				}
			}

			TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);

			// for-40
       int requestNumber1 = 4;
			List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.simulationDocumentNumber * 0.01);
//			int high = (int) Math.ceil(initialization.simulationDocumentNumber * 0.1);
			int low = 10;
			int high = 50;
			for (int i = low; i <= high; i += low) {
				requestNumberList.add(i);
			}

			// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
			for (int requestNumber : requestNumberList) {
				System.out.println();
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:" + requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());

//				PriorityQueue<HACTreeNode> sequentialSearchResult = searchAlgorithm.sequentialSearch(sequentialIndex, trapdoor, requestNumber);
//
//				Map<String, Double> nodeScoreMap = new HashMap<>();
//				for (HACTreeNode node : priorityQueue) {
//					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//				}
//
//				Map<String, Double> nodeScoreMapSequential = new HashMap<>();
//				for (HACTreeNode node : sequentialSearchResult) {
//					nodeScoreMapSequential.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//				}
//
//				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
//				List<String> filenameList2 = sequentialSearchResult.stream().map((node) -> node.fileDescriptor).collect(toList());
//				if (filenameList.size() == filenameList2.size()) {
//					IntStream.range(0, filenameList.size()).forEach((i) -> {
//						String name1 = filenameList.get(i);
//						String name2 = filenameList2.get(i);
//						System.out.println(name1 + "\t" + nodeScoreMap.get(name1) + "\t\t\t\t" + name2 + "\t" + nodeScoreMapSequential.get(name2) + "\t" + name1.equals(name2));
//					});
//					boolean b = IntStream.range(0, filenameList.size()).allMatch((i) -> filenameList.get(i).equals(filenameList2.get(i)));
//					System.out.println(b);
//				} else {
//					System.out.println("false");
//				}
				printDash();
//				Map<String, Double> nodeScoreMap = new HashMap<>();
//				for (HACTreeNode node : priorityQueue) {
//					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//				}
//				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());

//				System.out.println("\nrequestNumber:" + requestNumber + "\t" + keywordsIndex);

				// 验证搜索结果是否包含特定的文档。
//				searchResultVerify(initialization, filenameList, keywordsIndex, nodeScoreMap);
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

	public static void testWithFixedDocumentNumberKeywordNumberRequestNumber() {
		testWithFixedDocumentNumberKeywordNumberRequestNumber(6000, 20, 20);
	}

	/**
	 * 测试，固定文档数目和查询数目
	 * @param documentNumber 实验中对比数据是 6000
	 * @param requestNumber  实验中对比数据是 20
	 */
	public static void testWithFixedDocumentNumberKeywordNumberRequestNumber(int documentNumber,
																																					 int keywordNumber,
																																					 int requestNumber) {
		try {
			List<Integer> dictSizeList = Arrays.asList(2000, 4000, 6000, 8000, 10000);
//			List<Integer> dictSizeList = Arrays.asList(200, 400, 600, 800, 1000);
			for (int i = 0; i < dictSizeList.size(); i++) {
				printDash();
				System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
						"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSizeList.get(i));
				System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
						"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSizeList.get(i));
				Initialization initialization = new Initialization();
				MySecretKey mySecretKey = initialization.getMySecretKeySimulation(
						documentNumber, dictSizeList.get(i));
				HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
				hacTreeIndexBuilding.encryptFiles();
				hacTreeIndexBuilding.generateAuxiliaryMatrix();
				System.out.println("HACTreeIndexBuilding build & encrypt index start...");
				long start = System.currentTimeMillis();
				HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
//				System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
//				long start = System.currentTimeMillis();
				hacTreeIndexBuilding.encryptHACTreeIndex(root);
//				System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
				System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
				System.out.println("HACTreeIndexBuilding build & encrypt index end.");

				System.out.println("\nHACTreeIndexBuilding build & encrypt sequential-index start.");
				start = System.currentTimeMillis();
				List<HACTreeNode> sequentialIndex = hacTreeIndexBuilding.buildSequentialIndex();
				sequentialIndex = hacTreeIndexBuilding.encryptSequentialIndex(sequentialIndex);
				System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
				System.out.println("HACTreeIndexBuilding build & encrypt sequential-index end.");

				Random random = new Random(31);
				// attention, 此处的requestNumber和生成陷门处兴趣模型时的关键词是耦合的。
				List<Integer> keywordsIndex = new ArrayList<>(keywordNumber);
				int j = 0;
				while (j < keywordNumber) {
					int queryIndex = random.nextInt(initialization.simulationDictSize);
					if (!initialization.simulationDummykeywordIndexSet.contains(queryIndex)) {
						if (keywordsIndex.indexOf(queryIndex) == -1) {
							keywordsIndex.add(queryIndex);
							j++;
						}
					}
				}

				TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:" + requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());

				PriorityQueue<HACTreeNode> sequentialSearchResult = searchAlgorithm.sequentialSearch(sequentialIndex, trapdoor, requestNumber);
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
				List<String> filenameList2 = sequentialSearchResult.stream().map((node) -> node.fileDescriptor).collect(toList());
				if (filenameList.size() == filenameList2.size()) {
					/*IntStream.range(0, filenameList.size()).forEach((i) -> {
						System.out.println(filenameList.get(i) + "\t" + filenameList2.get(i) + "\t" + filenameList.get(i).equals(filenameList2.get(i)));
					});*/
					boolean b = IntStream.range(0, filenameList.size()).allMatch((iii) -> filenameList.get(iii).equals(filenameList2.get(iii)));
					System.out.println(b);
				} else {
					System.out.println("false");
				}

				printDash();
//				Map<String, Double> nodeScoreMap = new HashMap<>();
//				for (HACTreeNode node : priorityQueue) {
//					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//				}
//				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
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

	public static void testWithFixedDictSizeKeywordNumberRequestNumber() {
		testWithFixedDictSizeKeywordNumberRequestNumber(4000, 20, 20);
	}

	/**
	 * 测试固定数目的
	 * @param dictSize
	 * @param requestNumber
	 */
	public static void testWithFixedDictSizeKeywordNumberRequestNumber(int dictSize,
																																		 int keywordNumber,
																																		 int requestNumber) {
		try {
			List<Integer> documentNumberList = Arrays.asList(2000, 4000, 6000, 8000, 10000);
//			List<Integer> documentNumberList = Arrays.asList(200, 400, 600, 800, 1000);
			for (int i = 0; i < documentNumberList.size(); i++) {
			printDash();
			int documentNumber = documentNumberList.get(i);
			System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
					"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
			System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
					"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
			Initialization initialization = new Initialization();
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(
					documentNumber, dictSize);
			HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			System.out.println("HACTreeIndexBuilding build & encrypt index start...");
			long start = System.currentTimeMillis();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
//				System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
//				long start = System.currentTimeMillis();
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
//				System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt index end.");

			System.out.println("\nHACTreeIndexBuilding build & encrypt sequential-index start.");
			start = System.currentTimeMillis();
			List<HACTreeNode> sequentialIndex = hacTreeIndexBuilding.buildSequentialIndex();
			sequentialIndex = hacTreeIndexBuilding.encryptSequentialIndex(sequentialIndex);
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt sequential-index end.");

			Random random = new Random(31);
			// attention, 此处的requestNumber和生成陷门处兴趣模型时的关键词是耦合的。
			List<Integer> keywordsIndex = new ArrayList<>(keywordNumber);
			int j = 0;
			while (j < keywordNumber) {
				int queryIndex = random.nextInt(initialization.simulationDictSize);
				if (!initialization.simulationDummykeywordIndexSet.contains(queryIndex)) {
					if (keywordsIndex.indexOf(queryIndex) == -1) {
						keywordsIndex.add(queryIndex);
						j++;
					}
				}
			}

			TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
			SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
			PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
			System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());

			PriorityQueue<HACTreeNode> sequentialSearchResult = searchAlgorithm.sequentialSearch(sequentialIndex, trapdoor, requestNumber);
			List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
			List<String> filenameList2 = sequentialSearchResult.stream().map((node) -> node.fileDescriptor).collect(toList());
			if (filenameList.size() == filenameList2.size()) {
				/*IntStream.range(0, filenameList.size()).forEach((i) -> {
					System.out.println(filenameList.get(i) + "\t" + filenameList2.get(i) + "\t" + filenameList.get(i).equals(filenameList2.get(i)));
				});*/
				boolean b = IntStream.range(0, filenameList.size()).allMatch((iii) -> filenameList.get(iii).equals(filenameList2.get(iii)));
				System.out.println(b);
			} else {
				System.out.println("false");
			}

			printDash();
//				Map<String, Double> nodeScoreMap = new HashMap<>();
//				for (HACTreeNode node : priorityQueue) {
//					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//				}
//				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
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

	public static void testWithFixedDocumentNumberDictSize() {
		testWithFixedDocumentNumberDictSize(6000, 4000);
	}


	/**
	 * 测试固定数目的
	 * @param documentNumber
	 * @param dictSize
	 */
	public static void testWithFixedDocumentNumberDictSize(int documentNumber, int dictSize) {
		try {

			Initialization initialization = new Initialization();
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(
					documentNumber, dictSize);
			HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			System.out.println("HACTreeIndexBuilding build & encrypt index start...");
			long start = System.currentTimeMillis();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
//				System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
//				long start = System.currentTimeMillis();
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
//				System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt index end.");

			System.out.println("\nHACTreeIndexBuilding build & encrypt sequential-index start.");
			start = System.currentTimeMillis();
			List<HACTreeNode> sequentialIndex = hacTreeIndexBuilding.buildSequentialIndex();
			sequentialIndex = hacTreeIndexBuilding.encryptSequentialIndex(sequentialIndex);
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt sequential-index end.");

			List<Integer> requestNumberList = Arrays.asList(5, 10, 20, 30, 40, 50);
			List<Integer> keywordNumberList = Arrays.asList(5, 10, 20, 30, 40, 50);
//			List<Integer> requestNumberList = Arrays.asList(5, 10, 15, 20, 25);
//			List<Integer> keywordNumberList = Arrays.asList(5, 10, 15, 20, 25);
			for (int i = 0; i < requestNumberList.size(); i++) {
				for (int j = 0; j < keywordNumberList.size(); j++) {
					int requestNumber = requestNumberList.get(i);
					int keywordNumber = keywordNumberList.get(j);
					printDash();
					System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
							"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
					System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
							"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);

					Random random = new Random(31);
					// attention, 此处的requestNumber和生成陷门处兴趣模型时的关键词是耦合的。
					List<Integer> keywordsIndex = new ArrayList<>(keywordNumber);
					int temp = 0;
					while (temp < keywordNumber) {
						int queryIndex = random.nextInt(initialization.simulationDictSize);
						if (!initialization.simulationDummykeywordIndexSet.contains(queryIndex)) {
							if (keywordsIndex.indexOf(queryIndex) == -1) {
								keywordsIndex.add(queryIndex);
								temp++;
							}
						}
					}

					TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
					Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
					SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
					PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
					System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());

					PriorityQueue<HACTreeNode> sequentialSearchResult = searchAlgorithm.sequentialSearch(sequentialIndex, trapdoor, requestNumber);
					List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
					List<String> filenameList2 = sequentialSearchResult.stream().map((node) -> node.fileDescriptor).collect(toList());
					if (filenameList.size() == filenameList2.size()) {
					/*IntStream.range(0, filenameList.size()).forEach((i) -> {
						System.out.println(filenameList.get(i) + "\t" + filenameList2.get(i) + "\t" + filenameList.get(i).equals(filenameList2.get(i)));
					});*/
						boolean b = IntStream.range(0, filenameList.size()).allMatch((iii) -> filenameList.get(iii).equals(filenameList2.get(iii)));
						System.out.println(b);
					} else {
						System.out.println("false");
					}

					printDash();
//					Map<String, Double> nodeScoreMap = new HashMap<>();
//					for (HACTreeNode node : priorityQueue) {
//						nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
//					}
//					List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
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

	private static void printDash() {
		System.out.println("----------------------------------------------------------------------------------");
	}

	private static void searchResultVerify(Initialization initialization, List<String> filenameList, List<Integer> keywordsIndex, Map<String, Double> nodeScoreMap) throws IOException {
		System.out.println();
		for (int i = 0; i < filenameList.size(); i++) {
			System.out.println();
			System.out.println(filenameList.get(i) + "\tscore:" + nodeScoreMap.get(filenameList.get(i)));
			String filename = filenameList.get(i);
			int simulationFileIndex = Integer.valueOf(filename);
			Matrix matrix = initialization.simulationDocuments.get(simulationFileIndex);
			for (int j = 0; j < keywordsIndex.size(); j++) {
				if (Double.compare(matrix.get(keywordsIndex.get(j), 0), 0) > 0) {
					System.out.printf("%-10s%-6f\n", keywordsIndex.get(j), matrix.get(keywordsIndex.get(j), 0));
				}
			}
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

	public static List<Integer> getKeywordsIndex(Initialization initialization, int keywordNumber) {
		Random random = new Random(31);
		List<Integer> keywordsIndex = new ArrayList<>(keywordNumber);
		int temp = 0;
		while (temp < keywordNumber) {
			int queryIndex = random.nextInt(initialization.simulationDictSize);
			if (!initialization.simulationDummykeywordIndexSet.contains(queryIndex)) {
				if (keywordsIndex.indexOf(queryIndex) == -1) {
					keywordsIndex.add(queryIndex);
					temp++;
				}
			}
		}
		return keywordsIndex;
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		System.out.println(new Date());
		long start = System.currentTimeMillis();
		System.out.println(QuerySimulation.class.getName() + " search.");
		test();
//		System.out.println("testWithFixedDocumentNumberKeywordNumberRequestNumber");
//		testWithFixedDocumentNumberKeywordNumberRequestNumber(6000, 20, 20);
////		testWithFixedDocumentNumberKeywordNumberRequestNumber(600, 10, 10);
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println("testWithFixedDictSizeKeywordNumberRequestNumber");
//		testWithFixedDictSizeKeywordNumberRequestNumber(4000,20, 20);
////		testWithFixedDictSizeKeywordNumberRequestNumber(400,10, 10);
//		System.out.println();
//		System.out.println();
//		System.out.println();
////		System.out.println("testWithFixedDocumentNumberDictSize");
//		testWithFixedDocumentNumberDictSize(6000, 4000);
////		testWithFixedDocumentNumberDictSize(600, 400);

		long end = System.currentTimeMillis();
		long s = (start - end) / 1000;
		long h = s / 3600;
		long m = (s - h * 3600) / 60;
		s = s - h * 3600 - m * 60;
		System.out.println(h + "hours" + m + "minutes" + s + "seconds");
		System.out.println(new Date());
	}

//	public static void main(String[] args) {
//		System.out.println(new Date());
//	}
}
