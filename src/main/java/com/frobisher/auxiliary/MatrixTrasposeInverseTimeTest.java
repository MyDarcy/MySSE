package com.frobisher.auxiliary;

import Jama.Matrix;
import com.frobisher.linux.accelerate.DiagonalMatrixUtils;
import org.ujmp.core.doublematrix.calculation.entrywise.creators.Rand;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * author: darcy
 * date: 2018/2/2 10:29
 * description: 
*/
public class MatrixTrasposeInverseTimeTest {
	static List<Long> transpose = new ArrayList<>();
	static List<Long> inverse = new ArrayList<>();
	static Map<Integer, Long> transposeMap = new LinkedHashMap<>();
	static Map<Integer, Long> inverseMap = new LinkedHashMap<>();

//	static List<Long> diagonalTranspose = new ArrayList<>();
//	static List<Long> diagonalInverse = new ArrayList<>();
//	static Map<Integer, Long> diagonalTransposeMap = new LinkedHashMap<>();
//	static Map<Integer, Long> diagonalInverseMap = new LinkedHashMap<>();

	static List<Double> diagonalTranspose = new ArrayList<>();
	static List<Double> diagonalInverse = new ArrayList<>();
	static Map<Integer, Double> diagonalTransposeMap = new LinkedHashMap<>();
	static Map<Integer, Double> diagonalInverseMap = new LinkedHashMap<>();

	static List<Integer> dimensions = new ArrayList<Integer>();

	static Random random = new Random(System.currentTimeMillis());

	static {
		dimensions = IntStream.rangeClosed(1000, 10000).filter((number) -> number % 1000 == 0).boxed().collect(Collectors.toList());
		dimensions.addAll(Arrays.asList(12000, 15000, 16000, 18000, 20000, 25000, 30000));
//		dimensions = IntStream.rangeClosed(100, 2000).filter((number) -> (number % 100) == 0).boxed().collect(Collectors.toList());
	}

	public static void main(String[] args) {

		testDiagonalTransposeInverse();
		testDiagonalTransposeInversePrint();
		testTransposeInverse();
		testTransposeInversePrint();
	}

	private static void testDiagonalTransposeInverse() {
		int low = 100;
		int high = 2000;
		Random random = new Random();
		/*for (int i = low; i <= high; i += low)*/
		 for(Integer i : dimensions){
			System.out.println(i);
			double[] M1 = new double[i];
			fillArray(M1);
			double[] M2 = new double[i];
			 fillArray(M2);

			 long start = System.currentTimeMillis();
			 long nstart = start;

			 double[] M1Transpose = DiagonalMatrixUtils.transpose(M1);
			 double[] M2Transpose = DiagonalMatrixUtils.transpose(M2);
			 System.out.println("two transpose:" + (System.currentTimeMillis() - start) + "ms");

			 start = System.currentTimeMillis();
			 double[] M1Inverse = DiagonalMatrixUtils.inverse(M1);
			 double[] M2Inverse = DiagonalMatrixUtils.inverse(M2);
			 System.out.println("two inverse:" + (System.currentTimeMillis() - start) + "ms");
//			fillArray(matrix2);
////			long start = System.currentTimeMillis();
//			long start = System.nanoTime();
//			double[] x = DiagonalMatrixUtils.transpose(matrix1);
//			double[] y = DiagonalMatrixUtils.transpose(matrix2);
////			long cost = System.currentTimeMillis() - start;
//			long cost = System.nanoTime() - start;
//			diagonalTranspose.add(cost / 1000000.0);
//			diagonalTransposeMap.put(i, cost / 1000000.0);
////			start = System.currrentTimeMillis();
//			start = System.nanoTime();
//			double[] xi = DiagonalMatrixUtils.inverse(matrix1);
//			double[] yi = DiagonalMatrixUtils.inverse(matrix2);
////			long cost2 = System.currentTimeMillis() - start;
//			long cost2 = System.nanoTime() - start;
//			diagonalInverse.add(cost2 / 1000000.0);
//			diagonalInverseMap.put(i, cost2 / 1000000.0);
		}
	}

	public static void testDiagonalTransposeInversePrint() {
		System.out.println("diagonal transpose");
		diagonalTranspose.stream().forEach(System.out::println);
		System.out.println("diagonal inverse");
		diagonalInverse.stream().forEach(System.out::println);
		System.out.println("diagonal transpose map");
		diagonalTransposeMap.entrySet().stream().forEach((item) -> System.out.println(item.getKey() + ":" + item.getValue()));
		System.out.println("diagonal inverse map");
		diagonalInverseMap.entrySet().stream().forEach((item) -> System.out.println(item.getKey() + ":" + item.getValue()));
	}

	private static void fillArray(double[] matrix1) {
		for (int i = 0; i < matrix1.length; i++) {
			matrix1[i] = random.nextDouble() * 3;
		}
	}

	private static void testTransposeInverse() {
		int low = 100;
		int high = 2000;
		/*for (int i = low; i <= high; i += low)*/
		for (Integer i : dimensions){
			System.out.println("======= " + i + "=======");
			Matrix matrix1 = Matrix.random(i, i);
			Matrix matrix2 = Matrix.random(i, i);
			long start = System.currentTimeMillis();
			matrix1.transpose();
			matrix2.transpose();
			long cost = System.currentTimeMillis() - start;
			transpose.add(cost);
			transposeMap.put(i, cost);
			start = System.currentTimeMillis();
			matrix1.inverse();
			matrix2.inverse();
			long cost2 = System.currentTimeMillis() - start;
			inverse.add(cost2);
			inverseMap.put(i, cost2);
		}
	}

	public static void testTransposeInversePrint() {
		System.out.println("transpose");
		transpose.stream().forEach(System.out::println);
		System.out.println("inverse");
		inverse.stream().forEach(System.out::println);
		System.out.println("transpose map");
		transposeMap.entrySet().stream().forEach((item) -> System.out.println(item.getKey() + ":" + item.getValue()));
		System.out.println("inverse map");
		inverseMap.entrySet().stream().forEach((item) -> System.out.println(item.getKey() + ":" + item.getValue()));
	}
}
