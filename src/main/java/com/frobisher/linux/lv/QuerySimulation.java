package com.frobisher.linux.lv;

import Jama.Matrix;
import com.frobisher.linux.pv.HACTreeNode;
import com.frobisher.linux.pv.Initialization;
import com.frobisher.linux.pv.MySecretKey;
import com.frobisher.linux.pv.Trapdoor;

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

	public static void test2() {
		try {
			Initialization initialization = new Initialization();
			// 没有写textrank和plain文档分离的版本。
			// 只是new def了两个函数。
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(3, 5,
					600, 600);

			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
//			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
//			// 基于TextRank.
//			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndexWithTextRank();
			System.out.println("HACTreeIndexBuilding build & encrypt index start...");
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
			int notNumbers = 3;

			List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);

			TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);

			// for-40
       int requestNumber1 = 4;
			List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.simulationDocumentNumber * 0.001);
//			int high = (int) Math.ceil(initialization.simulationDocumentNumber * 0.01);
			int low = 2;
			int high = 10;
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
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());

				System.out.println("\nrequestNumber:" + requestNumber + "\t" + keywordsIndex);

				// 验证搜索结果是否包含特定的文档。
				searchResultVerify(initialization, filenameList, keywordsIndex, nodeScoreMap);
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
			if (orKeywordsIndex.indexOf(queryIndex) == -1) {
				andKeywordsIndex.add(queryIndex);
				j++;
			}
		}

		j = 0;
		while (j < notNumbers) {
			int queryIndex = random.nextInt(initialization.simulationDictSize - 1);
			if (orKeywordsIndex.indexOf(queryIndex) == -1
					&& andKeywordsIndex.indexOf(queryIndex) == -1) {
				notKeywordsIndex.add(queryIndex);
				j++;
			}
		}

		return Arrays.asList(orKeywordsIndex, andKeywordsIndex, notKeywordsIndex);
	}

	public static void testWithFixedDocumentNumberAndQueryNumber() {
		testWithFixedDocumentNumberAndQueryNumber(6000, 20);
	}

	/**
	 * 测试，固定文档数目和查询数目
	 * @param documentNumber 实验中对比数据是 6000
	 * @param requestNumber  实验中对比数据是 20
	 */
	public static void testWithFixedDocumentNumberAndQueryNumber(int documentNumber, int requestNumber) {
		try {
			List<Integer> dictSizeList = Arrays.asList(2000, 4000, 6000, 8000, 10000);
			for (int i = 0; i < dictSizeList.size(); i++) {
				System.out.println();
				System.err.println("documentNumber:" + documentNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSizeList.get(i));
				System.out.println("documentNumber:" + documentNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSizeList.get(i));
				Initialization initialization = new Initialization();
				MySecretKey mySecretKey = initialization.getMySecretKeySimulation(3, 5,
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

				Random random = new Random(31);

				// attention, 和 prefer版本不同

				int orNumbers = 5;
				int andNumbers = 2;
				int notNumbers = 3;

				List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);


				TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
				}
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
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

	public static void testWithFixedDictSizeAndQueryNumber() {
		testWithFixedDictSizeAndQueryNumber(4000, 20);
	}

	/**
	 * 测试固定数目的
	 * @param dictSize
	 * @param requestNumber
	 */
	public static void testWithFixedDictSizeAndQueryNumber(int dictSize, int requestNumber) {
		try {
			List<Integer> documentNumberList = Arrays.asList(2000, 4000, 6000, 8000, 10000);
			for (int i = 0; i < documentNumberList.size(); i++) {
				System.out.println();
				int documentNumber = documentNumberList.get(i);
				System.err.println("documentNumber:" + documentNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
				System.out.println("documentNumber:" + documentNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
				Initialization initialization = new Initialization();
				MySecretKey mySecretKey = initialization.getMySecretKeySimulation(3, 5,
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

				int orNumbers = 5;
				int andNumbers = 2;
				int notNumbers = 3;

				List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);


				TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
				}
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
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

	public static void testWithFixedDocumentNumberFixedDictSize() {
		testWithFixedDocumentNumberFixedDictSize(6000, 4000);
	}


	/**
	 * 测试固定数目的
	 * @param documentNumber
	 * @param dictSize
	 */
	public static void testWithFixedDocumentNumberFixedDictSize(int documentNumber, int dictSize) {
		try {
			List<Integer> requestNumberList = Arrays.asList(10, 20, 30, 40, 50);
			for (int i = 0; i < requestNumberList.size(); i++) {
				System.out.println();
				int requestNumber = requestNumberList.get(i);
				System.err.println("documentNumber:" + documentNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
				System.out.println("documentNumber:" + documentNumber + "\trequestNumber:" + requestNumber + "\tdictSize:" + dictSize);
				Initialization initialization = new Initialization();
				MySecretKey mySecretKey = initialization.getMySecretKeySimulation(3, 5,
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

				int orNumbers = 5;
				int andNumbers = 2;
				int notNumbers = 3;

				List<List<Integer>> keywordsIndex = getThreeKindsOfKeywordsIndex(initialization, orNumbers, andNumbers, notNumbers);


				TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
				Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);
				SearchAlgorithmSimulation searchAlgorithm = new SearchAlgorithmSimulation();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("requestNumber:"+ requestNumber + "\tpriorityQueue.size():" + priorityQueue.size());
				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
				}
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
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

	private static void searchResultVerify(Initialization initialization, List<String> filenameList, List<List<Integer>> keywordsIndex, Map<String, Double> nodeScoreMap) throws IOException {
		System.out.println();
		for (int fileIndex = 0; fileIndex < filenameList.size(); fileIndex++) {
			System.out.println();
			System.out.println(filenameList.get(fileIndex) + "\tscore:" + nodeScoreMap.get(filenameList.get(fileIndex)));
			String filename = filenameList.get(fileIndex);
			int simulationFileIndex = Integer.valueOf(filename);
			Matrix matrix = initialization.simulationDocuments.get(simulationFileIndex);
			for (int j = 0; j < keywordsIndex.size(); j++) {
				List<Integer> keywordsIndexKinds = keywordsIndex.get(j);
				for (int k = 0; k < keywordsIndexKinds.size(); k++) {
					if (matrix.get(keywordsIndexKinds.get(k), 0) > 0) {
						System.out.println(keywordsIndexKinds.get(k) + "\t\t\t" + matrix.get(keywordsIndexKinds.get(k), 0));
					}
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

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		System.out.println(QuerySimulation.class.getName() + " search.");
//		test2();
		System.err.println("testWithFixedDocumentNumberAndQueryNumber");
		testWithFixedDocumentNumberAndQueryNumber(6000, 20);
		System.out.println();
		System.out.println();
		System.out.println();
		System.err.println("testWithFixedDictSizeAndQueryNumber");
		testWithFixedDictSizeAndQueryNumber(4000, 20);

		System.out.println();
		System.out.println();
		System.out.println();
		System.err.println("testWithFixedDocumentNumberFixedDictSize");
		testWithFixedDocumentNumberFixedDictSize(6000, 4000);

	}
}
