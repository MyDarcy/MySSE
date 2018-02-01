package com.frobisher.linux.extend;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/*
 * author: darcy
 * date: 2017/12/19 15:28
 * description:
 *
 * NCP-DFS 非候选深度优先搜索算法.
*/
public class SearchAlgorithm {

	double thresholdScore = Double.NEGATIVE_INFINITY;
	private Comparator<HACTreeNode> maxComparator;
	private Comparator<HACTreeNode> minComparator;
	private PriorityQueue<HACTreeNode> allDocumentSocreQueue;
	private int leafNodeCount = 0;
	private int containsCount = 0;

	/**
	 * 实现方式1: 采用堆的性性质.
	 *
	 * @param root
	 * @param trapdoor
	 * @param requestNumber
	 * @return
	 */
	public PriorityQueue<HACTreeNode> search(HACTreeNode root, Trapdoor trapdoor, int requestNumber) {
		System.out.println("SearchAlgorithm search start.");
		long start = System.currentTimeMillis();
		minComparator = new Comparator<HACTreeNode>() {
			@Override
			public int compare(HACTreeNode o1, HACTreeNode o2) {
				double score1 = scoreForPruning(o1, trapdoor);
				double score2 = scoreForPruning(o2, trapdoor);
				if (Double.compare(score1, score2) > 0) {
					return 1;
				} else if (Double.compare(score1, score2) == 0) {
					return 0;
				} else {
					return -1;
				}
			}
		};
		// 既然这个就是跟查询之间相关性评分最高的文档. 那么只需要利用此优先级队列或者相反的
		// 优先级队列就可以求出最不相关的文档.
		maxComparator = new Comparator<HACTreeNode>() {
			@Override
			public int compare(HACTreeNode node1, HACTreeNode node2) {
				double score1 = scoreForPruning(node1, trapdoor);
				double score2 = scoreForPruning(node2, trapdoor);
				if (Double.compare(score1, score2) > 0) {
					return -1;
				} else if (Double.compare(score1, score2) == 0) {
					return 0;
				} else {
					return 1;
				}
			}
		};

		allDocumentSocreQueue = new PriorityQueue<>(maxComparator);
		PriorityQueue<HACTreeNode> minHeap = new PriorityQueue<>(minComparator);
		dfs(root, trapdoor, requestNumber, minHeap);
		PriorityQueue<HACTreeNode> maxHeap = new PriorityQueue<>(maxComparator);
		maxHeap.addAll(minHeap);
		// 服务器端排序，然后返回top-K个最相关的文档.

		System.out.println("total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("SearchAlgorithm search end.");

		System.out.println("leafNodeCount:" + leafNodeCount);
		System.out.println("containsCount:" + containsCount);

		System.out.println("all document-size:" + allDocumentSocreQueue.size());
		System.out.println("all document-score.");
		while (!allDocumentSocreQueue.isEmpty()) {
			HACTreeNode node = allDocumentSocreQueue.poll();
			System.out.printf("%-60s%.8f\n", node.fileDescriptor, scoreForPruning(node, trapdoor));
		}

		System.out.println("\nresult document-score.");
		PriorityQueue<HACTreeNode> result = new PriorityQueue<>(maxComparator);
		while (!maxHeap.isEmpty()) {
			HACTreeNode node = maxHeap.poll();
			result.add(node);
			System.out.printf("%-60s%.8f\n", node.fileDescriptor, scoreForPruning(node, trapdoor));
		}

		return result;
	}

	private void dfs(HACTreeNode root, Trapdoor trapdoor, int requestNumber, PriorityQueue<HACTreeNode> minHeap) {
		// 是叶子结点.
		if (root.left == null && root.right == null) {
			leafNodeCount++;
			if (allDocumentSocreQueue.contains(root)) {
				containsCount++;
			}
			allDocumentSocreQueue.add(root);

			// 并且候选结果集合中没有top-K个元素.
			int size = minHeap.size();
			if (size < requestNumber - 1) {
				System.out.println("< (N-1) add:" + root.fileDescriptor);
				System.out.println();

				minHeap.add(root);

				// 已经找到了 N-1个文档，然后将当前文档加入, 但是要更新现在的阈值评分.
			} else if (size == (requestNumber - 1)) {
				minHeap.add(root);
				System.out.println("= (N-1) add:" + root.fileDescriptor);
				thresholdScore = scoreForPruning(minHeap.peek(), trapdoor);
				System.out.println("thresholdSocre:" + thresholdScore);
				System.out.println();

				// 仍然时叶子节点，但是候选结果集合中已经有了N个文档.
			} else {
				// 那么此时如果当前结点跟查询之间的相关性评分大于阈值，那么是需要更新
				// 候选结果集合的。
				double score1 = scoreForPruning(root, trapdoor);
				if (score1 > thresholdScore) {
					HACTreeNode minScoreNode = minHeap.poll();
					double score = scoreForPruning(minScoreNode, trapdoor);
					System.out.println("== (N) remove:" + minScoreNode.fileDescriptor + " socre:" + score);
					System.out.println();

					minHeap.add(root);
					thresholdScore = scoreForPruning(minHeap.peek(), trapdoor);
				}
			}
		} else {
			double score = scoreForPruning(root, trapdoor);
			/*MatrixUitls.print(root.pruningVectorPart1);
			MatrixUitls.print(root.pruningVectorPart2);
			MatrixUitls.print(trapdoor.trapdoorPart1.transpose());
			MatrixUitls.print(trapdoor.trapdoorPart2.transpose());*/
			System.out.printf("%-10s\t%.8f\t%-20s\t%.8f\n", "score", score, "thresholdScore", thresholdScore);
			if (score > thresholdScore) {
				if (root.left != null) {
					System.out.println("left");
					dfs(root.left, trapdoor, requestNumber, minHeap);
				}
				if (root.right != null) {
					System.out.println("right");
					dfs(root.right, trapdoor, requestNumber, minHeap);
				}
			} else {
				System.out.println("score:" + score + " no bigger than thresholdScore:" + thresholdScore);
				System.out.println();
			}

		}
	}

	/**
	 * 计算跟查询向量之间最不相关的文档.
	 *
	 * @param resultList
	 * @param trapdoor
	 * @return
	 */
	private HACTreeNode getMinScoreNode(List<HACTreeNode> resultList, Trapdoor trapdoor) {
		HACTreeNode result = null;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < resultList.size(); i++) {
			double currentScore = scoreForPruning(resultList.get(i), trapdoor);
			if (currentScore < min) {
				min = currentScore;
				result = resultList.get(i);
			}
		}
		return result;
	}

