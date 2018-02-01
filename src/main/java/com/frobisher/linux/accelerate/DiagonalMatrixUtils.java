package com.frobisher.linux.accelerate;

import java.util.Random;

/*
 * author: darcy
 * date: 2017/12/27 16:08
 * description:
 *
 * 对角矩阵utils.
 *
*/
public class DiagonalMatrixUtils {


	/**
	 * 第一个代表是 (1*M)的矩阵
	 * 第二个代表是 (M*1)的矩阵
	 * @param ccv1
	 * @param ccv2
	 * @return
	 */
	public static double score(double[] ccv1, double[] ccv2) {
		double sum = 0;
		for (int i = 0; i < ccv1.length; i++) {
			sum += ccv1[i] * ccv2[i];
		}
		return sum;
	}


	/**
	 * 注意, 因为后面计算评分的时候是 (1 * M) * (M * 1) {两个部分分别是加密后的文档向量和加密后的陷门的一部分}得到分值。
	 * 但是这里 (M * M) * (M * 1)得到的是 (M * 1)的矩阵而不是转置后的(1*M)的, 而没有借由Matrix,
	 * 所以我这里就直接使用double[] 来作为转置后的(1 * M)的矩阵。
	 * @param diagonalMatrix
	 * @param columnVector
	 * @return
	 */
	public static double[] times(double[] diagonalMatrix, double[] columnVector) {
		double[] part = new double[columnVector.length];
		// 因为行向量和列向量都只有一个元素.
		for (int i = 0; i < columnVector.length; i++) {
			part[i] = diagonalMatrix[i] * columnVector[i];
		}
		return part;
	}

	/**
	 * 生成 size 维度的对角矩阵, 但是因为实际的元素只有size个，所以只用
	 * 一个double[] 数组存储即可。
	 * @param size
	 * @return
	 */
	public static double[] random(int size) {
		Random random = new Random(System.currentTimeMillis());
		double[] matrix = new double[size];
		for (int i = 0; i < size; i++) {
			double number = random.nextDouble();
			while (Double.compare(number, 0.0) == 0) {
				number = random.nextDouble();
			}
			matrix[i] = number;
		}
		return matrix;
	}

	/**
	 * 对角矩阵的转置矩阵是原矩阵。
	 * @param matrix
	 * @return
	 */
	public static double[] transpose(double[] matrix) {
		double[] transposeMatrix = new double[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			transposeMatrix[i] = matrix[i];
		}
		return transposeMatrix;
	}

	/**
	 * 对角矩阵的逆矩阵是原矩阵的相应位置处元素的倒数
	 * @param matrix
	 * @return
	 */
	public static double[] inverse(double[] matrix) {
		double[] inverseMatrix = new double[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			inverseMatrix[i] = 1.0 / matrix[i];
		}
		return inverseMatrix;
	}

	public static void main(String[] args) {
		int size = 1000000;
		long start = System.currentTimeMillis();
		double[] matrix = random(size);
		System.out.println("random time:" + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		double[] transpose = transpose(matrix);
		System.out.println("transpose time:" + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		double[] inverse = inverse(matrix);
		System.out.println("inverse time:" + (System.currentTimeMillis() - start));
		/*System.out.println(Arrays.toString(matrix));
		System.out.println(Arrays.toString(transpose));
		System.out.println(Arrays.toString(inverse));*/
	}


	public static double[] times(double[] q, int r) {
		double[] result = new double[q.length];
		for (int i = 0; i < q.length; i++) {
			result[i] = q[i] * r;
		}
		return result;
	}
}
