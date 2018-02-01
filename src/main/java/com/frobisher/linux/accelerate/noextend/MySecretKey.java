package com.frobisher.linux.accelerate.noextend;

import javax.crypto.SecretKey;
import java.util.BitSet;

/*
 * author: darcy
 * date: 2017/11/7 16:56
 * description: 
*/
public class MySecretKey {

	public BitSet S;
	public double[] M1;
	public double[] M2;
	public SecretKey secretKey;

	@Override
	public String toString() {
		return "MySecretKey{" +
				"S.length()=" + S.length() +
				", M1.length=" + M1.length +
				", M2.length=" + M2.length +
				", secretKey=" + secretKey +
				'}';
	}
}
