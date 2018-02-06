package com.frobisher.linux.accelerate.lv;


import com.frobisher.linux.accelerate.DiagonalMatrixUtils;
import com.frobisher.linux.utils.MathUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;

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

	static class ValueComparator implements Comparator<Map.Entry<String, Integer>> {
		@Override
		public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
			return e1.getValue() - e2.getValue();
		}
	}

	/**
	 * @param keywordsIndex
	 * @return
	 */
	public Trapdoor generateTrapdoor(List<List<Integer>> keywordsIndex) {
		System.out.println("TrapdoorGenerating trapdoorGenerating start.");
		long start = System.currentTimeMillis();
		Random random = new Random(31);
		double[] Q = new double[initialization.simulationDictSize];

		List<Integer> orKeywordsIndex = keywordsIndex.get(0);
		List<Integer> andKeywordsIndex = keywordsIndex.get(1);
		List<Integer> notKeywordsIndex = keywordsIndex.get(2);
		double[] orHyperIncreasingSequence = null;
		double[] andHyperIncreasingSequence = null;
		double[] notHyperIncreasingSequence = null;

		int upper = 100;
		// 生成or关键词所对应的超递增序列.
		if (orKeywordsIndex != null) {
			orHyperIncreasingSequence = MathUtils.generateHyperIncreasingSequence(orKeywordsIndex.size(), upper);
		}
//		System.out.println("or hyper sequence:" + Arrays.toString(orHyperIncreasingSequence));

		if (andKeywordsIndex != null) {
			andHyperIncreasingSequence = new double[andKeywordsIndex.size()];
			if (orHyperIncreasingSequence != null) {
				double sum = Arrays.stream(orHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
				for (int i = 0; i < andKeywordsIndex.size(); i++) {
					andHyperIncreasingSequence[i] = sum + upper * Initialization.RANDOM.nextInt(upper);
				}
			}
		}
//		System.out.println("and hyper sequence:" + Arrays.toString(andHyperIncreasingSequence));

		if (notKeywordsIndex != null) {
			double orSum = 0;
			double andSum = 0;
			notHyperIncreasingSequence = new double[notKeywordsIndex.size()];
			if (orHyperIncreasingSequence != null) {
				orSum = Arrays.stream(orHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
			}
			if (andHyperIncreasingSequence != null) {
				andSum = Arrays.stream(andHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
			}

			for (int i = 0; i < notKeywordsIndex.size(); i++) {
				notHyperIncreasingSequence[i] = -(orSum + andSum + upper * Initialization.RANDOM.nextInt(upper));
			}
		}

//		System.out.println("not hyper sequence:" + Arrays.toString(notHyperIncreasingSequence));

		/**
		 * 根据FMS_II的思路,
		 * s是and所对应的关键词的超递增序列的和.
		 */
		double s = 0;
		if (andHyperIncreasingSequence != null) {
			s = Arrays.stream(andHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
		}
		// r作为任意的随机数即可.
		int r = 1 + Initialization.RANDOM.nextInt(upper);

		/**
		 * the corresponding values in Q are set as
		 * (a1, a2,.. al1; b1, b2...bl2; -c1,-c2...-cl3).
		 * Other values in Q are set as 0.
		 */
		int count = 0;
		if (orHyperIncreasingSequence != null) {
			for (int index : orKeywordsIndex) {
				Q[index] = orHyperIncreasingSequence[count++];
			}
		}

		count = 0;
		if (andHyperIncreasingSequence != null) {
			for (int index : andKeywordsIndex) {
				Q[index] = andHyperIncreasingSequence[count++];
			}
		}

		count = 0;
		if (notHyperIncreasingSequence != null) {
			for (int index : notKeywordsIndex) {
				Q[index] = notHyperIncreasingSequence[count++];
			}
		}

		// 最后一位要要设置为 -s 即and连接的关键词的权重。
		Q[initialization.simulationDictSize - 1] =  -s;

		// Q 更新为 rQ
		Q = DiagonalMatrixUtils.times(Q, r);
		double[] qa = new double[initialization.simulationDictSize];
		double[] qb = new double[initialization.simulationDictSize];


		for (int i = 0; i < initialization.simulationDictSize; i++) {
			// S[i] == 1;
			if (mySecretKey.S.get(i)) {
				double v1 = random.nextDouble();
				qa[i] = Q[i] * v1;
				qb[i] = Q[i] * (1 - v1);

				//S[i] == 0;
			} else {
				qa[i] = Q[i];
				qb[i] = Q[i];
			}
		}

		/*MatrixUitls.print(qa.transpose());
		MatrixUitls.print(qb.transpose());*/

		/*System.out.println(mySecretKey.M1.getRowDimension() + "\t" + mySecretKey.M2.getColumnDimension());
		System.out.println(inverseM1.getRowDimension() + "\t" +inverseM2.getColumnDimension());
		System.out.println(qa.getRowDimension() + "\t" +qb.getColumnDimension());*/

		double[] part1 = DiagonalMatrixUtils.times(AuxiliaryMatrix.M1Inverse, qa);
		double[] part2 = DiagonalMatrixUtils.times(AuxiliaryMatrix.M2Inverse, qb);
		System.out.println("generate trapdoor total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("TrapdoorGenerating trapdoorGenerating finished.");
		return new Trapdoor(part1, part2);
	}

	private Map<String, Double> generateIDFs(List<String> keywordList) {
		double sum = 0;
		// 文档的个数.
		List<Double> keywordIDFLists = new ArrayList<>(keywordList.size());
		int fileNumber = initialization.fileLength.size();
		for (int i = 0; i < keywordList.size(); i++) {
			String keyword = keywordList.get(i);
			if (initialization.dict.contains(keyword)) {
				// sum(ln(1 + m / fwi) ^ 2)
				double idf = Math.log(1 + fileNumber / initialization.numberOfDocumentContainsKeyword.get(keyword));
				sum += Math.pow(idf, 2);
				keywordIDFLists.add(idf);
			} else {
				keywordIDFLists.add(0.0);
			}
		}

		double denominator = Math.sqrt(sum);
		Map<String, Double> idfs = new HashMap<>(keywordList.size());
		for (int i = 0; i < keywordList.size(); i++) {
			String keyword = keywordList.get(i);
			idfs.put(keyword, keywordIDFLists.get(i));
		}

		return idfs;
	}

	private double idfDenominator(List<String> keywordList) {
		double sum = 0;
		// 文档的个数.
		int fileNumber = initialization.fileLength.size();
		for (int i = 0; i < keywordList.size(); i++) {
			String keyword = keywordList.get(i);
			if (initialization.dict.contains(keyword)) {
				// sum(ln(1 + m / fwi) ^ 2)
				sum += Math.pow(Math.log(1 + fileNumber / initialization.numberOfDocumentContainsKeyword.get(keyword)), 2);
			}
		}
		return Math.sqrt(sum);
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

		Map<String, Double> idfs = trapdoorGenerating.generateIDFs(keywordList);
		for (int i = 0; i < keywordList.size(); i++) {
			System.out.printf("%-15s\t %.6f\n", keywordList.get(i), idfs.get(keywordList.get(i)));
		}
	}
}
