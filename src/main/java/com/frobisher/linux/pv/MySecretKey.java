package com.frobisher.linux.pv;

import Jama.Matrix;

import javax.crypto.SecretKey;
import java.util.BitSet;

/*
 * author: darcy
 * date: 2017/11/7 16:56
 * description: 
*/
public class MySecretKey {

	public BitSet S;
	public Matrix M1;
	public Matrix M2;
	public SecretKey secretKey;

	public String toString1() {
		/*long start = System.currentTimeMillis();
		int rank1 = M1.rank();
		System.out.println("matrix rank time consume:" + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		int rank2 = M2.rank();*/
//		int rank = (Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER + 1);
//		long end = System.currentTimeMillis();
//		return "MySecretKey{" +
//				"S.length=" + S.length() +
//				", M1.rank=" + rank +
//				", M2.rank=" + rank +
//				", secretKey=" + secretKey +
//				/*", time=" + (end - start) +*/
//				'}';
		return null;
	}

}
