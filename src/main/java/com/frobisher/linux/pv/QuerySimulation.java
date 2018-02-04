package com.frobisher.linux.pv;

import Jama.Matrix;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
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
public class QuerySimulation {

	public static void test2() {
		try {
			Initialization initialization = new Initialization();
			// 没有写textrank和plain文档分离的版本。
			// 只是new def了两个函数。
			MySecretKey mySecretKey = initialization.getMySecretKeySimulation(100, 1500);

			// 这个的问题在于fileLength没有统计出来，生成消息摘要会出现问题。
//			MySecretKey mySecretKey = initialization.getMySecretKeyWithTextRank();
			HACTreeIndexBuildingSimulation hacTreeIndexBuilding = new HACTreeIndexBuildingSimulation(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
			// 基于TextRank.
//			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndexWithTextRank();
			System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex start...");
			long start = System.currentTimeMillis();
			hacTreeIndexBuilding.encryptHACTreeIndex(root);
			System.out.println("time:" + (System.currentTimeMillis() - start) + "ms");
			System.out.println("HACTreeIndexBuilding.encryptHACTreeIndex end.");

			List<Integer> keywordsIndex = Arrays.asList(
					18, 122, 144, 290, 450, 800, 1200, 2500, 3712, 4817, 5412, 6611, 7810
			);

			System.out.println("Query2 start generating trapdoor.");
			TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(keywordsIndex);

			// for-40
       int requestNumber1 = 4;
			List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.02);
//			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.2);
			int low = 2;
			int high = 2;
			for (int i = low; i <= high; i += low) {
				requestNumberList.add(i);
			}

			// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
			for (int requestNumber : requestNumberList) {
				SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("Query2 priorityQueue.size():" + priorityQueue.size());
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

	private static void searchResultVerify(Initialization initialization, List<String> filenameList, List<Integer> keywordsIndex, Map<String, Double> nodeScoreMap) throws IOException {
		System.out.println();
		for (int i = 0; i < filenameList.size(); i++) {
			System.out.println(filenameList.get(i) + "\tscore:" + nodeScoreMap.get(filenameList.get(i)));
			String filename = filenameList.get(i);
			int simulationFileIndex = Integer.valueOf(filename);
			Matrix matrix = initialization.simulationDocuments.get(simulationFileIndex);
			for (int j = 0; j < keywordsIndex.size(); j++) {
				System.out.printf("%-10s%-6f\n", keywordsIndex.get(j), matrix.get(simulationFileIndex, 0));
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
		System.out.println("pv search.");
		test2();
	}
}
