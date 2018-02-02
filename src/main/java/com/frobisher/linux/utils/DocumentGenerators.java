package com.frobisher.linux.utils;

import Jama.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * author: darcy
 * date: 2018/2/2 16:04
 * description: 
*/

class MyRandom {
	Random random = new Random();
	int low = 0;
	int high = 2;

	public MyRandom(int low, int high) {
		this.low = low;
		this.high = high;
	}

	public boolean next() {
		return random.nextInt(high) == low;
	}

	public double get() {
		return random.nextDouble();
	}
}

public class DocumentGenerators {
	static List<MyRandom> randomList = new ArrayList<>();
	static int HIGH = 32;

	public static void initRandomList() {
		int high = HIGH;
		for (int i = 2; i <= high; i++) {
			randomList.add(new MyRandom(0, i));
		}
	}

	public static Matrix generateMatrix(int size) {
		double[][] array = new double[size][1];
		Random selector = new Random(randomList.size());
		MyRandom myRandom = randomList.get(selector.nextInt(randomList.size()));
		for (int i = 0; i < size; i++) {
			array[i][0] = myRandom.get();
		}
		return new Matrix(array);
	}

	public static double[] generateDoubleArray(int size) {
		double[] P = new double[size];
		Random selector = new Random(randomList.size());
		MyRandom myRandom = randomList.get(selector.nextInt(randomList.size()));
		for (int i = 0; i < size; i++) {
			P[i] = myRandom.get();
		}
		return P;
	}



	public static void main(String[] args) {
		initRandomList();
		for (int i = 0; i < randomList.size(); i++) {
			int total = 100000;
			int rightCount = 0;
			for (int j = 0; j < total; j++) {
				if (randomList.get(i).next()) {
					rightCount++;
				}
			}
			System.out.printf("%-4d%-6f\n", randomList.get(i).high, rightCount * 1.0 / total);
		}
	}

}
