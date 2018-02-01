package com.frobisher.linux.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * author: darcy
 * date: 2018/1/19 9:10
 * description: 
*/
public class TextRankUtils {

	public static Map<String, Map<String, Double>> textRankAllFilesToMap(String parentName) {
		Map<String, Map<String, Double>> fileTextRankMap = new HashMap<>();
		try {
			File[] allFiles = new File(parentName).listFiles();
			for (int i = 0; i < allFiles.length; i++) {
				fileTextRankMap.put(allFiles[i].getName(), textRankFileToMap(allFiles[i].toPath()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileTextRankMap;
	}

	public static Map<String, Double> textRankFileToMap(Path path) throws IOException {
		List<String> wordWeightLines = Files.readAllLines(path);
		String separator = "\\s+";
		Map<String, Double> wordWeightMap = new HashMap<>();
		/*wordWeightLines.stream()
				.map((str) -> str.split(separator))
				.filter((String[] array) -> array.length == 2)
				.forEach((String[] array) -> {wordWeightMap.put(array[0], Double.parseDouble(array[1]));
		});*/

		wordWeightLines.stream()
				.map((line) -> line.split(separator))
				.forEach((String[] array) -> {
					if (array.length == 2) {
						wordWeightMap.put(array[0], Double.parseDouble(array[1]));

						// 可能提取出来包含多个关键词的短语。譬如 american influence  0.5633
					} else {
						Double score = Double.parseDouble(array[array.length - 1]);
						for (int i = 0; i < array.length - 1; i++) {
							wordWeightMap.put(array[i], score);
						}
					}
				});
		return wordWeightMap;
	}


	public static Map<String, Double> textRankFileToMap(String filename) throws IOException {
		return textRankFileToMap(Paths.get(filename));
	}

	public static void main(String[] args) throws IOException {
		String parentName = "D:\\MrDarcy\\ForGraduationWorks\\Code\\TextRank-master\\textrank\\doc\\1000\\keywords";
		File[] files = new File(parentName).listFiles();
		for (int i = 0; i < files.length; i++) {
			System.out.println("=========== " + files[i].getName() + " =============");
			Map<String, Double> wordWeightMap = textRankFileToMap(files[i].toPath());
			System.out.println(wordWeightMap);
		}

		System.out.println();

		Map<String, Map<String, Double>> fileTextRankMap = textRankAllFilesToMap(parentName);
		for (Map.Entry<String, Map<String, Double>> item : fileTextRankMap.entrySet()) {
			System.out.println(item.getKey() + "  " + item.getValue().size());
		}
	}
}
