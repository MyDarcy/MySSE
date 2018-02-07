package com.frobisher.linux.ordinary.pv;

import Jama.Matrix;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.util.stream.Collectors.toList;

/*
 * author: darcy
 * date: 2017/12/18 22:22
 * description: 
*/



public class HACTreeIndexBuildingSimulation {

	// 秘密钥
	public MySecretKey mySecretKey;
	public Map<Integer, byte[]> fileBytesMap = new HashMap<>();
	public Comparator<HacTreeNodePairScore> maxComparator;
	public Initialization initialization;

	// 实例块中初始化maxComparator。
	{
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
	}

	static class HacTreeNodePairScore {
		HACTreeNode node1;
		HACTreeNode node2;
		double score;

		public HacTreeNodePairScore(HACTreeNode node1, HACTreeNode node2, double score) {
			this.node1 = node1;
			this.node2 = node2;
			this.score = score;
		}
	}

	public HACTreeIndexBuildingSimulation(MySecretKey mySecretKey) {
		this.mySecretKey = mySecretKey;
	}

	public HACTreeIndexBuildingSimulation(MySecretKey mySecretKey, Initialization initialization) {
		this.mySecretKey = mySecretKey;
		this.initialization = initialization;
	}

	// 添加的冗余关键词的权重取值范围
	// 论文中取值 -0.01~0.01 -0.03~0.03 -0.05~0.05
	// 因为本方案中文档向量中的tf-idf值是0.00x级别的。
	public RealDistribution distribution = new UniformRealDistribution(-0.00001, 0.00001);
	public Random random = new Random(System.currentTimeMillis());

	/**
	 * 求MySecretKey中两个矩阵的转置矩阵和逆矩阵, 因为在构造索引阶段要用。
	 */
	public void generateAuxiliaryMatrix() {
		System.out.println("HACTreeIndexBuilding generateAuxiliaryMatirx start.");
		long start = System.currentTimeMillis();
		long nstart = start;

		AuxiliaryMatrix.M1Transpose = mySecretKey.M1.transpose();
		AuxiliaryMatrix.M2Transpose = mySecretKey.M2.transpose();
		System.out.println("two transpose:" + (System.currentTimeMillis() - start) + "ms");

		start = System.currentTimeMillis();
		AuxiliaryMatrix.M1Inverse = mySecretKey.M1.inverse();
		AuxiliaryMatrix.M2Inverse = mySecretKey.M2.inverse();
		System.out.println("two inverse:" + (System.currentTimeMillis() - start) + "ms");

		System.out.println("transpose & inverse time:" + (System.currentTimeMillis() - nstart) + "ms");
		System.out.println("HACTreeIndexBuilding generateAuxiliaryMatrix finished.");
	}

	/**
	 * 加密文档，
	 *
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 */
	public void encryptFiles() throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
		System.out.println("HACTreeIndexBuilding encryptFiles start.");
		long start = System.currentTimeMillis();
		for (int i = 0; i < initialization.simulationDocuments.size(); i++) {
			Matrix matrix = initialization.simulationDocuments.get(i);
			double[][] array = matrix.getArray();
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < initialization.simulationDictSize; j++) {
				if (Double.compare(0, array[j][0]) != 0) {
					sb.append(j + "\t");
				}
			}
			byte[] bytes = sb.toString().trim().getBytes();
			fileBytesMap.put(i, bytes);
		}

//		System.out.println("total time:" + (System.currentTimeMillis() - start) + "ms");
//		System.out.println("HACTreeIndexBuilding encryptFiles finish.");
	}

	public HACTreeNode buildHACTreeIndex() throws NoSuchAlgorithmException {
//		System.out.println("HACTreeIndexBuilding buildHACTreeIndex start.");
		long start = System.currentTimeMillis();
		Set<HACTreeNode> currentProcessingHACTreeNodeSet = new HashSet<>();
		Set<HACTreeNode> newGeneratedHACTreeNodeSet = new HashSet<>();

		PriorityQueue<Double> tfIdfMinHeap = new PriorityQueue<>(20, Double::compare);
		PriorityQueue<Double> tfIdfMaxHeap = new PriorityQueue<>(20, Comparator.reverseOrder());

		for (int i = 0; i < initialization.simulationDocumentNumber; i++) {
			// System.out.println(files[i].getName());

			Matrix P = initialization.simulationDocuments.get(i);
			double[] sample = distribution.sample(initialization.simulationDummykeywordIndexSet.size());
			int indexCount = 0;
			for (int index : initialization.simulationDummykeywordIndexSet) {
				P.set(index, 0, sample[indexCount++]);
			}
			// 获取消息摘要.
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] keyBytes = mySecretKey.secretKey.getEncoded();

			byte[] fileBytes = fileBytesMap.get(i);
			byte[] bytes = new byte[keyBytes.length + fileBytes.length];
			System.arraycopy(keyBytes, 0, bytes, 0, keyBytes.length);
			System.arraycopy(fileBytes, 0, bytes, keyBytes.length, fileBytes.length);
			messageDigest.update(bytes);

			HACTreeNode currentNode = new HACTreeNode(P, P, 1,
					null, null, String.valueOf(i), messageDigest);

			/*HACTreeNode currentNode = new HACTreeNode(P, P, 1,
					null, null, files[i].getName(), files[i].getName());*/
			/*System.out.println(currentNode);*/

			currentProcessingHACTreeNodeSet.add(currentNode);
		}

