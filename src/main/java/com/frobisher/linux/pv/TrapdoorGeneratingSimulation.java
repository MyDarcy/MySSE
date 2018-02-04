package com.frobisher.linux.pv;

import Jama.Matrix;
import com.frobisher.linux.utils.MathUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * author: darcy
 * date: 2017/11/18 20:27
 * description:
 *
 * 第三阶段: 陷门的生成.
*/
public class TrapdoorGeneratingSimulation {

	private MySecretKey mySecretKey;
	public Initialization initialization;

	public TrapdoorGeneratingSimulation(MySecretKey mySecretKey) {
		this.mySecretKey = mySecretKey;
	}

	public TrapdoorGeneratingSimulation(MySecretKey mySecretKey, Initialization initialization) {
		this.mySecretKey = mySecretKey;
		this.initialization = initialization;
	}

	static class ValueComparator implements Comparator<Map.Entry<Integer, Integer>> {
		@Override
		public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
			return e1.getValue() - e2.getValue();
		}
	}

	/**
	 *
	 * @param keywordsIndex
	 * @return
	 */
	public Trapdoor generateTrapdoor(List<Integer> keywordsIndex) {
		System.out.println("TrapdoorGenerating trapdoorGenerating start.");
		long start = System.currentTimeMillis();

		/**
		 * 关键词的偏好因子.
		 *
		 */
		Map<Integer, Integer> interestModel = new HashMap<>();
		List<Integer> indices = IntStream.range(0, initialization.simulationDummykeywordIndexSet.size()).boxed().collect(Collectors.toList());
		for (Integer i : indices) {
			interestModel.put(i, 0);
		}

		// 18, 122, 144, 290, 450, 800, 1200, 2500, 3712, 4817, 5412, 6611, 7810
		interestModel.put(18, 4);
		interestModel.put(122, 9);
		interestModel.put(144, 2);
		interestModel.put(290, 5);
		interestModel.put(450, 17);
		interestModel.put(800, 3);
		interestModel.put(800, 5);
		interestModel.put(1200, 4);
		interestModel.put(2500, 2);
		interestModel.put(3712, 1);
		interestModel.put(4817, 4);
		interestModel.put(5412, 7);
		interestModel.put(6611, 12);
		interestModel.put(7810, 10);

		Matrix Q = new Matrix(initialization.simulationDictSize, 1);

		// 超递增序列是关键词的偏好因子.
		double factor = 0.2;
		double[] hyperIncreasingSequence = MathUtils.generateHyperIncreasingSequence(keywordsIndex.size(), factor);
		List<Map.Entry<Integer, Integer>> keyPreferenceList = new ArrayList<>();
		keyPreferenceList.addAll(interestModel.entrySet());
		ValueComparator vc = new ValueComparator();
		Collections.sort(keyPreferenceList, vc);

		/**
		 * 生成搜索关键词:偏好因子的映射关系.
		 */
		Map<Integer, Double> preferenceFactors = new HashMap<>();
		// list中的元素按照 重要性排序.
		int count = 0;
		for (Map.Entry<Integer, Integer> item : keyPreferenceList) {
			// 现在list中的关键词重要性是递增的.
			// keywordList的大小和超递增序列的大小是相同的.
			// 那么就可以实现 搜索关键词和偏好因子的对应.
			double pFactor = hyperIncreasingSequence[count++];
			System.out.printf("%-20s%-15d%.8f\n", item.getKey(), interestModel.get(item.getKey()), pFactor);
			preferenceFactors.put(item.getKey(), pFactor);
			// 找不到, 那么构建查询陷门也用不到此关键词
		}

		for (int i = 0; i < keywordsIndex.size(); i++) {
			Integer index = keywordsIndex.get(i);
				Double preferenceFacotr = preferenceFactors.get(index);
				System.out.printf("%-20s%-15s%.8f\n", index, "preference", preferenceFacotr);
				Q.set(index, 0, preferenceFacotr);

		}

		// 之前一直都忘记了这一部分。
		for (Integer index : initialization.simulationDummykeywordIndexSet) {
			if (Initialization.RANDOM.nextBoolean()) {
				Q.set(index, 0, 1);
			}
		}

		Random random = new Random(31);

		Matrix qa = new Matrix(initialization.simulationDictSize, 1);
		Matrix qb = new Matrix(initialization.simulationDictSize, 1);


		for (int i = 0; i < initialization.simulationDictSize; i++) {
			// S[i] == 1;
			if (mySecretKey.S.get(i)) {
				double v1 = random.nextDouble();
				qa.set(i, 0, Q.get(i, 0) * v1);
				qb.set(i, 0, Q.get(i, 0) * (1 - v1));

				//S[i] == 0;
			} else {
				qa.set(i, 0, Q.get(i, 0));
				qb.set(i, 0, Q.get(i, 0));
			}
		}

		/*MatrixUitls.print(qa.transpose());
		MatrixUitls.print(qb.transpose());*/

		/*System.out.println(mySecretKey.M1.getRowDimension() + "\t" + mySecretKey.M2.getColumnDimension());
		System.out.println(inverseM1.getRowDimension() + "\t" +inverseM2.getColumnDimension());
		System.out.println(qa.getRowDimension() + "\t" +qb.getColumnDimension());*/

		Matrix part1 = AuxiliaryMatrix.M1Inverse.times(qa);
		Matrix part2 = AuxiliaryMatrix.M2Inverse.times(qb);
		System.out.println("generate trapdoor total time:" + (System.currentTimeMillis() - start));
		System.out.println("TrapdoorGenerating trapdoorGenerating finished.");
		return new Trapdoor(part1, part2);
	}

	public static void main(String[] args) throws IOException {
		Initialization initialization = new Initialization();
		MySecretKey mySecretKey = initialization.getMySecretKey();
		TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(mySecretKey, initialization);
		String query = "Pope Francis honorary citizenship Democratic Revolution clinton owners oversee would half pick";
		List<String> keywordList = new ArrayList<>();
		Matcher matcher = Initialization.WORD_PATTERN.matcher(query);
		while (matcher.find()) {
			keywordList.add(matcher.group().toLowerCase());
		}

		System.out.println(initialization.dict.contains("clinton"));
		System.out.println(initialization.numberOfDocumentContainsKeyword.keySet().contains("clinton"));

		for (int i = 0; i < keywordList.size(); i++) {
			String keyword = keywordList.get(i);
			if (initialization.dict.contains(keyword)) {
				System.out.printf("%-15s\t %6d\n", keyword, initialization.numberOfDocumentContainsKeyword.get(keyword));
			} else {
				System.out.printf("%-15s\t %6d\n", keyword, 0);
			}
		}
	}
}
