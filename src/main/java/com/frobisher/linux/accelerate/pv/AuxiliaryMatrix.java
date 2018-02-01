package com.frobisher.linux.accelerate.pv;

/*
 * author: darcy
 * date: 2017/12/20 15:21
 * description:
 * 缓存两个可逆矩阵的转置和逆矩阵.
 * 方便复用.
*/
public class AuxiliaryMatrix {
	// transpose
	public static double[] M1Transpose;
	public static double[] M2Transpose;

	// inverse
	public static double[] M1Inverse;
	public static double[] M2Inverse;
}
