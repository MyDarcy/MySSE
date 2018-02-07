package com.frobisher.linux.ordinary.lv;

import Jama.Matrix;
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

		/**
		 * FMS_II中需要关键词按照重要性排序, 但是单纯的逻辑操作时跟关键词的重要性没有关系.
		 * 后面这里可能需要调整.
		 */
		/*List<Map.Entry<String, Integer>> list = new ArrayList<>();
		list.addAll(null);
		ValueComparator vc = new ValueComparator();
		Collections.sort(list, vc);*/

		/*System.out.println(list);*/

		String[] orKeywords = null;
		String[] andKeywords = null;
		String[] notKeywords = null;

		double[] orHyperIncreasingSequence = null;
		double[] andHyperIncreasingSequence = null;
		double[] notHyperIncreasingSequence = null;

		int orCharIndex = query.indexOf('|');
		// 说明查询支付字符串中有or操作符.
		if (orCharIndex != -1) {
			int last = query.indexOf(')', orCharIndex);
			String temp = query.substring(orCharIndex + 2, last);
			orKeywords = temp.split("\\s+");
		}
		System.err.println("orKeywords:" + Arrays.toString(orKeywords));

		int andCharIndex = query.indexOf('&');
		if (andCharIndex != -1) {
			int last = query.indexOf(')', andCharIndex);
			String temp = query.substring(andCharIndex + 2, last);
			andKeywords = temp.split("\\s+");
		}
		System.err.println("andKeywords:" + Arrays.toString(andKeywords));

		int notCharIndex = query.indexOf('!');
		if (notCharIndex != -1) {
			int last = query.indexOf(')', notCharIndex);
			String temp = query.substring(notCharIndex + 2, last);
			notKeywords = temp.split("\\s+");
		}
		System.err.println("notKeywords:" + Arrays.toString(notKeywords));


		int upper = 100;
		// 生成or关键词所对应的超递增序列.
		if (orKeywords != null) {
			orHyperIncreasingSequence = MathUtils.generateHyperIncreasingSequence(orKeywords.length, upper);
		}
		System.out.println("or hyper sequence:" + Arrays.toString(orHyperIncreasingSequence));

		if (andKeywords != null) {
			andHyperIncreasingSequence = new double[andKeywords.length];
			if (orHyperIncreasingSequence != null) {
				double sum = Arrays.stream(orHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
				for (int i = 0; i < andKeywords.length; i++) {
					andHyperIncreasingSequence[i] = sum + upper * Initialization.RANDOM.nextInt(upper);
				}
			}
		}
		System.out.println("and hyper sequence:" + Arrays.toString(andHyperIncreasingSequence));

		if (notKeywords != null) {
			double orSum = 0;
			double andSum = 0;
			notHyperIncreasingSequence = new double[notKeywords.length];
			if (orHyperIncreasingSequence != null) {
				orSum = Arrays.stream(orHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
			}
			if (andHyperIncreasingSequence != null) {
				andSum = Arrays.stream(andHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
			}

			for (int i = 0; i < notKeywords.length; i++) {
				notHyperIncreasingSequence[i] = -(orSum + andSum + upper * Initialization.RANDOM.nextInt(upper));
			}
		}

		System.out.println("not hyper sequence:" + Arrays.toString(notHyperIncreasingSequence));

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

		Matrix Q = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);

		/**
		 * the corresponding values in Q are set as
		 * (a1, a2,.. al1; b1, b2...bl2; -c1,-c2...-cl3).
		 * Other values in Q are set as 0.
		 */
		int count = 0;
		if (orHyperIncreasingSequence != null) {
			for (String keyword : orKeywords) {
				int index = initialization.dict.indexOf(keyword);
				if (index != -1) {
					Q.set(index, 0, orHyperIncreasingSequence[count++]);
				}
			}
		}

		count = 0;
		if (andHyperIncreasingSequence != null) {
			for (String keyword : andKeywords) {
				int index = initialization.dict.indexOf(keyword);
				if (index != -1) {
					Q.set(index, 0, andHyperIncreasingSequence[count++]);
				}
			}
		}

		count = 0;
		if (notHyperIncreasingSequence != null) {
			for (String keyword : notKeywords) {
				int index = initialization.dict.indexOf(keyword);
				if (index != -1) {
					Q.set(index, 0, notHyperIncreasingSequence[count++]);
				}
			}
		}

		Q.set(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER - 1, 0, -s);
		// Q 更新为 rQ
		Q = Q.times(r);

		/*
		// 之前一直都忘记了这一部分。
		for (int i = 0; i < Initialization.DUMMY_KEYWORD_NUMBER; i++) {
			String str = Initialization.extendDummyDict.get(i);
			int index = Initialization.dict.indexOf(str);
			if (index != -1) {
				// 设置一部分bit为1.
				if (Initialization.RANDOM.nextBoolean()) {
					Q.set(index, 0, 1);
				}
			}
		}*/

		/*System.out.println("Q Qa Qb transponse.");
		MatrixUitls.print(Q.transpose());*/

		Matrix qa = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);
		Matrix qb = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);

		for (int i = 0; i < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; i++) {
			// S[i] == 1;
			if (mySecretKey.S.get(i)) {
				double v1 = Initialization.RANDOM.nextDouble();
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
