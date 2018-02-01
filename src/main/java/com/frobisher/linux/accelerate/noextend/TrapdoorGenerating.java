package com.frobisher.linux.accelerate.noextend;

import com.frobisher.linux.accelerate.DiagonalMatrixUtils;

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
public class TrapdoorGenerating {

	private MySecretKey mySecretKey;
	public Initialization initialization;

	public TrapdoorGenerating(MySecretKey mySecretKey) {
		this.mySecretKey = mySecretKey;
	}

	public TrapdoorGenerating(MySecretKey mySecretKey, Initialization initialization) {
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
	 * @param query
	 * @return
	 */
	public Trapdoor generateTrapdoor(String query) {
		System.out.println("TrapdoorGenerating trapdoorGenerating start.");
		long start = System.currentTimeMillis();

		List<String> keywordList = new ArrayList<>();
		Matcher matcher = Initialization.WORD_PATTERN.matcher(query);
		while (matcher.find()) {
			keywordList.add(matcher.group().toLowerCase());
		}

		Map<String, Double> idfs = generateIDFs(keywordList);

		// 原来的Q矩阵是M*1的。
		/*Matrix Q = new Matrix(Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER, 1);*/
		double[] Q = new double[initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER];

		for (int i = 0; i < keywordList.size(); i++) {
			String keyword = keywordList.get(i);
			int index = initialization.dict.indexOf(keyword);
			if (index != -1) {
				System.out.printf("%-30s\t%-10s\t%.8f\n", keyword, "idf-value", idfs.get(keyword));
				// Q.set(index, 0, idfs.get(keyword));
				Q[index] = idfs.get(keyword);
				/*System.out.printf("%-10s\t%-10s\t%.8f\n", keyword, "Q.get(index, 0)", Q.get(index, 0));*/
			}
		}

		/*System.out.println("Q Qa Qb transponse.");*/

		Random random = new Random(31);

		// Matrix qa = new Matrix(Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER, 1);
		// Matrix qb = new Matrix(Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER, 1);
		double[] qa = new double[initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER];
		double[] qb = new double[initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER];


		for (int i = 0; i < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; i++) {
			// S[i] == 1 相加
			if (mySecretKey.S.get(i)) {
				double rand = random.nextDouble();
				/*double va = Q.get(i, 0) * rand;
				double vb = Q.get(i, 0) * (1 - rand);
				qa.set(i, 0, va);
				qb.set(i, 0, vb);*/

				/*double v = 1.0 / 2.0 * Q.get(i, 0);
				qa.set(i, 0, v + rand);
				qb.set(i, 0, v - rand);*/

				/*double v = Q[i] / 2.0;
				qa[i] = v + rand;
				qb[i] = v - rand;*/

				qa[i] = Q[i] * rand;
				qb[i] = Q[i] * (1 - rand);

				//S[i] == 0 相等
			} else {
				/*qa.set(i, 0, Q.get(i, 0));
				qb.set(i, 0, Q.get(i, 0));*/
				/*qa[i] = Q[i];
				qb[i] = Q[i];*/

				qa[i] = Q[i];
				qb[i] = Q[i];
			}
		}

		/*MatrixUitls.print(qa.transpose());
		MatrixUitls.print(qb.transpose());*/

		/*System.out.println(mySecretKey.M1.getRowDimension() + "\t" + mySecretKey.M2.getColumnDimension());
		System.out.println(inverseM1.getRowDimension() + "\t" +inverseM2.getColumnDimension());
		System.out.println(qa.getRowDimension() + "\t" +qb.getColumnDimension());*/

		/*Matrix part1 = AuxiliaryMatrix.M1Inverse.times(qa);
		Matrix part2 = AuxiliaryMatrix.M2Inverse.times(qb);*/

		double[] part1 = DiagonalMatrixUtils.times(AuxiliaryMatrix.M1Inverse, qa);
		double[] part2 = DiagonalMatrixUtils.times(AuxiliaryMatrix.M2Inverse, qb);
		System.out.println("generate trapdoor total time:" + (System.currentTimeMillis() - start));
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
		TrapdoorGenerating trapdoorGenerating = new TrapdoorGenerating(mySecretKey, initialization);
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