//		System.out.println("start construct hac-tree.");
		int round = 1;
		while (currentProcessingHACTreeNodeSet.size() > 1) {
//			System.out.println("the " + (round++) + "'s round to build tree.");

			PriorityQueue<HacTreeNodePairScore> maxHeap = getPriorityQueue(currentProcessingHACTreeNodeSet);
			Set<HACTreeNode> managedNodeSet = new HashSet<>();

			while (currentProcessingHACTreeNodeSet.size() > 1) {
				//HACTreeNodePair mostCorrespondNodePair = findMostCorrespondNodePair(currentProcessingHACTreeNodeSet);

				HacTreeNodePairScore mostSimilarNodePair = maxHeap.poll();
				// 最相关的两个节点有节点是已经处理过了。
				if (managedNodeSet.contains(mostSimilarNodePair.node1)
						|| managedNodeSet.contains(mostSimilarNodePair.node2)) {
					continue;
				}

				HACTreeNodePair mostCorrespondNodePair = new HACTreeNodePair(mostSimilarNodePair.node1,
						mostSimilarNodePair.node2);

				Matrix parentNodePruningVector = getParentNodePruningVectorByOne(mostCorrespondNodePair);
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

//		System.out.println("currentProcessingHACTreeNodeSet.size():" + currentProcessingHACTreeNodeSet.size());
		// currentProcessingHACTreeNodeSet中一定是有一个节点的.
		HACTreeNode root = currentProcessingHACTreeNodeSet.iterator().next();
//		System.out.println("build hac tree index total time:" + (System.currentTimeMillis() - start) + "ms");
//		System.out.println("HACTreeIndexBuilding buildHACTreeIndex finished.");
		return root;
	}

	private Matrix getParentNodePruningVectorByOne(HACTreeNodePair pair) {
		Matrix parent = new Matrix(initialization.simulationDictSize, 1);
		for (int i = 0; i < initialization.simulationDictSize; i++) {
			parent.set(i, 0, Double.max(pair.node1.pruningVector.get(i, 0), pair.node2.pruningVector.get(i, 0)));
		}
		return parent;
	}

	public void encryptHACTreeIndex(HACTreeNode root) {
		if (root == null) {
			return;
		}

		// 获取可逆矩阵加密后的Matrix.
		Matrix pa = new Matrix(initialization.simulationDictSize, 1);
		Matrix pb = new Matrix(initialization.simulationDictSize, 1);
		Matrix P = root.pruningVector;
		for (int j = 0; j < initialization.simulationDictSize; j++) {
			// 置0时候相加
			if (!mySecretKey.S.get(j)) {
				double v1 = random.nextDouble();
				// 不是简单的v1和 p-v1,
				pa.set(j, 0, P.get(j, 0) * v1);
				pb.set(j, 0, P.get(j, 0) * (1 - v1));

				// 置1时候相等。
			} else {
				pa.set(j, 0, P.get(j, 0));
				pb.set(j, 0, P.get(j, 0));
			}
		}

		Matrix paEncrypted = AuxiliaryMatrix.M1Transpose.times(pa);
		Matrix pbEncrypted = AuxiliaryMatrix.M2Transpose.times(pb);

		root.pruningVectorPart1 = paEncrypted;
		root.pruningVectorPart2 = pbEncrypted;
		root.pruningVector = null;
		root.clusterCenterVector = null;
		root.numberOfNodeInCurrentCluster = 0;

		if (root.left != null) {
			encryptHACTreeIndex(root.left);
		}
		if (root.right != null) {
			encryptHACTreeIndex(root.right);
		}
	}

	private PriorityQueue<HacTreeNodePairScore> getPriorityQueue(Set<HACTreeNode> hacTreeNodePairScoreSet) {
//		System.out.println("getPriorityQueue start.");
		long start = System.currentTimeMillis();
		List<HACTreeNode> list = hacTreeNodePairScoreSet.stream().collect(toList());
		PriorityQueue<HacTreeNodePairScore> maxHeap = new PriorityQueue<>(maxComparator);
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				maxHeap.add(new HacTreeNodePairScore(list.get(i), list.get(j),
						correspondingScore(list.get(i), list.get(j))));
			}
		}
