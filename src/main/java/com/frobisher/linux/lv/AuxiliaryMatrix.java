package com.frobisher.linux.lv;

import Jama.Matrix;

/*
 * author: darcy
 * date: 2017/12/20 15:21
 * description:
 * 缓存两个可逆矩阵的转置和逆矩阵.
 * 方便复用.
*/
public class AuxiliaryMatrix {
	// transpose
	public static Matrix M1Transpose;
	public static Matrix M2Transpose;

	// inverse
	public static Matrix M1Inverse;
	public static Matrix M2Inverse;
}
