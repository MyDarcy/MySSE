package com.frobisher.linux.lv;

import Jama.Matrix;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
	public Initialization initialization;

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
	public RealDistribution distribution = new UniformRealDistribution(-0.0001, 0.0001);
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
		System.out.println("HACTreeIndexBuilding encryptFiles start.");
		long start = System.currentTimeMillis();
		File parentDir = new File(initialization.PLAIN_DIR);
		if (parentDir.exists()) {
			File[] files = parentDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				Path path = files[i].toPath();
				byte[] bytes = Files.readAllBytes(path);
				// 先缓存加密文件.
				fileBytesMap.put(files[i].getName(), bytes);
				// 获取加密后的bytes.
				byte[] encrypt = EncryptionUtils.encrypt(bytes);

				/*System.out.println(path.getFileName());*/
				String encryptedFileName = initialization.ENCRYPTED_DIR
						+ Initialization.SEPERATOR + "encrypted_" + path.getFileName().toString();
				// 二进制文件的后缀是.dat
				encryptedFileName = encryptedFileName.substring(0, encryptedFileName.lastIndexOf('.')) + ".dat";

				/*System.out.println(encryptedFileName);*/

				Files.write(new File(encryptedFileName).toPath(), encrypt);

				// 显示文件加密后的内容.
				/*byte[] decrypt = EncryptionUtils.decrypt(encrypt);
				String text = new String(decrypt);
				System.out.println(text);
				System.out.println();*/
			}
		}

		System.out.println("total time:" + (System.currentTimeMillis() - start) + "ms");
		System.out.println("HACTreeIndexBuilding encryptFiles finish.");
	}

	/**
	 *  *  HAC-tree中节点u是一个五元组〈VM,PL,PR,FD,sig〉, 其中，u.VM是是一个剪枝向量，u.PL和u.PR分别是指向节点u的左右孩子节点。
	 *  u.FD代表的是文档额唯一的ID。u.sig代表的是u.FD文档的消息摘要。此外，u.VC是聚类C_u的聚类中心向量，u.N表示聚类C_u中文档的数目，
	 *  聚类C_u代表的是以u为根节点的子树中所有的叶子结点代表的文档，同时注意u.VC和u.N仅仅存在于HAC-Tree的构造阶段，
	 *  不需要存储在HAC-Tree中。根据节点u的类型，我们详细的描述HAC-Tree节点如下
	 *      如果u是叶子结点，那么u.PL= u.PR= ϕ , u.FD 存储的是文档的id，u.VM和u.VC都存储的是当前文档的向量，u.N=1, u.sig
	 *  存储的是当前文档的消息摘要，消息摘要主要用于后续的搜搜结果的验证。
	 *      如果u是一个内部的中间节点，那么u.FD= ϕ, u.sig= ϕ, u.PL和u.PR代表节点u的左右孩子节点。u.N= u.PL.N + u.PR.N,
	 *  而u.VM则是从聚类C_u中提取的最大向量。u.VC则是聚类C_u的聚类中心向量。
	 *  u.VM= (max) ⃗{u.PL.VM,u.PR.VM}    (9)
	 *  u.VC=  (u.PL.N × u.PL.VC+u.PR.N+u.PR.VC)/(u.PL.N+u.PR.N)    (10)
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

		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i].getName());

			Matrix P = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);
			P.set(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER - 1, 0, 1);

			// 当前文档的长度.
			int lengthOfFile = initialization.fileLength.get(files[i].getName());

			Map<String, Integer> keywordFrequencyInCurrentDocument =
					initialization.keywordFrequencyInDocument.get(files[i].getName());

			int fileNumbers = initialization.fileLength.size();

			// 安装FMS_II的实现，只需要设置文档向量为0, 1值，即对关键词的包含关系即可。
			for (String key : keywordFrequencyInCurrentDocument.keySet()) {
				int index = initialization.dict.indexOf(key);
				if (index != -1) {
					P.set(index, 0, 1);
				}
			}

			/*MatrixUitls.print(P);*/

			double[] sample = distribution.sample(initialization.DUMMY_KEYWORD_NUMBER);
			for (int j = 0; j < initialization.DUMMY_KEYWORD_NUMBER; j++) {
				String str = initialization.extendDummyDict.get(j);
				int index = initialization.dict.indexOf(str);
				// System.out.printf("%-20s%-8d%-20s%.8f\n", "index", index, str, sample[j]);
				if (index != -1) {
					P.set(index, 0, sample[j]);
				}
			}

			/*for (int j = 0; j < (Initialization.DUMMY_KEYWORD_NUMBER); j++) {
				P.set(Initialization.DICTIONARY_SIZE + j, 0, sample[j]);
			}

			System.out.println("P extend part show as follow.");
			double sum = 0;
			for (int j = 0; j < Initialization.DUMMY_KEYWORD_NUMBER; j++) {
				sum += P.get(Initialization.DICTIONARY_SIZE + j, 0);
				System.out.print(P.get(Initialization.DICTIONARY_SIZE + j, 0) + "\t");
			}
			System.out.println("\ndistrubtion elements sum:" + sum);
			System.out.println("\n");*/

			// 获取可逆矩阵加密后的Matrix.
			Matrix pa = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);
			Matrix pb = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);

			for (int j = 0; j < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; j++) {
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

			/*
			MatrixUitls.print(pa);
			MatrixUitls.print(pb);
			System.out.println();
			*/

			// 获取消息摘要.
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] keyBytes = mySecretKey.secretKey.getEncoded();

			byte[] fileBytes = fileBytesMap.get(files[i].getName());
			byte[] bytes = new byte[keyBytes.length + fileBytes.length];
			System.arraycopy(keyBytes, 0, bytes, 0, keyBytes.length);
			System.arraycopy(fileBytes, 0, bytes, keyBytes.length, fileBytes.length);
			messageDigest.update(bytes);

			Matrix paEncrypted = AuxiliaryMatrix.M1Transpose.times(pa);
			Matrix pbEncrypted = AuxiliaryMatrix.M2Transpose.times(pb);

			// 存疑, 中心向量向上构造的过程中.最开始的中心向量的选择，因为原来的
			// 剪枝向量已经用两个转置矩阵加密了。
			HACTreeNode currentNode = new HACTreeNode(paEncrypted, pbEncrypted, P, 1,
					null, null, files[i].getName(), messageDigest);

			/*HACTreeNode currentNode = new HACTreeNode(P, P, 1,
					null, null, files[i].getName(), files[i].getName());*/
			/*System.out.println(currentNode);*/

			currentProcessingHACTreeNodeSet.add(currentNode);
		}

		/**
		 * 到这里已经加密了一轮文档,
		 */

		System.out.println("start construct hac-tree.");
		int round = 1;
		while (currentProcessingHACTreeNodeSet.size() > 1) {
			System.out.println("the " + (round++) + "'s round to build tree.");

			PriorityQueue<HacTreeNodePairScore> maxHeap = getPriorityQueue(currentProcessingHACTreeNodeSet);
			Set<HACTreeNode> managedNodeSet = new HashSet<>();

			while (currentProcessingHACTreeNodeSet.size() > 1) {
				// HACTreeNodePair mostCorrespondNodePair = findMostCorrespondNodePair(currentProcessingHACTreeNodeSet);

				HacTreeNodePairScore mostSimilarNodePair = maxHeap.poll();
				// 最相关的两个节点有节点是已经处理过了。
				if (managedNodeSet.contains(mostSimilarNodePair.node1)
						|| managedNodeSet.contains(mostSimilarNodePair.node2)) {
					continue;
				}

				HACTreeNodePair mostCorrespondNodePair = new HACTreeNodePair(mostSimilarNodePair.node1,
						mostSimilarNodePair.node2);

				List<Matrix> parentNodePruningVectors = getParentNodePruningVector(mostCorrespondNodePair);

				/*MatrixUitls.print(parentNodePruningVectors.get(0));
				MatrixUitls.print(parentNodePruningVectors.get(1));*/

				Matrix parentNodeCenterVector = getParentNodeCenterVector(mostCorrespondNodePair);
				int parentNumberOfNodeInCurrentCluster = mostCorrespondNodePair.node1.numberOfNodeInCurrentCluster
						+ mostCorrespondNodePair.node2.numberOfNodeInCurrentCluster;
				// 存疑，这样构造出来的剪枝向量有效吗？
				HACTreeNode parentNode = new HACTreeNode(parentNodePruningVectors.get(0), parentNodePruningVectors.get(1),
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
				maxHeap.add(new HacTreeNodePairScore(list.get(i), list.get(j),
						correspondingScore(list.get(i), list.get(j))));
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
	 * @param nodePair
	 * @return
	 */
	private Matrix getParentNodeCenterVector(HACTreeNodePair nodePair) {
		int newNumberOfNode = nodePair.node1.numberOfNodeInCurrentCluster + nodePair.node2.numberOfNodeInCurrentCluster;
		Matrix parentCenterVector = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);
		for (int i = 0; i < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; i++) {
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
		Matrix parent1 = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);
		Matrix parent2 = new Matrix(initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER, 1);
		for (int i = 0; i < initialization.DICTIONARY_SIZE + initialization.DUMMY_KEYWORD_NUMBER; i++) {
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

	/**
	 *
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
