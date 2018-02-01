package com.frobisher.linux.accelerate.lv;

import com.frobisher.linux.accelerate.DiagonalMatrixUtils;

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
public class Query2 {

	public static void test2() {
		try {
			Initialization initialization = new Initialization();
			MySecretKey mySecretKey = initialization.getMySecretKey();
			HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);
			hacTreeIndexBuilding.encryptFiles();
			hacTreeIndexBuilding.generateAuxiliaryMatrix();
			HACTreeNode root = hacTreeIndexBuilding.buildHACTreeIndex();
			// System.out.println(root);

			// for-16
			// String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";

			// for-40
      // String query = "clinton broadcasting voice Francis honorary citizenship Democratic Revolution church president conferences";
			// String query = "church China hospital performance British interview Democratic citizenship broadcasting voice";
			String query = "|(church china) &(make took) !(status human)";

//			unavailable=2 salary=4 guards=15  canceling=2  creative=22, coast=50
//			before=403, world=389, while=386, against=331, still=331, three=330, during=302, between=300, called=300, another=299,american=238, days=236, public=235,
//			discuss=30, managed=30, seconds=30, agent=30,zone=28, division=28, mention=28, physical=28, alive=28, legislation=28, nuclear=28, equipment=28, wonderful=28,
//			supervisor=7, admire=7,authorization=6, predicting=6,  tuition=4,emotion=5, wasted=6,samsung=7,

			query = "|(church china physical legislation) &(before while during american) !(tuition status human predicting authorization)";
					System.out.println("Query2 start generating trapdoor.");
			TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
			Trapdoor trapdoor = trapdoorGenerating.generateTrapdoor(query);

			// for-40
       int requestNumber1 = 50;
			// int requestNumber = 6;

			List<Integer> requestNumberList = new ArrayList<>();
			int low = (int) Math.ceil(initialization.DOC_NUMBER * 0.01);
			int high = (int) Math.ceil(initialization.DOC_NUMBER * 0.2);
			for (int i = low; i <= high; i += low) {
				requestNumberList.add(i);
			}

			for (int requestNumber : requestNumberList) {
				SearchAlgorithm searchAlgorithm = new SearchAlgorithm();
				PriorityQueue<HACTreeNode> priorityQueue = searchAlgorithm.search(root, trapdoor, requestNumber);
				System.out.println("\nQuery2 priorityQueue.size():" + priorityQueue.size());

				Map<String, Double> nodeScoreMap = new HashMap<>();
				for (HACTreeNode node : priorityQueue) {
					nodeScoreMap.put(node.fileDescriptor, scoreForPruning(node, trapdoor));
				}
				List<String> filenameList = priorityQueue.stream().map((node) -> node.fileDescriptor).collect(toList());
				String keywordPatternStr = getQueryPattern(query);
				System.out.println("\nrequestNumber:" + requestNumber + "\t" + query);

				// 验证搜索结果是否包含特定的文档。
				searchResultVerify(initialization, filenameList, keywordPatternStr, nodeScoreMap);
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
				if (initialization.keywordFrequencyInDocument.get(filenameList.get(i)).get(keyword) != null) {
					System.out.printf("%-15s\t%-10s%-15s\t%10s\n", keyword,
							initialization.keywordFrequencyInDocument.get(filenameList.get(i)).get(keyword),
							"docsNumber", initialization.numberOfDocumentContainsKeyword.get(keyword));
				  count++;
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
		System.out.println("accelerate lv search.");
		test2();
	}
}
