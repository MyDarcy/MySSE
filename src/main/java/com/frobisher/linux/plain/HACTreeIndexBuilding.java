package com.frobisher.linux.plain;

import Jama.Matrix;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.util.stream.Collectors.toList;

/*
 * author: darcy
 * date: 2017/12/18 22:22
 * description: 
*/

class HacTreeNodePairScore {
	HACTreeNode node1;
	HACTreeNode node2;
	double score;

	public HacTreeNodePairScore(HACTreeNode node1, HACTreeNode node2, double score) {
		this.node1 = node1;
		this.node2 = node2;
		this.score = score;
	}
}

public class HACTreeIndexBuilding {

	// 秘密钥
	public MySecretKey mySecretKey;
	public Map<String, byte[]> fileBytesMap = new HashMap<>();
	public Comparator<HacTreeNodePairScore> maxComparator;
	public Initialization initialization = new Initialization();

	public HACTreeIndexBuilding(MySecretKey mySecretKey) {
		this.mySecretKey = mySecretKey;
	}

	public HACTreeIndexBuilding(MySecretKey mySecretKey, Initialization initialization) {
		this.mySecretKey = mySecretKey;
		this.initialization = initialization;
	}

	// 添加的冗余关键词的权重取值范围
	// 论文中取值 -0.01~0.01 -0.03~0.03 -0.05~0.05
	// 最佳取值 -0.01~0.01
	// 这里我取值 0.005~0.005. 发现搜出来的结果没法看。
	public RealDistribution distribution = new UniformRealDistribution(-0.01, 0.01);
	public Random random = new Random(31);


	// 去掉矩阵的求逆和转置运算。

	// 去掉加密操作。

