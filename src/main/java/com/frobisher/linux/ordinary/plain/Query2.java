package com.frobisher.linux.ordinary.plain;

import Jama.Matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

	public static void test1() {
		try {
			Initialization initialization = new Initialization();
			MySecretKey mySecretKey = initialization.getMySecretKey();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey);
			hacTreeIndexBuilding.buildHACTreeIndex();

			String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";

			System.out.println("Query2 start generating trapdoor.");
			TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey);
			trapdoorGenerating.generateTrapdoor(query);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static void test2() {
		try {
			Initialization initialization = new Initialization();
			MySecretKey mySecretKey = initialization.getMySecretKey();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);

			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
			// System.out.println(root);

			// for-16
			// String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";

			// for-40
			// String query = "clinton broadcasting voice Francis honorary citizenship Democratic Revolution church president conferences";
			String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";

			System.out.println("Query2 start generating trapdoor.");
			TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);

			Matrix queryVector = trapdoorGenerating.generateTrapdoor(query);

			// for-40
			int requestNumber1 = 10;
			List<Integer> requestNumberList = new ArrayList<>();
//			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.01);
//			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.1);
			int low = 5;
			int high = 5;
			for (int i = low; i <= high; i += low) {
				requestNumberList.add(i);
			}

			// Arrays.asList(5, 10, 15, 20, 25, 30, 40, 50, 60, 80)
			for (int requestNumber : requestNumberList) {
				SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, queryVector, requestNumber);
				System.out.println("Query2 priorityQueue.size():" + priorityQueue.size());
				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, queryVector));
				}
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
				String keywordPatternStr = getQueryPattern(query);

				System.out.println("\n requestNumber:" + requestNumber + "\t" + query);

				// 验证搜索结果是否包含特定的文档。
				searchResultVerify(initialization, filenameList, keywordPatternStr, nodeScoreMap);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根节点和Trapdoor之间的相关性评分.
	 *
	 * @param root
	 * @param queryVector
	 * @return
	 */
	private static double scoreForPruning(HACTreeNode root, Matrix queryVector) {
		return root.pruningVector.times(queryVector).get(0, 0);
	}

	private static void searchResultVerify(Initialization initialization, List<String> filenameList, String keywordPatternStr, Map<String, Double> nodeScoreMap) throws IOException {
		Pattern keywordPattern = Pattern.compile(keywordPatternStr);
		for (int i = 0; i < filenameList.size(); i++) {
			System.out.println(filenameList.get(i) + "\tscore:" + nodeScoreMap.get(filenameList.get(i)));
			String separator = "\\";
			if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
				separator = "/";
			}
			List<String> allLines = Files.readAllLines(new File(initialization.PLAIN_DIR + separator + filenameList.get(i)).toPath());
			String passage = allLines.stream().map(String::toLowerCase).collect(joining("\n"));

			Matcher matcher = keywordPattern.matcher(passage);
			int count = 0;
			while (matcher.find()) {
				String keyword = matcher.group().toLowerCase();
				/*System.out.println(filenameArray[i] + "\t" + keyword + "\t" + Initialization.keywordFrequencyInDocument.get(filenameArray[i]).get(keyword) + "\t" + "documentNumber\t" + Initialization.numberOfDocumentContainsKeyword.get(keyword));*/
				/*System.out.printf("%-60s\t%-15s\t%-10s%-15s\t%10s\n", filenameList.get(i), keyword,
						Initialization.keywordFrequencyInDocument.get(filenameList.get(i)).get(keyword),
						"docsNumber", Initialization.numberOfDocumentContainsKeyword.get(keyword));*/
				System.out.printf("%-15s\t%-10s%-15s\t%10s\n", keyword,
						initialization.keywordFrequencyInDocument.get(filenameList.get(i)).get(keyword),
						"docsNumber", initialization.numberOfDocumentContainsKeyword.get(keyword));
				count++;
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
		System.out.println("plain search.");

		/*test1();*/

		test2();
	}
}
