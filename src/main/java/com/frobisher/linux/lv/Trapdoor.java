package com.frobisher.linux.lv;

import Jama.Matrix;

/*
 * author: darcy
 * date: 2017/11/18 20:15
 * description:
 *
 * 陷门由两部分组成.
*/
public class Trapdoor {

	public Matrix trapdoorPart1;
	public Matrix trapdoorPart2;

	public Trapdoor(Matrix trapdoorPart1, Matrix trapdoorPart2) {
		this.trapdoorPart1 = trapdoorPart1;
		this.trapdoorPart2 = trapdoorPart2;
	}

	@Override
	public String toString() {
		double[][] a1 = trapdoorPart1.getArray();
		double[][] a2 = trapdoorPart2.getArray();
		for (int i = 0; i < a1.length; i++) {
			for (int j = 0; j < a1[i].length; j++) {
				System.out.printf("%-6f\t", a1[i][j]);
			}
			System.out.println();
		}

		for (int i = 0; i < a2.length; i++) {
			for (int j = 0; j < a2[i].length; j++) {
				System.out.printf("%-6f\t", a2[i][j]);
			}
			System.out.println();
		}
		return null;
	}
}