//		System.out.println("time:" + (System.currentTimeMillis() - start));
//		System.out.println("getPriorityQueue end.");
		return maxHeap;
	}

	/**
	 * 获取两个聚类的中心向量.
	 * 修复了 + -> *
	 * @param nodePair
	 * @return
	 */
	private Matrix getParentNodeCenterVector(HACTreeNodePair nodePair) {
		int newNumberOfNode = nodePair.node1.numberOfNodeInCurrentCluster + nodePair.node2.numberOfNodeInCurrentCluster;
		Matrix parentCenterVector = new Matrix(initialization.simulationDictSize, 1);
		for (int i = 0; i < initialization.simulationDictSize; i++) {
			double sum = nodePair.node1.clusterCenterVector.get(i, 0) * nodePair.node1.numberOfNodeInCurrentCluster
					+ nodePair.node2.clusterCenterVector.get(i, 0) * nodePair.node2.numberOfNodeInCurrentCluster;
			parentCenterVector.set(i, 0, sum / newNumberOfNode);
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
	 *
	 * 但是问题是，使用矩阵加密后，仍然是这样的构造父节点的剪枝向量吗
	 * 这样有效吗?
	 * @param pair
	 * @return
	 */
	public List<Matrix> getParentNodePruningVector(HACTreeNodePair pair) {
		Matrix parent1 = new Matrix(initialization.simulationDictSize, 1);
		Matrix parent2 = new Matrix(initialization.simulationDictSize, 1);
		for (int i = 0; i < initialization.simulationDictSize; i++) {
			parent1.set(i, 0, Double.max(pair.node1.pruningVectorPart1.get(i, 0), pair.node2.pruningVectorPart1.get(i, 0)));
			parent2.set(i, 0, Double.max(pair.node1.pruningVectorPart2.get(i, 0), pair.node2.pruningVectorPart2.get(i, 0)));
		}
		return Arrays.asList(parent1, parent2);
	}

	/**
	 * 节点和节点之间的相关性评分。
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
		/*Matrix matrix = node1.clusterCenterVector.transpose().times(node2.clusterCenterVector);
		return matrix.get(0, 0);*/
		double sum = 0;
		double[][] node1Array = node1.clusterCenterVector.getArray();
		double[][] node2Array = node2.clusterCenterVector.getArray();
		// m*1的。
		for (int i = 0; i < node1Array.length; i++) {
			// 加速查找。
			// 矩阵的转置，乘法都比double[][]的乘法要慢。
			sum += node1Array[i][0] * node2Array[i][0];
		}
		return sum;
	}

	public static void main(String[] args) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException {
		Initialization initialization = new Initialization();
		MySecretKey mySecretKey = initialization.getMySecretKey();
		HACTreeIndexBuilding hacTreeIndexBuilding = new HACTreeIndexBuilding(mySecretKey, initialization);

		// 在加密文件之前，需要先加密文档、生成辅助索引。
		hacTreeIndexBuilding.encryptFiles();
		hacTreeIndexBuilding.generateAuxiliaryMatrix();

		// test2
		long start = System.currentTimeMillis();
		System.out.println("hac tree index building:");
		hacTreeIndexBuilding.buildHACTreeIndex();
		System.out.println("finish hac tree index building:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println((1 + Math.log(16)) / (1 + Math.log(1)));
		System.out.println(0.156131 / 0.041386);
	}
}
/*
frequency           1         docNumber           2
protesters          TF-IDF              0.00133977

frequency           11        docNumber           1
chemical            TF-IDF              0.00587010

frequency           1         docNumber           8
left                TF-IDF              0.00066989

frequency           1         docNumber           2
5                   TF-IDF              0.00133977

frequency           1         docNumber           1
turning             TF-IDF              0.00172757

frequency           6         docNumber           1
inspectors          TF-IDF              0.00482296

frequency           1         docNumber           4
9                   TF-IDF              0.00098136

frequency           1         docNumber           1
returns             TF-IDF              0.00172757

frequency           1         docNumber           1
wing                TF-IDF              0.00172757

frequency           1         docNumber           2
shortly             TF-IDF              0.00133977

 */