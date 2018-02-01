package com.frobisher.linux.plain;

import Jama.Matrix;

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
	private int computeCount = 0;
	private int pruneCount = 0;

	private PriorityQueue<Double> leastCorrespondingScore =
			new PriorityQueue<>(40, Comparator.reverseOrder());

	/**
	 * 实现方式1: 采用堆的性性质.
	 *
	 * @param root
	 * @param queryVector
	 * @param requestNumber
	 * @return
	 */
	public PriorityQueue<HACTreeNode> search(HACTreeNode root, Matrix queryVector, int requestNumber) {
		System.out.println("SearchAlgorithm search start.");
		long start = System.currentTimeMillis();
		minComparator = new Comparator<HACTreeNode>() {

			@Override
			public int compare(HACTreeNode o1, HACTreeNode o2) {
				double s1 = scoreForPruning(o1, queryVector);
				double s2 = scoreForPruning(o2, queryVector);
				if (s1 > s2) {
					return 1;
				} else if (s1 < s2) {
					return -1;
				} else {
					return 0;
				}
			}

			/*@Override
			public int compare(HACTreeNode o1, HACTreeNode o2) {
				double score1 = scoreForPruning(o1, queryVector);
				double score2 = scoreForPruning(o2, queryVector);
				if (Double.compare(score1, score2) > 0) {
					return 1;
				} else if (Double.compare(score1, score2) == 0) {
					return 0;
				} else {
					return -1;
				}
			}*/
		};

		// 既然这个就是跟查询之间相关性评分最高的文档. 那么只需要利用此优先级队列或者相反的
		// 优先级队列就可以求出最不相关的文档.
		maxComparator = new Comparator<HACTreeNode>() {

			@Override
			public int compare(HACTreeNode o1, HACTreeNode o2) {
				double s1 = scoreForPruning(o1, queryVector);
				double s2 = scoreForPruning(o2, queryVector);
				if (s1 > s2) {
					return -1;
				} else if (s1 < s2) {
					return 1;
				} else {
					return 0;
				}
			}

			/*@Override
			public int compare(HACTreeNode node1, HACTreeNode node2) {
				double score1 = scoreForPruning(node1, queryVector);
				double score2 = scoreForPruning(node2, queryVector);
				if (Double.compare(score1, score2) > 0) {
					return -1;
				} else if (Double.compare(score1, score2) == 0) {
					return 0;
				} else {
					return 1;
				}
			}*/
		};

		allDocumentSocreQueue = new PriorityQueue<>(maxComparator);

		PriorityQueue<HACTreeNode> minHeap = new PriorityQueue<>(minComparator);
		dfs(root, queryVector, requestNumber, minHeap);
		PriorityQueue<HACTreeNode> maxHeap = new PriorityQueue(maxComparator);
		maxHeap.addAll(minHeap);
		// 服务器端排序，然后返回top-K个最相关的文档.

		System.out.println("total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("SearchAlgorithm search end.");

		System.out.println("requestNumber:" + requestNumber);
		System.out.println("leafNodeCount:" + leafNodeCount);
		System.out.println("containsCount:" + containsCount);
		System.out.println("computeCount:" + computeCount);
		System.out.println("pruneCount:" + pruneCount);

		System.out.println("least corresponding scores;");
		System.out.println(leastCorrespondingScore);

		System.out.println("all document-size:" + allDocumentSocreQueue.size());
		System.out.println("all document-score.");
		while (!allDocumentSocreQueue.isEmpty()) {
			HACTreeNode node = allDocumentSocreQueue.poll();
			System.out.printf("%-60s%.8f\n", node.fileDescriptor, scoreForPruning(node, queryVector));
		}

		System.out.println("\nresult document-score.");
		PriorityQueue<HACTreeNode> result = new PriorityQueue<>(maxComparator);
		while (!maxHeap.isEmpty()) {
			HACTreeNode node = maxHeap.poll();
			result.add(node);
			System.out.printf("%-60s%.8f\n", node.fileDescriptor, scoreForPruning(node, queryVector));
		}

		return result;
	}
	public static final double PRUNE_THRESHOLD_SCORE = 0.0004;

	private void dfs(HACTreeNode root, Matrix trapdoor, int requestNumber, PriorityQueue<HACTreeNode> minHeap) {
		// 是叶子结点.
		if (root.left == null && root.right == null) {
			leafNodeCount++;
			if (allDocumentSocreQueue.contains(root)) {
				containsCount++;
			}
			allDocumentSocreQueue.add(root);

			double scoreForPrune = scoreForPruning(root, trapdoor);
			computeCount++;


			// 并且候选结果集合中没有top-K个元素.
			int size = minHeap.size();
			if (scoreForPrune >= PRUNE_THRESHOLD_SCORE) {
				if (size < requestNumber - 1) {
					System.out.println("< (N-1) add:" + root.fileDescriptor);
					minHeap.add(root);
					// 已经找到了 N-1个文档，然后将当前文档加入, 但是要更新现在的阈值评分.
				} else if (size == (requestNumber - 1)) {
					minHeap.add(root);
					System.out.println("= (N-1) add:" + root.fileDescriptor);
					thresholdScore = scoreForPruning(minHeap.peek(), trapdoor);
					System.out.println("new thresholdScore:" + thresholdScore);
					computeCount++;
//					System.out.println("thresholdSocre:" + thresholdScore);
					// 仍然时叶子节点，但是候选结果集合中已经有了N个文档.
				} else {
					// 那么此时如果当前结点跟查询之间的相关性评分大于阈值，那么是需要更新
					// 候选结果集合的。
					if (/*scoreForPruning(root, trapdoor)*/ scoreForPrune > thresholdScore) {
						HACTreeNode minScoreNode = minHeap.poll();
//						double score = scoreForPruning(minScoreNode, trapdoor);
//						System.out.println("== (N) remove:" + minScoreNode.fileDescriptor + " socre:" + score);
						minHeap.add(root);
						thresholdScore = scoreForPruning(minHeap.peek(), trapdoor);
						System.out.println("new thresholdScore:" + thresholdScore);
						computeCount++;
					}
				}
			} else {
				System.out.println("leaf node not add for score < " + PRUNE_THRESHOLD_SCORE);
			}

			// 非叶子结点。
		} else {
			double score = scoreForPruning(root, trapdoor);
			computeCount++;
			/*MatrixUitls.print(root.pruningVectorPart1);
			MatrixUitls.print(root.pruningVectorPart2);
			MatrixUitls.print(trapdoor.trapdoorPart1.transpose());
			MatrixUitls.print(trapdoor.trapdoorPart2.transpose());*/
			System.out.printf("%-10s\t%.8f\t%-20s\t%.8f\n", "score", score, "thresholdScore", thresholdScore);
			// 上层父节点和查询向量之间的相关性评分都比阈值threshold小，那么下层的叶子结点会更小。
			// 大于0.0004对于上层节点不一定有用，但是对于下层节点是可能是有效的。因为乘积只可能是
			//
			if (score > PRUNE_THRESHOLD_SCORE && (score > thresholdScore/* || minHeap.size() < requestNumber*/)) {
				if (root.left != null) {
					System.out.println("left");
					dfs(root.left, trapdoor, requestNumber, minHeap);
				}
				if (root.right != null) {
					System.out.println("right");
					dfs(root.right, trapdoor, requestNumber, minHeap);
				}
			} else {
				System.out.println("score:" + score + " no bigger than thresholdScore:" + thresholdScore + " or socre <" + 0.0004);
				pruneCount++;
				System.out.println();
			}
		}
	}
	/**
	 * 计算跟查询向量之间最不相关的文档.
	 *
	 * @param resultList
	 * @param queryVector
	 * @return
	 */
	private HACTreeNode getMinScoreNode(List<HACTreeNode> resultList, Matrix queryVector) {
		HACTreeNode result = null;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < resultList.size(); i++) {
			double currentScore = scoreForPruning(resultList.get(i), queryVector);
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
	 * @param queryVector
	 * @return
	 */
	private double scoreForPruning(HACTreeNode root, Matrix queryVector) {
		return root.pruningVector.times(queryVector).get(0, 0);
	}


	/**
	 * 搜索的时候用于更新最低阈值分数。
	 *
	 * @param resultList
	 * @param queryVector
	 * @return
	 */
	private double updateThresholdScore(List<HACTreeNode> resultList, Matrix queryVector) {

		double min = Double.MAX_VALUE;
		for (int i = 0; i < resultList.size(); i++) {
			double score = resultList.get(i).pruningVector.times(queryVector).get(0, 0);
			// 更新最小相关性评分.
			if (score < min) {
				min = score;
			}
		}
		return min;
	}
}
