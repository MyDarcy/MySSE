package com.frobisher.linux.accelerate.pv;

import com.frobisher.linux.accelerate.DiagonalMatrixUtils;
import com.frobisher.linux.utils.ByteArrayUtils;

import java.util.*;

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

	private Map<HACTreeNode, Double> nodeScoreMapForThreshold;

	/**
	 * 实现方式1: 采用堆的性性质.
	 *
	 * @param root
	 * @param trapdoor
	 * @param requestNumber
	 * @return
	 */
	public SearchResult search(HACTreeNode root, Trapdoor trapdoor, int requestNumber) {

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

		System.out.println("SearchAlgorithm search start.");
		long start = System.currentTimeMillis();
		nodeScoreMapForThreshold = new HashMap<>(requestNumber);
//		allDocumentSocreQueue = new PriorityQueue<>(maxComparator);
		PriorityQueue<HACTreeNode> minHeap = new PriorityQueue<>(minComparator);
		dfs(root, trapdoor, requestNumber, minHeap);
		System.out.println("tree time:" + (System.currentTimeMillis() - start) + "ms");
//		System.out.println("depth-high-score-branch-first-search");
//		dhsbsf2(root, trapdoor, requestNumber, minHeap);

		List<HACTreeNode> leafNodes = new ArrayList<>();
		getLeafNodes(root, leafNodes);
//		System.out.println("leafNodes.size():" + leafNodes.size());
		PriorityQueue<HACTreeNode> minHeap2 = new PriorityQueue<>(minComparator);
		start = System.currentTimeMillis();
		sequential(leafNodes, trapdoor, requestNumber, minHeap2);
		System.out.println("sequential time:" + (System.currentTimeMillis() - start) + "ms");


		PriorityQueue<HACTreeNode> maxHeap = new PriorityQueue<>(maxComparator);
		maxHeap.addAll(minHeap);
		// 服务器端排序，然后返回top-K个最相关的文档.

		System.out.println("total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("SearchAlgorithm search end.");

		System.out.println("requestNumber:" + requestNumber);
		System.out.println("leafNodeCount:" + leafNodeCount);
		System.out.println("containsCount:" + containsCount);
		System.out.println("computeCount:" + computeCount);
		System.out.println("pruneCount:" + pruneCount);


//		System.out.println("all document-size:" + allDocumentSocreQueue.size());
//		System.out.println("all document-score.");
//		while (!allDocumentSocreQueue.isEmpty()) {
//			HACTreeNode node = allDocumentSocreQueue.poll();
//			System.out.printf("%-60s%.8f\n", node.fileDescriptor, scoreForPruning(node, trapdoor));
//		}

//		System.out.println("\nresult document-score.");
		PriorityQueue<HACTreeNode> result = new PriorityQueue<>(maxComparator);
		byte[] resultXorBytes = new byte[32];
		while (!maxHeap.isEmpty()) {
			HACTreeNode node = maxHeap.poll();
			result.add(node);
			resultXorBytes = ByteArrayUtils.xorArrays(resultXorBytes, node.digest.digest());
//			System.out.println(node.digest.digest().length);;
//			System.out.printf("%-60s%.8f\n", node.fileDescriptor,scoreForPruning(node, trapdoor));
		}

		SearchResult searchResult = new SearchResult();
		searchResult.byteXorResults = resultXorBytes;
		searchResult.result = result;

		return searchResult;
	}

	private void getLeafNodes(HACTreeNode root, List<HACTreeNode> leafNodes) {
		if (root.left == null && root.right == null) {
			leafNodes.add(root);
			return;
		}

		if (root.left != null) {
			getLeafNodes(root.left, leafNodes);
		}

		if (root.right != null) {
			getLeafNodes(root.right, leafNodes);
		}
	}

	private void sequential(List<HACTreeNode> leafNodes, Trapdoor trapdoor, int requestNumber, PriorityQueue<HACTreeNode> minHeap) {
		Map<HACTreeNode, Double> map = new HashMap<>();
		int count = 0;
		for (int i = 0; i < leafNodes.size(); i++) {
			HACTreeNode node = leafNodes.get(i);
			double score = scoreForPruning(node, trapdoor);
			count++;
			map.put(node, score);
			if (minHeap.size() < requestNumber) {
				minHeap.add(node);
			} else {
				// 大于的话,
				if (score > map.get(minHeap.peek())) {
					// 移除当前的top-K个分值较小的节点，加入当前节点
					minHeap.poll();
					minHeap.add(node);
				}
			}
		}
		System.out.println("map.size():" + map.size() + ", count:" + count);
	}

	public static final double PRUNE_THRESHOLD_SCORE = 0.0004;

	private void dfs(HACTreeNode root, Trapdoor trapdoor, int requestNumber, PriorityQueue<HACTreeNode> minHeap) {
		// 是叶子结点.
		if (root.left == null && root.right == null) {
			leafNodeCount++;
			double scoreForPrune = scoreForPruning(root, trapdoor);
			computeCount++;
			if (!nodeScoreMapForThreshold.containsKey(root)) {
				nodeScoreMapForThreshold.put(root, scoreForPrune);
			}

			// 并且候选结果集合中没有top-K个元素.
			int size = minHeap.size();
			if (scoreForPrune >= PRUNE_THRESHOLD_SCORE) {
				if (size < requestNumber - 1) {
//					System.out.println("< (N-1) add:" + root.fileDescriptor);
					minHeap.add(root);
					// 已经找到了 N-1个文档，然后将当前文档加入, 但是要更新现在的阈值评分.
				} else if (size == (requestNumber - 1)) {
					minHeap.add(root);
//					System.out.println("= (N-1) add:" + root.fileDescriptor);
					HACTreeNode peekNode = minHeap.peek();
					if (nodeScoreMapForThreshold.containsKey(peekNode)) {
						thresholdScore = nodeScoreMapForThreshold.get(peekNode);
						containsCount++;
					} else {
						thresholdScore = scoreForPruning(peekNode, trapdoor);
						computeCount++;
					}
//					System.out.println("new thresholdScore:" + thresholdScore);
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
						HACTreeNode peekNode = minHeap.peek();
						if (nodeScoreMapForThreshold.containsKey(peekNode)) {
							thresholdScore = nodeScoreMapForThreshold.get(peekNode);
							containsCount++;
						} else {
							thresholdScore = scoreForPruning(peekNode, trapdoor);
							computeCount++;
						}
//					  System.out.println("new thresholdScore:" + thresholdScore);
					}
				}
			}

//			else {
////				System.out.println("leaf node not add for score < " + PRUNE_THRESHOLD_SCORE);
//			}

			// 非叶子结点。
		} else {
			double score = scoreForPruning(root, trapdoor);
			computeCount++;
			/*MatrixUitls.print(root.pruningVectorPart1);
			MatrixUitls.print(root.pruningVectorPart2);
			MatrixUitls.print(trapdoor.trapdoorPart1.transpose());
			MatrixUitls.print(trapdoor.trapdoorPart2.transpose());*/
//			System.out.printf("%-10s\t%.8f\t%-20s\t%.8f\n", "score", score, "thresholdScore", thresholdScore);
			// 上层父节点和查询向量之间的相关性评分都比阈值threshold小，那么下层的叶子结点会更小。
			// 大于0.0004对于上层节点不一定有用，但是对于下层节点是可能是有效的。因为乘积只可能是
			//
			if (score > PRUNE_THRESHOLD_SCORE && (score > thresholdScore/* || minHeap.size() < requestNumber*/)) {
				if (root.left != null) {
//					System.out.println("left");
					dfs(root.left, trapdoor, requestNumber, minHeap);
				}
				if (root.right != null) {
//					System.out.println("right");
					dfs(root.right, trapdoor, requestNumber, minHeap);
				}
			} else {
//				System.out.println("score:" + score + " no bigger than thresholdScore:" + thresholdScore + " or socre <" + 0.0004);
				pruneCount++;
//				System.out.println();
			}
		}
	}


	/**
	 * 高分值分支first.
	 * depth-high-score-branch-first-search
	 * @param root
	 * @param trapdoor
	 * @param requestNumber
	 * @param minHeap
	 */
	private void dhsbsf(HACTreeNode root, Trapdoor trapdoor, int requestNumber, PriorityQueue<HACTreeNode> minHeap) {
		// 是叶子结点.
		if (root.left == null && root.right == null) {
			leafNodeCount++;

			double scoreForPrune = 0.0;

			// 之前已经计算过了.
			if (nodeScoreMapForThreshold.containsKey(root)) {
				scoreForPrune = nodeScoreMapForThreshold.get(root);
			} else {
				scoreForPrune = scoreForPruning(root, trapdoor);
				computeCount++;
			}
			if (!nodeScoreMapForThreshold.containsKey(root)) {
				nodeScoreMapForThreshold.put(root, scoreForPrune);
			}
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
					HACTreeNode peekNode = minHeap.peek();
					if (nodeScoreMapForThreshold.containsKey(peekNode)) {
						thresholdScore = nodeScoreMapForThreshold.get(peekNode);
						containsCount++;
					} else {
						thresholdScore = scoreForPruning(peekNode, trapdoor);
						computeCount++;
					}
					System.out.println("new thresholdScore:" + thresholdScore);
//					// System.out.println("thresholdSocre:" + thresholdScore);
//					// 仍然时叶子节点，但是候选结果集合中已经有了N个文档.
				} else {
					// 那么此时如果当前结点跟查询之间的相关性评分大于阈值，那么是需要更新
					// 候选结果集合的。
					if (/*scoreForPruning(root, trapdoor)*/ scoreForPrune > thresholdScore) {
						HACTreeNode minScoreNode = minHeap.poll();
//						//double score = scoreForPruning(minScoreNode, trapdoor);
//						//System.out.println("== (N) remove:" + minScoreNode.fileDescriptor + " socre:" + score);
						minHeap.add(root);
						HACTreeNode peekNode = minHeap.peek();
						if (nodeScoreMapForThreshold.containsKey(peekNode)) {
							thresholdScore = nodeScoreMapForThreshold.get(peekNode);
							containsCount++;
						} else {
							thresholdScore = scoreForPruning(peekNode, trapdoor);
							computeCount++;
						}
						System.out.println("new thresholdScore:" + thresholdScore);
					}
				}
			} else {
				System.out.println("leaf node not add for score < " + PRUNE_THRESHOLD_SCORE);
			}

			// 非叶子结点。
		} else {
			double score = 0.0;
			if (nodeScoreMapForThreshold.containsKey(root)) {
				score = nodeScoreMapForThreshold.get(root);
			} else {
				score = scoreForPruning(root, trapdoor);
				computeCount++;
			}
			System.out.printf("%-10s\t%.8f\t%-20s\t%.8f\n", "score", score, "thresholdScore", thresholdScore);
//			// 上层父节点和查询向量之间的相关性评分都比阈值threshold小，那么下层的叶子结点会更小。
//			// 大于THRESHOLD对于上层节点不一定有用，但是对于下层节点是可能是有效的。因为乘积只可能是
//			// 只是引入的冗余关键词的乘积.
			if (score > PRUNE_THRESHOLD_SCORE && (score > thresholdScore/* || minHeap.size() < requestNumber*/)) {
				if (root.left != null && root.right != null) {
					double leftScore = scoreForPruning(root.left, trapdoor);
					double rightScore = scoreForPruning(root.right, trapdoor);
					computeCount += 2;
					nodeScoreMapForThreshold.put(root.left, leftScore);
					nodeScoreMapForThreshold.put(root.right, rightScore);
					if (Double.compare(leftScore, rightScore) >= 0) {
						System.out.println("left");
						dhsbsf(root.left, trapdoor, requestNumber, minHeap);
						System.out.println("right");
						dhsbsf(root.right, trapdoor, requestNumber, minHeap);
					} else if (Double.compare(leftScore, rightScore) < 0) {
						System.out.println("right");
						dhsbsf(root.right, trapdoor, requestNumber, minHeap);
						System.out.println("left");
						dhsbsf(root.left, trapdoor, requestNumber, minHeap);
					}
				}
				 // 中间节点一定是有两个子节点的,所以这些不用判断。
//				else if (root.left != null) {
//					System.out.println("left");
//					dfs(root.left, trapdoor, requestNumber, minHeap);
//				}else if (root.right != null) {
//					System.out.println("right");
//					dfs(root.right, trapdoor, requestNumber, minHeap);
//				}

			} else {
				System.out.println("score:" + score + " no bigger than thresholdScore:" + thresholdScore + " or socre <" + 0.0004);
				pruneCount++;
				System.out.println();
			}
		}
	}

	private void dhsbsf2(HACTreeNode root, Trapdoor trapdoor, int requestNumber, PriorityQueue<HACTreeNode> minHeap) {
		// 是叶子结点.
		if (root.left == null && root.right == null) {
			leafNodeCount++;

			double scoreForPrune = 0.0;

			// 之前已经计算过了.
			if (nodeScoreMapForThreshold.containsKey(root)) {
				scoreForPrune = nodeScoreMapForThreshold.get(root);
			} else {
				scoreForPrune = scoreForPruning(root, trapdoor);
				computeCount++;
			}
			if (!nodeScoreMapForThreshold.containsKey(root)) {
				nodeScoreMapForThreshold.put(root, scoreForPrune);
			}
			// 并且候选结果集合中没有top-K个元素.
			int size = minHeap.size();
			if (scoreForPrune >= PRUNE_THRESHOLD_SCORE) {
				if (size < requestNumber - 1) {
					minHeap.add(root);
					// 已经找到了 N-1个文档，然后将当前文档加入, 但是要更新现在的阈值评分.
				} else if (size == (requestNumber - 1)) {
					minHeap.add(root);
					HACTreeNode peekNode = minHeap.peek();
					if (nodeScoreMapForThreshold.containsKey(peekNode)) {
						thresholdScore = nodeScoreMapForThreshold.get(peekNode);
						containsCount++;
					} else {
						thresholdScore = scoreForPruning(peekNode, trapdoor);
						computeCount++;
					}
				} else {
					// 那么此时如果当前结点跟查询之间的相关性评分大于阈值，那么是需要更新
					// 候选结果集合的。
					if (/*scoreForPruning(root, trapdoor)*/ scoreForPrune > thresholdScore) {
						HACTreeNode minScoreNode = minHeap.poll();
						minHeap.add(root);
						HACTreeNode peekNode = minHeap.peek();
						if (nodeScoreMapForThreshold.containsKey(peekNode)) {
							thresholdScore = nodeScoreMapForThreshold.get(peekNode);
							containsCount++;
						} else {
							thresholdScore = scoreForPruning(peekNode, trapdoor);
							computeCount++;
						}
					}
				}
			}

			// 非叶子结点。
		} else {
			double score = 0.0;
			if (nodeScoreMapForThreshold.containsKey(root)) {
				score = nodeScoreMapForThreshold.get(root);
			} else {
				score = scoreForPruning(root, trapdoor);
				computeCount++;
			}
			if (score > PRUNE_THRESHOLD_SCORE && (score > thresholdScore/* || minHeap.size() < requestNumber*/)) {
				if (root.left != null && root.right != null) {
					double leftScore = scoreForPruning(root.left, trapdoor);
					double rightScore = scoreForPruning(root.right, trapdoor);
					computeCount += 2;
					nodeScoreMapForThreshold.put(root.left, leftScore);
					nodeScoreMapForThreshold.put(root.right, rightScore);
					if (Double.compare(leftScore, rightScore) >= 0) {
						dhsbsf2(root.left, trapdoor, requestNumber, minHeap);
						dhsbsf2(root.right, trapdoor, requestNumber, minHeap);
					} else if (Double.compare(leftScore, rightScore) < 0) {
						dhsbsf2(root.right, trapdoor, requestNumber, minHeap);
						dhsbsf2(root.left, trapdoor, requestNumber, minHeap);
					}
				}
			} else {
				pruneCount++;
			}
		}
	}

	/**
	 * 计算跟查询向量之间最不相关的文档.
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
	 * @param root
	 * @param trapdoor
	 * @return
	 */
	private double scoreForPruning(HACTreeNode root, Trapdoor trapdoor) {
		/*return root.pruningVector.times(queryVector).get(0, 0);*/
		return DiagonalMatrixUtils.score(root.pruningVectorPart1, trapdoor.trapdoorPart1) +
				DiagonalMatrixUtils.score(root.pruningVectorPart2, trapdoor.trapdoorPart2);
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
			double score = DiagonalMatrixUtils.score(resultList.get(i).pruningVectorPart1, trapdoor.trapdoorPart1)
					+ DiagonalMatrixUtils.score(resultList.get(i).pruningVectorPart2, trapdoor.trapdoorPart2);
			// 更新最小相关性评分.
			if (score < min) {
				min = score;
			}
		}
		return min;
	}
}