	/**
	 * 根节点和Trapdoor之间的相关性评分.
	 *
	 * @param root
	 * @param trapdoor
	 * @return
	 */
	private double scoreForPruning(HACTreeNode root, Trapdoor trapdoor) {
		/*return root.pruningVector.times(queryVector).get(0, 0);*/
		/*double temp = root.pruningVectorPart1.transpose().times(trapdoor.trapdoorPart1).get(0, 0)
				+ root.pruningVectorPart2.transpose().times(trapdoor.trapdoorPart2).get(0, 0);*/
    // p1, p2, q1, q2都是m*1的矩阵。
		double[][] p1 = root.pruningVectorPart1.getArray();
		double[][] p2 = root.pruningVectorPart2.getArray();
		double[][] q1 = trapdoor.trapdoorPart1.getArray();
		double[][] q2 = trapdoor.trapdoorPart2.getArray();
		double sum = 0;
		for (int i = 0; i < p1.length; i++) {
			sum += p1[i][0] * q1[i][0] + p2[i][0] * q2[i][0];
		}

		//System.out.println("temp:" + temp + "  sum:" + sum + "\tequals:" + (Double.compare(temp, sum) == 0));

		return sum;
	}


	/**
	 * 搜索的时候用于更新最低阈值分数。
	 *
	 * @param resultList
	 * @param trapdoor
	 * @return
	 */
	private double updateThresholdScore(List<HACTreeNode> resultList, Trapdoor trapdoor) {

		double min = Double.MAX_VALUE;
		for (int i = 0; i < resultList.size(); i++) {
			/*double score = resultList.get(i).pruningVector.times(queryVector).get(0, 0);*/
			double score = resultList.get(i).pruningVectorPart1.transpose().times(trapdoor.trapdoorPart1).get(0, 0)
					+ resultList.get(i).pruningVectorPart2.transpose().times(trapdoor.trapdoorPart2).get(0, 0);
			// 更新最小相关性评分.
			if (score < min) {
				min = score;
			}
		}
		return min;
	}
}
