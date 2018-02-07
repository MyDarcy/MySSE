package com.frobisher.linux.accelerate.pv;

import com.frobisher.linux.accelerate.DiagonalMatrixUtils;
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

	public static class HacTreeNodePairScore {
		HACTreeNode node1;
		HACTreeNode node2;
		double score;

		public HacTreeNodePairScore(HACTreeNode node1, HACTreeNode node2, double score) {
			this.node1 = node1;
			this.node2 = node2;
			this.score = score;
		}
	}

	// 秘密钥
	public MySecretKey mySecretKey;
	public Map<Integer, byte[]> fileBytesMap = new HashMap<>();
	public Comparator<HacTreeNodePairScore> maxComparator;
	public Initialization initialization;

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

		AuxiliaryMatrix.M1Transpose = DiagonalMatrixUtils.transpose(mySecretKey.M1);
		AuxiliaryMatrix.M2Transpose = DiagonalMatrixUtils.transpose(mySecretKey.M2);
		System.out.println("two transpose:" + (System.currentTimeMillis() - start) + "ms");

		start = System.currentTimeMillis();
		AuxiliaryMatrix.M1Inverse = DiagonalMatrixUtils.inverse(mySecretKey.M1);
		AuxiliaryMatrix.M2Inverse = DiagonalMatrixUtils.inverse(mySecretKey.M2);
		System.out.println("two inverse:" + (System.currentTimeMillis() - start) + "ms");

		System.out.println("total time:" + (System.currentTimeMillis() - nstart) + "ms");
		System.out.println("HACTreeIndexBuilding generateAuxiliaryMatrix finished.");
	}

	/**
	 * 加密文档，
	 * 同时生成name -> fileBytes的映射, 因为要生成消息摘要的使用要用到文档的内容，
	 * 为了避免两次IO读取操作，所以牺牲了内存的性能。
	 *
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 */
	public void encryptFiles() throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
		System.out.println("HACTreeIndexBuildingSimulation encryptFiles start.");
		long start = System.currentTimeMillis();
		for (int i = 0; i < initialization.simulationDocuments.size(); i++) {
			double[]  array = initialization.simulationDocuments.get(i);
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < initialization.simulationDictSize; j++) {
				if (Double.compare(0, array[j]) != 0) {
					sb.append(j + "\t");
				}
			}
			byte[] bytes = sb.toString().trim().getBytes();
			fileBytesMap.put(i, bytes);
		}
		System.out.println("HACTreeIndexBuildingSimulation encryptFiles end.");
	}

	public HACTreeNode buildHACTreeIndex() throws NoSuchAlgorithmException {
		System.out.println("HACTreeIndexBuildingSimulation buildHACTreeIndex start.");
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

//		PriorityQueue<Double> tfIdfMinHeap = new PriorityQueue<>(20, Double::compare);
//		PriorityQueue<Double> tfIdfMaxHeap = new PriorityQueue<>(20, Comparator.reverseOrder());
		for (int i = 0; i < initialization.simulationDocumentNumber; i++) {
			// System.out.println(files[i].getName());
			double[] P = initialization.simulationDocuments.get(i);
			double[] sample = distribution.sample(initialization.simulationDummykeywordIndexSet.size());
			int indexCount = 0;
			for (int index : initialization.simulationDummykeywordIndexSet) {
				P[index] = sample[indexCount++];
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

				double[] parentNodePruningVector = getParentNodePruningVectorByOne(mostCorrespondNodePair);
				double[] parentNodeCenterVector = getParentNodeCenterVector(mostCorrespondNodePair);
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

	private double[] getParentNodePruningVectorByOne(HACTreeNodePair pair) {
		double[] parent = new double[initialization.simulationDictSize];
		for (int i = 0; i < initialization.simulationDictSize; i++) {
			parent[i] = Double.max(pair.node1.pruningVector[i], pair.node2.pruningVector[i]);
		}
		return parent;
	}

	/**
	 * 加密root为根节点的树.
	 * @param root
	 */
	public void encryptHACTreeIndex(HACTreeNode root) {
		if (root == null) {
			return;
		}

		double[] pa = new double[initialization.simulationDictSize];
		double[] pb = new double[initialization.simulationDictSize];
		// 剪枝向量
		double[] P = root.pruningVector;
		/**
		 * S[i] = 0, pa[i] + pb[i] = P[i]
		 * S[i] = 1, pa[i] = pb[i] = P[i]
		 */
		for (int j = 0; j < initialization.simulationDictSize; j++) {
			// 置0相加
			if (!mySecretKey.S.get(j)) {
				double rand = random.nextDouble();
				pa[j] = P[j] * rand;
				pb[j] = P[j] * (1 - rand);

				// 置1相等
			} else {
				pa[j] = P[j];
				pb[j] = P[j];
			}
		}

		// 加密剪枝子向量.
		double[] paEncrypted = DiagonalMatrixUtils.times(AuxiliaryMatrix.M1Transpose, pa);
		double[] pbEncrypted = DiagonalMatrixUtils.times(AuxiliaryMatrix.M2Transpose, pb);
//		Arrays.toString(paEncrypted);
//		Arrays.toString(pbEncrypted);

		root.pruningVectorPart1 = paEncrypted;
		root.pruningVectorPart2 = pbEncrypted;
		// 因为加密后的密文树需要上传, 所以这些临时值就要置为 null or 0.
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
//		System.out.println("time: " + (System.currentTimeMillis() - start) + " ms");
//		System.out.println("getPriorityQueue end.");
		return maxHeap;
	}

	/**
	 * 获取两个聚类的中心向量.
	 * @param nodePair
	 * @return
	 */
	private double[] getParentNodeCenterVector(HACTreeNodePair nodePair) {
		int newNumberOfNode = nodePair.node1.numberOfNodeInCurrentCluster + nodePair.node2.numberOfNodeInCurrentCluster;
		double[] parentCenterVector = new double[initialization.simulationDictSize];
		for (int i = 0; i < initialization.simulationDictSize; i++) {
			double sum = nodePair.node1.clusterCenterVector[i] * nodePair.node1.numberOfNodeInCurrentCluster
					+ nodePair.node2.clusterCenterVector[i] * nodePair.node2.numberOfNodeInCurrentCluster;
			parentCenterVector[i] = sum / newNumberOfNode;
		}
		return parentCenterVector;
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
		double result  = DiagonalMatrixUtils.score(node1.clusterCenterVector, node2.clusterCenterVector);
		return result;
		/*System.out.println(matrix.getRowDimension() + "\t" + matrix.getColumnDimension());*/
	}

	/**
	 * 求tf-idf的分值。
	 * @param lengthOfFile 文件i的长度.
	 * @param frequency 当前关键词在文档i中出现的频率.
	 * @param numberOfDocumentContainsKeyword 有多少个文档包含关键词.
	 * @param filesNumber 总的文档的数目.
	 * @return
	 */
	private double score(int lengthOfFile, Integer frequency, Integer numberOfDocumentContainsKeyword,int filesNumber) {
		return ((1 + Math.log(frequency)) / lengthOfFile)
				* Math.log(1 + filesNumber / numberOfDocumentContainsKeyword);
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