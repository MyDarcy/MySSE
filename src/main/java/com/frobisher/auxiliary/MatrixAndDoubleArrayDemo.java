package com.frobisher.auxiliary;

import Jama.Matrix;

/*
 * author: darcy
 * date: 2018/2/1 17:00
 * description: 
*/
public class MatrixAndDoubleArrayDemo {
	/**
	 * 耗时基本差别不大.
	 * matrix cost:612ms
	 * array cost:605ms
	 *
	 * size * round > 10^9的话，那么也会有秒级别的差异.
	 */
	private static void testGetAndSet() {
		int size = 30000;
		int round = 100000;
		Matrix matrix = new Matrix(size, 1);
		double[][] array = new double[size][1];

		long start = System.currentTimeMillis();
		for (int i = 0; i < round; i++) {
			for (int j = 0; j < size; j++) {
				matrix.set(j, 0, matrix.get(j, 0) + i);
			}
		}
		System.out.println("matrix cost:" + (System.currentTimeMillis() - start) + "ms");

		start = System.currentTimeMillis();
		for (int i = 0; i < round; i++) {
			for (int j = 0; j < size; j++) {
				array[j][0] = array[j][0] + i;
			}
		}
		System.out.println("array cost:" + (System.currentTimeMillis() - start) + "ms");


	}
	public static void main(String[] args) {
		testGetAndSet();
	}

}
