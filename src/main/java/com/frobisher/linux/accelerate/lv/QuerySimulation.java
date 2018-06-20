package com.frobisher.linux.accelerate.lv;

import com.frobisher.linux.accelerate.DiagonalMatrixUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

			// 如果文档数比较短小，那么逻辑检索就会需要搜索整棵树，因为满足条件的文档不会特别多。
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(
					2, 4,2000, 4000);

			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
//			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
//			// 基于TextRank.
//			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndexWithTextRank();
			System.out.println("\nHACTreeIndexBuilding build & encrypt index start...");
			long start = System.currentTimeMillis();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
//				System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
//				long start = System.currentTimeMillis();
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
//				System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding build & encrypt index end.");

			int orNumbers = 5;
			int andNumbers = 2;
			int notNumbers = 2;

			List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);


			TrapdoorGeneratingSimulation trapdoorGenerating =
					new TrapdoorGeneratingSimulation(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);

			// for-40
       int requestNumber1 = 4;
			List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.simulationDocumentNumber * 0.001);
//			int high = (int) Math.ceil(initialization.simulationDocumentNumber * 0.01);
			int low = 5;
			int high = 50;
			for (int i = low; i <= high; i += low) {
				requestNumberList.add(i);
			}

			// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
			for (int requestNumber : requestNumberList) {
				System.out.println();
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
				}
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
				System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" +
						keywordNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSizeList.get(i));
				System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" +
						keywordNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSizeList.get(i));
				Initialization initialization = new Initialization();
				MySecretKey mySecretKey = initialization.getMySecretKeySimulation(2, 4,
						documentNumber, dictSizeList.get(i));
				HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
				hacTreeIndexBuilding.encryptFiles();
				hacTreeIndexBuilding.generateAuxiliaryMatrix();
				System.out.println("\nHACTreeIndexBuilding build & encrypt index start.");
				long start = System.currentTimeMillis();
				HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
//				System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
//				long start = System.currentTimeMillis();
				hacTreeIndexBuilding.encryptHACTreeIndex(root);
//				System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
				System.out.println("build & encrypt cost:" + (System.currentTimeMillis() - start) + "ms");
				System.out.println("HACTreeIndexBuilding build & encrypt index end.");

				int andNumbers = 2;
				int notNumbers = 3;
				int orNumbers = keywordNumber - andNumbers - notNumbers;

				List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);

				TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:" + requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
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
				MySecretKey mySecretKey = initialization.getMySecretKeySimulation(2, 4,
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

				int andNumbers = 2;
				int notNumbers = 3;
				int orNumbers = keywordNumber - andNumbers - notNumbers;

				List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);

				TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:" + requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
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
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(2, 4,
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


			List<Integer> requestNumberList = Arrays.asList(5, 10, 20, 30, 40, 50);
			List<Integer> keywordNumberList = Arrays.asList(5, 10, 20, 30, 40, 50);
			for (int i = 0; i < requestNumberList.size(); i++) {
				for (int j = 0; j < keywordNumberList.size(); j++) {
					printDash();
					int requestNumber = requestNumberList.get(i);
					int keywordNumber = requestNumberList.get(j);
					System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
							"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
					System.out.println("documentNumber:" + documentNumber + "\tkeywordNumber:" + keywordNumber +
							"\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);

					int andNumbers = 2;
					int notNumbers = 3;
					int orNumbers = keywordNumber - andNumbers - notNumbers;
					if (keywordNumber == 5) {
						andNumbers = 1;
						notNumbers = 1;
						orNumbers = 3;
					}

					List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization,
							orNumbers, andNumbers, notNumbers);

					TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
					Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
					SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
					PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
					System.out.println("requestNumber:" + requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
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

	/**
	 * @param initialization
	 * @param orNumbers
	 * @param andNumbers
	 * @param notNumbers
	 * @return
	 */
	private static List<List<Integer>> getThreeKindsOfKeywordsIndex(Initialization initialization,
																																	int orNumbers, int andNumbers, int notNumbers) {
		Random random = new Random(31);
		List<Integer> orKeywordsIndex = new ArrayList<>(orNumbers);
		List<Integer> andKeywordsIndex = new ArrayList<>(andNumbers);
		List<Integer> notKeywordsIndex = new ArrayList<>(notNumbers);

		int j = 0;
		while (j < orNumbers) {
			int queryIndex = random.nextInt(initialization.simulationDictSize - 1);
			orKeywordsIndex.add(queryIndex);
			j++;
		}

		j = 0;
		while (j < andNumbers) {
			int queryIndex = random.nextInt(initialization.simulationDictSize - 1);
			if (queryIndex != initialization.simulationDictSize - 1 && orKeywordsIndex.indexOf(queryIndex) == -1) {
				andKeywordsIndex.add(queryIndex);
				j++;
			}
		}

		j = 0;
		while (j < notNumbers) {
			int queryIndex = random.nextInt(initialization.simulationDictSize - 1);
			if (queryIndex != initialization.simulationDictSize - 1 && orKeywordsIndex.indexOf(queryIndex) == -1
					&& andKeywordsIndex.indexOf(queryIndex) == -1) {
				notKeywordsIndex.add(queryIndex);
				j++;
			}
		}

		return Arrays.asList(orKeywordsIndex, andKeywordsIndex, notKeywordsIndex);
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
			double[] matrix = initialization.simulationDocuments.get(simulationFileIndex);
			for (int j = 0; j < keywordsIndex.size(); j++) {
				if (Double.compare(matrix[keywordsIndex.get(j)], 0) > 0) {
					System.out.printf("%-10s%-6f\n", keywordsIndex.get(j), matrix[keywordsIndex.get(j)]);
				}
			}
		}
	}

	private static double scoreForPruning(HACTreeNode root, Trapdoor trapdoor) {
		/*return root.pruningVector.times(queryVector).get(0, 0);*/
		return DiagonalMatrixUtils.score(root.pruningVectorPart1, trapdoor.trapdoorPart1) +
				DiagonalMatrixUtils.score(root.pruningVectorPart2, trapdoor.trapdoorPart2);
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		System.out.println(QuerySimulation.class.getName() + " search.");
		System.out.println(new Date());
		long start = System.currentTimeMillis();

		test();
//		System.out.println("testWithFixedDictSizeKeywordNumberRequestNumber");
//		testWithFixedDictSizeKeywordNumberRequestNumber(4000, 20, 20);
////		testWithFixedDocumentNumberKeywordNumberRequestNumber(600,20, 20);
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println("testWithFixedDocumentNumberKeywordNumberRequestNumber");
//		testWithFixedDocumentNumberKeywordNumberRequestNumber(6000,20, 20);
////		testWithFixedDictSizeKeywordNumberRequestNumber(400, 20, 20);
//
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println("testWithFixedDocumentNumberFixedDictSize");
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
}
