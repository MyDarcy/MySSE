package com.frobisher.linux.utils;

import Jama.Matrix;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/*
 * author: darcy
 * date: 2018/2/2 16:04
 * description: 
*/

class MyRandom {
	Random random = new Random();
		ThreadLocalRandom localRandom = ThreadLocalRandom.current();
//	Random localRandom = random;
	int low = 0;
	int high = 2;

	public MyRandom() {
	}

	public MyRandom(int low, int high) {
		this.low = low;
		this.high = high;
	}

	public MyRandom(int high) {
		this.high = high;
	}

	public boolean next() {
//		return random.nextInt(high) == low;
		return localRandom.nextInt(high) == low;
	}

	/**
	 * 要能反映概率，
	 * @return
	 */
	public double get() {
		return get(1);
	}

	/**
	 *
	 * @param times
	 * @return
	 */
	public double get(int times) {
		if (next()) {
//			return random.nextDouble() * times;
			return localRandom.nextDouble(times);
		} else {
			return 0;
		}
	}

	public int get01() {
		if (next()) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		return "MyRandom{" +
				"low=" + low +
				", high=" + high +
				'}';
	}
}

public class DocumentGenerators {
	List<MyRandom> randomList = new ArrayList<>();
	Set<Integer> dummykeywordIndexSet = new HashSet<>();
	int LOW = 4;
	int HIGH = 64;

	public DocumentGenerators() {
		int low = LOW;
		int high = HIGH;
		for (int i = LOW; i <= high; i++) {
			randomList.add(new MyRandom(0, i));
		}
	}

	public DocumentGenerators(int high) {
		int low = LOW;
		for (int i = low; i <= high; i++) {
			randomList.add(new MyRandom(0, i));
		}
	}

	public DocumentGenerators(int low, int high) {
		for (int i = low; i <= high; i++) {
			randomList.add(new MyRandom(0, i));
		}
	}

	public DocumentGenerators(Set<Integer> dummykeywordIndexSet, int low, int high) {
		this.dummykeywordIndexSet = dummykeywordIndexSet;
		for (int i = low; i <= high; i++) {
			randomList.add(new MyRandom(0, i));
		}
	}

	/**
	 * 生成的文档数目是documentNumber, 总的字典的维度是matrixSize;
	 * @param documentNumber
	 * @param matrixSize
	 * @return
	 */
	public List<Matrix> generateDocumentsMatrix(int documentNumber, int matrixSize) {
		List<Matrix> documents = new ArrayList<>(documentNumber);
		for (int i = 0; i < documentNumber; i++) {
			documents.add(generateMatrix(matrixSize));
		}
		return documents;
	}

	/**
	 * 生成数组形式表示的文档向量，主要用于对角矩阵的版本
	 * @param documentNumber
	 * @param arraySize
	 * @return
	 */
	public List<double[]> generateDocumentsArray(int documentNumber, int arraySize) {
		List<double[]> documents = new ArrayList<>(documentNumber);
		for (int i = 0; i < documentNumber; i++) {
			documents.add(generateDoubleArray(arraySize));
		}
		return documents;
	}

	public List<Matrix> generateDocumentsMatrix01(int documentNumber, int matrixSize) {
		List<Matrix> documents = new ArrayList<>(documentNumber);
		for (int i = 0; i < documentNumber; i++) {
			documents.add(generateMatrix01(matrixSize));
		}
		return documents;
	}

	public List<double[]> generateDocumentsArray01(int documentNumber, int matrixSize) {
		List<double[]> documents = new ArrayList<>(documentNumber);
		for (int i = 0; i < documentNumber; i++) {
			documents.add(generateDoubleArray01(matrixSize));
		}
		return documents;
	}

	private Matrix generateMatrix01(int size) {
		double[][] array = new double[size][1];
		Random selector = new Random();
		MyRandom myRandom = randomList.get(selector.nextInt(randomList.size()));
		// 字典维度是size, 那么假定的总的虚拟关键词的个数是 size - 1, 因为最后一个位置用
		// 于构造查询需要的。
		for (int i = 0; i < size - 1; i++) {
			array[i][0] = myRandom.get01();
		}
		return new Matrix(array);
	}

	public double[] generateDoubleArray01(int size) {
		double[] P = new double[size];
		Random selector = new Random();
		MyRandom myRandom = randomList.get(selector.nextInt(randomList.size()));
		for (int i = 0; i < size - 1; i++) {
			// 同理
			P[i] = myRandom.get01();
		}
		return P;
	}

	/**
	 * 获取一个MyRandom对象，其概率能反映文档的长度
	 * @param size
	 * @return
	 */
	public Matrix generateMatrix(int size) {
		double[][] array = new double[size][1];
		Random selector = new Random(31);
		MyRandom myRandom = randomList.get(selector.nextInt(randomList.size()));
		for (int i = 0; i < size; i++) {
			if (!dummykeywordIndexSet.contains(i)) {
				array[i][0] = myRandom.get(2);
			}
		}
		return new Matrix(array);
	}

	public double[] generateDoubleArray(int size) {
		double[] P = new double[size];
		Random selector = new Random(31);
		MyRandom myRandom = randomList.get(selector.nextInt(randomList.size()));
		for (int i = 0; i < size; i++) {
			// 不包含.
			if (!dummykeywordIndexSet.contains(i)) {
				P[i] = myRandom.get(2);
			}
		}
		return P;
	}


	public static void main(String[] args) {
		DocumentGenerators generators = new DocumentGenerators();
		for (int i = 0; i < generators.randomList.size(); i++) {
			int total = 100000;
			int rightCount = 0;
			for (int j = 0; j < total; j++) {
				if (generators.randomList.get(i).next()) {
					rightCount++;
				}
			}
			System.out.printf("%-4d%-6f\n", generators.randomList.get(i).high, rightCount * 1.0 / total);
		}

		generators = new DocumentGenerators(10, 400);
//		List<double[]> documents = generators.generateDocumentsArray(
//				100, 20000);
//		for (int i = 0; i < documents.size(); i++) {
//			int count = 0;
//			double[] array = documents.get(i);
//			for (int j = 0; j < array.length; j++) {
//				if (Double.compare(array[j], 0) != 0) {
//					count++;
//				}
//			}
//			System.out.println(count + ":" + Arrays.toString(array));
//		}

		System.out.println();

		generators = new DocumentGenerators(5, 40);
		List<Matrix> matrices = generators.generateDocumentsMatrix01(30, 4000);
		Double MIN = Double.MAX_VALUE;
		for (int i = 0; i < matrices.size(); i++) {
			int count = 0;
			Matrix transpose = matrices.get(i).transpose();
			double[][] array = transpose.getArray();
			for (int j = 0; j < array.length; j++) {
				for (int k = 0; k < array[j].length; k++) {
					if (Double.compare(array[j][k], 0) != 0) {
						count++;
						if (Double.compare(array[j][k], MIN) < 0) {
							MIN = array[j][k];
						}
					}

				}

			}
			System.out.print(count + ":");
			MatrixUitls.print(transpose);
		}
		System.out.println("\n" + "MIN:" + MIN);
	}
}
