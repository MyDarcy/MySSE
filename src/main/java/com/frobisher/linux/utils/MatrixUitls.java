package com.frobisher.linux.utils;

import Jama.Matrix;

/*
 * author: darcy
 * date: 2017/11/8 22:39
 * description: 
*/
public class MatrixUitls {

	public static void print(Matrix matrix) {
		double[][] array = matrix.getArray();
		for (int i = 0; i < array.length; i++) {
			System.out.print("[");
			for (int j = 0; j < array[i].length; j++) {
				if (j < array[i].length - 1) {
					System.out.printf("%4f,", array[i][j]);
				} else {
					System.out.printf("%4f", array[i][j]);
				}
			}
			System.out.println("]");
		}
	}

	public static String dimension(Matrix matrix) {
		return matrix.getRowDimension() + "\t" + matrix.getColumnDimension();
	}
}