	/**
	 * *  HAC-tree中节点u是一个五元组〈VM,PL,PR,FD,sig〉, 其中，u.VM是是一个剪枝向量，u.PL和u.PR分别是指向节点u的左右孩子节点。
	 * u.FD代表的是文档额唯一的ID。u.sig代表的是u.FD文档的消息摘要。此外，u.VC是聚类C_u的聚类中心向量，u.N表示聚类C_u中文档的数目，
	 * 聚类C_u代表的是以u为根节点的子树中所有的叶子结点代表的文档，同时注意u.VC和u.N仅仅存在于HAC-Tree的构造阶段，
	 * 不需要存储在HAC-Tree中。根据节点u的类型，我们详细的描述HAC-Tree节点如下
	 * 如果u是叶子结点，那么u.PL= u.PR= ϕ , u.FD 存储的是文档的id，u.VM和u.VC都存储的是当前文档的向量，u.N=1, u.sig
	 * 存储的是当前文档的消息摘要，消息摘要主要用于后续的搜搜结果的验证。
	 * 如果u是一个内部的中间节点，那么u.FD= ϕ, u.sig= ϕ, u.PL和u.PR代表节点u的左右孩子节点。u.N= u.PL.N + u.PR.N,
	 * 而u.VM则是从聚类C_u中提取的最大向量。u.VC则是聚类C_u的聚类中心向量。
	 * u.VM= (max) ⃗{u.PL.VM,u.PR.VM}    (9)
	 * u.VC=  (u.PL.N × u.PL.VC+u.PR.N+u.PR.VC)/(u.PL.N+u.PR.N)    (10)
	 *
	 * @return
	 */
	public HACTreeNode buildHACTreeIndex() throws NoSuchAlgorithmException {
		System.out.println("HACTreeIndexBuilding buildHACTreeIndex start.");
		long start = System.currentTimeMillis();

		maxComparator = new Comparator<HacTreeNodePairScore>() {
			@Override
			public int compare(HacTreeNodePairScore nodePairScore1, HacTreeNodePairScore nodePairScore2) {
				if (Double.compare(nodePairScore1.score, nodePairScore2.score) > 0) {
					return -1;
				} else if (Double.compare(nodePairScore1.score, nodePairScore2.score) < 0) {
					return 1;
				} else {
					return 0;
				}
			}
		};

		Set<HACTreeNode> currentProcessingHACTreeNodeSet = new HashSet<>();
		Set<HACTreeNode> newGeneratedHACTreeNodeSet = new HashSet<>();

		File parentFile = new File(initialization.PLAIN_DIR);
		File[] files = parentFile.listFiles();

		PriorityQueue<Double> tfIdfMinHeap = new PriorityQueue<>(20, Double::compare);
		PriorityQueue<Double> tfIdfMaxHeap = new PriorityQueue<>(20, Comparator.reverseOrder());

		for (int i = 0; i < files.length; i++) {
			// System.out.println(files[i].getName());
			Matrix P = new Matrix(1, initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER);

			// 当前文档的长度.
			int lengthOfFile = initialization.fileLength.get(files[i].getName());

			Map<String, Integer> keywordFrequencyInCurrentDocument =
					initialization.keywordFrequencyInDocument.get(files[i].getName());

			double denominator = tfDenominator(keywordFrequencyInCurrentDocument, lengthOfFile);
			double molecule = 0;

/*			// 当前文档的长度.
			int lengthOfFile = Initialization.fileLength.get(files[i].getName());

			Map<String, Integer> keywordFrequencyInCurrentDocument =
					Initialization.keywordFrequencyInDocument.get(files[i].getName());

			// double denominator = tfDenominator(keywordFrequencyInCurrentDocument, lengthOfFile);
			// 只用重复计算一次.
//			double denominator3 = tfDenominator3(keywordFrequencyInCurrentDocument, lengthOfFile);

			double molecule = 0;*/

			int fileNumbers = initialization.fileLength.size();

			for (String key : keywordFrequencyInCurrentDocument.keySet()) {
				int index = initialization.dict.indexOf(key);
				if (index != -1) {
					/*int score = (int)Math.ceil(upper * score(lengthOfFile, keywordFrequencyInCurrentDocument.get(key),
							Initialization.numberOfDocumentContainsKeyword.get(key), files.length));
					P.set(0, index, score);*/

					// 本方案中， 文档向量中存储的是归一化的TF值.
					molecule = (1 + Math.log(keywordFrequencyInCurrentDocument.get(key))) / lengthOfFile;
					double tfValue = molecule / denominator;


					/*System.out.printf("%-20s %10s  %-10s %-15s %-10s\n", "key", "freq", "molecule", "denominator", "tfValue");
					System.out.printf("%-20s %10d  %-10f %-15f %-10f\n", key, keywordFrequencyInCurrentDocument.get(key)
							,molecule, denominator, tfValue);*/

//					double tfIdfValue3 = tfIdfVersion2(lengthOfFile, keywordFrequencyInCurrentDocument.get(key),
//							fileNumbers, Initialization.numberOfDocumentContainsKeyword.get(key));
					P.set(0, index, tfValue /*tfIdfValue3*/);

					double tfIdfValue3 = tfValue;

					// 取最小的几个数字.
					if (tfIdfMaxHeap.size() < 40) {
						tfIdfMaxHeap.add(tfIdfValue3);
					} else if (tfIdfValue3 < tfIdfMaxHeap.peek()) {
						tfIdfMaxHeap.add(tfIdfValue3);
						tfIdfMaxHeap.poll();
					}

					// 取最大的几个数字.
					if (tfIdfMinHeap.size() < 40) {
						tfIdfMinHeap.add(tfIdfValue3);
					} else if (tfIdfValue3 > tfIdfMinHeap.peek()) {
						tfIdfMinHeap.add(tfIdfValue3);
						tfIdfMinHeap.poll();
					}

				}
			}


			/*MatrixUitls.print(P);*/

			/*double[] sample = distribution.sample(Initialization.DUMMY_KEYWORD_NUMBER);*/
			double[] sample = new double[initialization.DUMMY_KEYWORD_NUMBER];
			for (int j = 0; j < (initialization.DUMMY_KEYWORD_NUMBER); j++) {
				P.set(0, initialization.DICTIONARY_SIZE + j, sample[j]);
			}


			HACTreeNode currentNode = new HACTreeNode(P, P, 1,
					null, null, files[i].getName(), files[i].getName());
			/*System.out.println(currentNode);*/

			currentProcessingHACTreeNodeSet.add(currentNode);
		}

		System.out.println("leaf node numbers:" + currentProcessingHACTreeNodeSet.size());

		System.out.println("start construct hac-tree.");
		int round = 1;
		while (currentProcessingHACTreeNodeSet.size() > 1) {
			System.out.println("\nthe " + (round++) + "'s round to build tree.");
			System.out.println("currentProcessingHACTreeNodeSet.size():" + currentProcessingHACTreeNodeSet.size());

			PriorityQueue<HacTreeNodePairScore> maxHeap = getPriorityQueue(currentProcessingHACTreeNodeSet);
			Set<HACTreeNode> managedNodeSet = new HashSet<>();

			// 当前处理的结果集合中可能是奇数个, 所以两两出可能只剩下一个，最后。
			while (currentProcessingHACTreeNodeSet.size() > 1) {
				// HACTreeNodePair mostCorrespondNodePair = findMostCorrespondNodePair(currentProcessingHACTreeNodeSet);
				HacTreeNodePairScore mostSimilarNodePair = maxHeap.poll();
				// 最相关的两个节点有节点是已经处理过了。
				if (managedNodeSet.contains(mostSimilarNodePair.node1) || managedNodeSet.contains(mostSimilarNodePair.node2)) {
					continue;
				}

				HACTreeNodePair mostCorrespondNodePair = new HACTreeNodePair(mostSimilarNodePair.node1, mostSimilarNodePair.node2);

				Matrix parentNodePruningVector = getParentNodePruningVector(mostCorrespondNodePair);

				/*MatrixUitls.print(parentNodePruningVectors.get(0));
				MatrixUitls.print(parentNodePruningVectors.get(1));*/

				Matrix parentNodeCenterVector = getParentNodeCenterVector(mostCorrespondNodePair);
				int parentNumberOfNodeInCurrentCluster = mostCorrespondNodePair.node1.numberOfNodeInCurrentCluster
						+ mostCorrespondNodePair.node2.numberOfNodeInCurrentCluster;

				HACTreeNode parentNode = new HACTreeNode(parentNodePruningVector,
						parentNodeCenterVector, parentNumberOfNodeInCurrentCluster,
						mostCorrespondNodePair.node1, mostCorrespondNodePair.node2, null, null);
				currentProcessingHACTreeNodeSet.remove(mostCorrespondNodePair.node1);
				currentProcessingHACTreeNodeSet.remove(mostCorrespondNodePair.node2);

				// 更新待处理的节点集合。
				managedNodeSet.add(mostCorrespondNodePair.node1);
				managedNodeSet.add(mostCorrespondNodePair.node2);
				newGeneratedHACTreeNodeSet.add(parentNode);
			}
			if (newGeneratedHACTreeNodeSet.size() > 0) {
				currentProcessingHACTreeNodeSet.addAll(newGeneratedHACTreeNodeSet);
				newGeneratedHACTreeNodeSet.clear();
			}
		}

		System.out.println("min max tf-idf value");
		while (!tfIdfMaxHeap.isEmpty()) {
			System.out.print(tfIdfMaxHeap.poll() + " ");
		}
		System.out.println();
		while (!tfIdfMinHeap.isEmpty()) {
			System.out.print(tfIdfMinHeap.poll() + " ");
		}
		System.out.println();

		System.out.println("currentProcessingHACTreeNodeSet.size():" + currentProcessingHACTreeNodeSet.size());
		// currentProcessingHACTreeNodeSet中一定是有一个节点的.
		HACTreeNode root = currentProcessingHACTreeNodeSet.iterator().next();
		System.out.println("build hac tree index total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("HACTreeIndexBuilding buildHACTreeIndex finished.");
		return root;
	}

	private PriorityQueue<HacTreeNodePairScore> getPriorityQueue(Set<HACTreeNode> hacTreeNodePairScoreSet) {
		System.out.println("getPriorityQueue start.");
		long start = System.currentTimeMillis();
		List<HACTreeNode> list = hacTreeNodePairScoreSet.stream().collect(toList());
		PriorityQueue<HacTreeNodePairScore> maxHeap = new PriorityQueue<>(maxComparator);
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				maxHeap.add(new HacTreeNodePairScore(list.get(i), list.get(j), correspondingScore(list.get(i), list.get(j))));
			}
		}
		System.out.println("time:" + (System.currentTimeMillis() - start));
		System.out.println("getPriorityQueue end.");
		return maxHeap;
	}

	private double tfDenominator3(Map<String, Integer> keywordFrequencyInCurrentDocument, int lengthOfFile) {
		double sum = 0.0;
		int fileNumber = initialization.fileLength.size();
		for (String key : keywordFrequencyInCurrentDocument.keySet()) {
			int frequency = keywordFrequencyInCurrentDocument.get(key);
			int containNumber = initialization.numberOfDocumentContainsKeyword.get(key);
			sum += Math.pow(Math.log(1 + frequency) * Math.log(1 + fileNumber * 1.0 / containNumber), 2);
		}
		return Math.sqrt(sum);
	}

	private double tfIdfVersion2(int lengthOfFile, Integer frequency, int fileNumbers, Integer containNumber) {
		return (1 + Math.log(frequency)) / lengthOfFile * Math.log(1 + fileNumbers * 1.0 / containNumber);
	}

	private double tfIdfVersion1(String key, Map<String, Integer> keywordFrequencyInCurrentDocument, int fileNumbers) {
		return  -keywordFrequencyInCurrentDocument.get(key) *
				Math.log10(initialization.numberOfDocumentContainsKeyword.get(key) / fileNumbers);
	}

	/**
	 * 获取两个聚类的中心向量.
	 *
	 * @param nodePair
	 * @return
	 */
	private Matrix getParentNodeCenterVector(HACTreeNodePair nodePair) {
		int newNumberOfNode = nodePair.node1.numberOfNodeInCurrentCluster + nodePair.node2.numberOfNodeInCurrentCluster;
		Matrix parentCenterVector = new Matrix(1, initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER);
		for (int i = 0; i < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; i++) {
			double sum = nodePair.node1.clusterCenterVector.get(0, i) * nodePair.node1.numberOfNodeInCurrentCluster
					+ nodePair.node2.clusterCenterVector.get(0, i) * nodePair.node2.numberOfNodeInCurrentCluster;
			parentCenterVector.set(0, i, sum / newNumberOfNode);
		}
		return parentCenterVector;
	}

	/**
	 * 一堆HACTreeNode中找最相关的文档。即相关性评分最高的文档.
	 * <p>
	 * version-1: 暴力的方法, n/2 * n * n * (向量维度的平方);
	 * version-2: 网上的凸包问题的解法, n^2 -> nlogn, 但是那个是2维平面的点，用到了2维的特性，拓展到n维，效率有没有提升，多大的提升都是未知的.
	 *
	 * @param currentProcessingHACTreeNodeSet
	 * @return
	 */
	private HACTreeNodePair findMostCorrespondNodePair(Set<HACTreeNode> currentProcessingHACTreeNodeSet) {
		System.out.println("findMostCorrespondNodePair start.");
		long start = System.currentTimeMillis();
		int maxIndex1 = 0;
		int maxIndex2 = 0;
		double max = Double.MIN_VALUE;
		List<HACTreeNode> list = currentProcessingHACTreeNodeSet.stream().collect(toList());
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				double score = correspondingScore(list.get(i), list.get(j));
				if (score > max) {
					maxIndex1 = i;
					maxIndex2 = j;
					max = score;
				}
				/*System.out.println(list.get(i) + "\t" + list.get(j) + "\tscore:" +score );*/
			}
		}
		/*System.out.println(list.get(maxIndex1) + "\t" + list.get(maxIndex2) + "\t max score:" + max);
		System.out.println();*/
		System.out.println("total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("findMostCorrespondNodePair finished.");
		return new HACTreeNodePair(list.get(maxIndex1), list.get(maxIndex2));
	}

	/**
	 * 获取两个子节点剪枝向量对应位置max值组成的父节点的剪枝向量.
	 * <p>
	 * 但是问题是，使用矩阵加密后，仍然是这样的构造父节点的剪枝向量吗
	 * 这样有效吗?
	 *
	 * @param pair
	 * @return
	 */
	public Matrix getParentNodePruningVector(HACTreeNodePair pair) {
		Matrix parent = new Matrix(1, initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER);
		for (int i = 0; i < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; i++) {
			parent.set(0, i, Double.max(pair.node1.pruningVector.get(0, i), pair.node2.pruningVector.get(0, i)));
		}
		return parent;
	}

	/**
	 * 节点和节点之间的相关性评分。
	 *
	 * @param node1
	 * @param node2
	 * @return
	 */
	private double correspondingScore(HACTreeNode node1, HACTreeNode node2) {
		// 剪枝向量与剪枝向量的乘积就是相关性评分。
		/*Matrix matrix1 = node1.pruningVectorPart1.times(node2.pruningVectorPart1.transpose());
		Matrix matrix2 = node1.pruningVectorPart2.times(node2.pruningVectorPart2.transpose());
		return matrix1.get(0, 0) + matrix2.get(0, 0);*/

		// 应该是使用相关性评分来求节点与节点之间的关系。
		// 节点之间的关系通过聚类中心向量之间的score来体现。

		// 测试了一下矩阵乘法，其实是比较慢的。
		/*Matrix matrix = node1.clusterCenterVector.times(node2.clusterCenterVector.transpose());
		return matrix.get(0, 0);*/
		double[][] node1Array = node1.clusterCenterVector.getArray();
		double[][] node2Array = node2.clusterCenterVector.getArray();
		double sum = 0;
		/*for (int i = 0; i < node1Array.length; i++) {
			for (int j = 0; j < node1Array[0].length; j++) {
				sum += node1Array[i][j] * node2Array[i][j];
			}
		}*/

		for (int i = 0; i < node1Array[0].length; i++) {
			sum += node1Array[0][i] * node2Array[0][i];
		}

		return sum;
	}

	/**
	 * @param keywordFrequencyInCurrentDocument
	 * @param lengthOfFile
	 * @return
	 */
	private double tfDenominator(Map<String, Integer> keywordFrequencyInCurrentDocument, int lengthOfFile) {
		double denominator = 0;
		for (String keyword : keywordFrequencyInCurrentDocument.keySet()) {
			// 所有单个关键词按此公式计算出来的TF值平方.
			denominator += Math.pow((1 + Math.log(keywordFrequencyInCurrentDocument.get(keyword))) / lengthOfFile, 2);
		}
		// 返回sqrt的数字.
		return Math.sqrt(denominator);
	}

	/**
	 * 求tf-idf的分值。
	 *
	 * @param lengthOfFile                    文件i的长度.
	 * @param frequency                       当前关键词在文档i中出现的频率.
	 * @param numberOfDocumentContainsKeyword 有多少个文档包含关键词.
	 * @param filesNumber                     总的文档的数目.
	 * @return
	 */
	private double score(int lengthOfFile, Integer frequency, Integer numberOfDocumentContainsKeyword, int filesNumber) {
		return ((1 + Math.log(frequency)) / lengthOfFile)
				* Math.log(1 + filesNumber / numberOfDocumentContainsKeyword);
	}

	public static void main(String[] args) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException {
		Initialization initialization = new Initialization();
		MySecretKey mySecretKey = initialization.getMySecretKey();
		HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey);

		// test2
		long start = System.currentTimeMillis();
		System.out.println("hac tree index building:");
		hacTreeIndexBuilding.buildHACTreeIndex();
		System.out.println("finish hac tree index building:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println((1 + Math.log(16)) / (1 + Math.log(1)));
		System.out.println(0.156131 / 0.041386);
	}
}
