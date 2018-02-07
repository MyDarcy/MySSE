package com.frobisher.linux.lv;

import Jama.Matrix;
import com.frobisher.linux.pv.AuxiliaryMatrix;
import com.frobisher.linux.pv.Initialization;
import com.frobisher.linux.pv.MySecretKey;
import com.frobisher.linux.pv.Trapdoor;
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
	public Trapdoor generateTrapdoor(List<List<Integer>> keywordsIndex) {
		System.out.println("TrapdoorGenerating trapdoorGenerating start.");
		long start = System.currentTimeMillis();
		Random random = new Random(31);
		Matrix Q = new Matrix(initialization.simulationDictSize, 1);

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
		System.out.println("or hyper sequence:" + Arrays.toString(orHyperIncreasingSequence));

		if (andKeywordsIndex != null) {
			andHyperIncreasingSequence = new double[andKeywordsIndex.size()];
			if (orHyperIncreasingSequence != null) {
				double sum = Arrays.stream(orHyperIncreasingSequence).reduce(Double::sum).getAsDouble();
				for (int i = 0; i < andKeywordsIndex.size(); i++) {
					andHyperIncreasingSequence[i] = sum + upper * Initialization.RANDOM.nextInt(upper);
				}
			}
		}
		System.out.println("and hyper sequence:" + Arrays.toString(andHyperIncreasingSequence));

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

		/**
		 * the corresponding values in Q are set as
		 * (a1, a2,.. al1; b1, b2...bl2; -c1,-c2...-cl3).
		 * Other values in Q are set as 0.
		 */
		int count = 0;
		if (orHyperIncreasingSequence != null) {
			for (int index : orKeywordsIndex) {
				Q.set(index, 0, orHyperIncreasingSequence[count++]);
			}
		}

		count = 0;
		if (andHyperIncreasingSequence != null) {
			for (int index : andKeywordsIndex) {
				Q.set(index, 0, andHyperIncreasingSequence[count++]);
			}
		}

		count = 0;
		if (notHyperIncreasingSequence != null) {
			for (int index : notKeywordsIndex) {
				Q.set(index, 0, notHyperIncreasingSequence[count++]);
			}
		}

		// 最后一位要要设置为 -s 即and连接的关键词的权重。
		Q.set(initialization.simulationDictSize - 1, 0, -s);

		// Q 更新为 rQ
		Q = Q.times(r);
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

	public static void main(String[] args) throws IOException {
		/**
		 * 论文中是陷门生成是按照关键词的数目生成的.
		 */
	}
}
