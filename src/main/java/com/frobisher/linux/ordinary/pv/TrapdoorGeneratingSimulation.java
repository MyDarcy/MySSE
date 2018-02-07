package com.frobisher.linux.ordinary.pv;

import Jama.Matrix;
import com.frobisher.linux.utils.MathUtils;

import java.io.IOException;
import java.util.*;

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
//		List<Integer> indices = IntStream.range(0, initialization.simulationDocuments.size()).boxed().collect(Collectors.toList());
//		for (Integer i : indices) {
//			interestModel.put(i, 0);
//		}

		// 18, 122, 144, 290, 450, 800, 1200, 2500, 3712, 4817, 5412, 6611, 7810
//		interestModel.put(18, 4);
//		interestModel.put(122, 9);
//		interestModel.put(144, 2);
//		interestModel.put(290, 5);
//		interestModel.put(450, 17);
//		interestModel.put(800, 3);
//		interestModel.put(800, 5);
//		interestModel.put(1200, 4);
//		interestModel.put(2500, 2);
//		interestModel.put(3712, 1);
//		interestModel.put(4817, 4);
//		interestModel.put(5412, 7);
//		interestModel.put(6611, 12);
//		interestModel.put(7810, 10);

		// 18, 45, 89, 198, 256, 390, 450, 680, 789, 812, 907, 1222, 1368, 1450
//		interestModel.put(18, 4);
//		interestModel.put(45, 9);
//		interestModel.put(89, 2);
//		interestModel.put(198, 5);
//		interestModel.put(256, 17);
//		interestModel.put(390, 3);
//		interestModel.put(450, 5);
//		interestModel.put(680, 4);
//		interestModel.put(789, 2);
//		interestModel.put(812, 1);
//		interestModel.put(907, 4);
//		interestModel.put(1222, 7);
//		interestModel.put(1368, 12);
//		interestModel.put(1450, 10);

		Random random = new Random(31);

//		List<Integer> factors = Arrays.asList(4, 9, 2, 5, 17, 3, 5, 4, 2, 1, 4, 7, 12, 10, 7, 1, 4, 9, 3, 2);
		List<Integer> factors = new ArrayList<>(keywordsIndex.size());
		for (int i = 0; i < keywordsIndex.size(); i++) {
			factors.add(random.nextInt(25));
		}

		for (int i = 0; i < factors.size(); i++) {
			interestModel.put(keywordsIndex.get(i), factors.get(i));
		}

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
//			System.out.printf("%-20s%-15d%.8f\n", item.getKey(), interestModel.get(item.getKey()), pFactor);
			preferenceFactors.put(item.getKey(), pFactor);
		}

//		System.out.println();
		for (int i = 0; i < keywordsIndex.size(); i++) {
			Integer index = keywordsIndex.get(i);
			double preferenceFactor = preferenceFactors.get(index);
//			System.out.printf("%-20s%-15s%.8f\n", index, "preference", preferenceFactor);
			Q.set(index, 0, preferenceFactor);
		}

		// 之前一直都忘记了这一部分。
		for (Integer index : initialization.simulationDummykeywordIndexSet) {
			if (Initialization.RANDOM.nextBoolean()) {
				Q.set(index, 0, 1);
			}
		}

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
		System.out.println("generate trapdoor total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("TrapdoorGenerating trapdoorGenerating finished.");
		return new Trapdoor(part1, part2);
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

	public static void main(String[] args) throws IOException {
		/**
		 * 论文中是陷门生成是按照关键词的数目生成的.
		 * 即指定查询中关键词数目, 然后生成.
		 */
		Initialization initialization = new Initialization();
		MySecretKey mySecretKey = initialization.getMySecretKeySimulation(3, 5,
				6000, 4000);
		HACTreeIndexBuildingSimulation indexBuilding = new HACTreeIndexBuildingSimulation(
				mySecretKey, initialization);
		indexBuilding.generateAuxiliaryMatrix();
		// 要生成转置矩阵，逆矩阵然后才能生成测试陷门的生成.
		List<Integer> keywordNumberList = Arrays.asList(10, 20, 30, 40, 50);
		for (int i = 0; i < keywordNumberList.size(); i++) {
			System.out.println();
			Integer keywordNumber = keywordNumberList.get(i);
			List<Integer> keywordsIndex = getKeywordsIndex(initialization, keywordNumber);
			TrapdoorGeneratingSimulation trapdoorGenerating = new TrapdoorGeneratingSimulation(
					mySecretKey, initialization);
			trapdoorGenerating.generateTrapdoor(keywordsIndex);
		}
	}
}
