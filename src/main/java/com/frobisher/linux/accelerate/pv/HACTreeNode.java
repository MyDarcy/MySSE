package com.frobisher.linux.accelerate.pv;

import java.security.MessageDigest;

/*
 * author: darcy
 * date: 2017/12/18 21:58
 * description:
 *
 *  Node u of HAC-tree is a fve-element tuple <VM, PL, PR, FD, sig>, where u.VM
 *  is the pruning vector, u.PL and u.PR, respectively, point to the lef and right
 *  child nodes of u.  u.FD stores the unique identifer of a document, and u.sig stores
 *  a digest of the u.FD document.
 *
 *  HAC-tree中节点u是一个五元组〈VM,PL,PR,FD,sig〉, 其中，u.VM是是一个剪枝向量，u.PL和u.PR分别是指向节点u的左右孩子节点。
 *  u.FD代表的是文档额唯一的ID。u.sig代表的是u.FD文档的消息摘要。此外，u.VC是聚类C_u的聚类中心向量，u.N表示聚类C_u中文档的数目，
 *  聚类C_u代表的是以u为根节点的子树中所有的叶子结点代表的文档，同时注意u.VC和u.N仅仅存在于HAC-Tree的构造阶段，
 *  不需要存储在HAC-Tree中。根据节点u的类型，我们详细的描述HAC-Tree节点如下
 *      如果u是叶子结点，那么u.PL= u.PR= ϕ , u.FD 存储的是文档的id，u.VM和u.VC都存储的是当前文档的向量，u.N=1, u.sig
 *  存储的是当前文档的消息摘要，消息摘要主要用于后续的搜搜结果的验证。
 *      如果u是一个内部的中间节点，那么u.FD= ϕ, u.sig= ϕ, u.PL和u.PR代表节点u的左右孩子节点。u.N= u.PL.N + u.PR.N,
 *  而u.VM则是从聚类C_u中提取的最大向量。u.VC则是聚类C_u的聚类中心向量。
 *  u.VM= (max) ⃗{u.PL.VM,u.PR.VM}    (9)
 *  u.VC=  (u.PL.N × u.PL.VC+u.PR.N+u.PR.VC)/(u.PL.N+u.PR.N)    (10)

*/
public class HACTreeNode {

	/*// 剪枝向量.
	public Matrix pruningVector;*/

	// 剪枝向量
	// 因为需要对剪枝向量通过可逆矩阵来加密。
	public double[] pruningVectorPart1;
	public double[] pruningVectorPart2;

	// 聚类中心向量
	public double[] clusterCenterVector;
	public int numberOfNodeInCurrentCluster;

	// 左子树， 右子树的指针.
	public HACTreeNode left;
	public HACTreeNode right;

	// 文档的唯一标志符
	// 可以是预先通过文件名生成消息摘要然后BASE32编码来编码一遍得到新的文件名，
	// 避免说文件名泄露了信息, 这样便于处理。
	public String fileDescriptor;

	// 对文档内容签名的值，以用于验证.
	// 字符串的signature转为消息摘要的形式.
	/*public String signature;*/
	public MessageDigest digest;



	public HACTreeNode() {
	}

	public HACTreeNode(double[] pruningVectorPart1, double[] pruningVectorPart2, HACTreeNode left, HACTreeNode right, String fileDescriptor, MessageDigest signature) {
		this.pruningVectorPart1 = pruningVectorPart1;
		this.pruningVectorPart2 = pruningVectorPart2;
		this.left = left;
		this.right = right;
		this.fileDescriptor = fileDescriptor;
		this.digest = signature;
	}

	public HACTreeNode(double[] pruningVectorPart1, double[] pruningVectorPart2, double[] clusterCenterVector, int numberOfNodeInCurrentCluster, HACTreeNode left, HACTreeNode right, String fileDescriptor, MessageDigest signature) {
		this.pruningVectorPart1 = pruningVectorPart1;
		this.pruningVectorPart2 = pruningVectorPart2;
		this.clusterCenterVector = clusterCenterVector;
		this.numberOfNodeInCurrentCluster = numberOfNodeInCurrentCluster;
		this.left = left;
		this.right = right;
		this.fileDescriptor = fileDescriptor;
		this.digest = signature;
	}

/*	@Override
	public String toString() {
		int pruningVectorLength = Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER;
		int clusterCenterVectorLength = Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER;
		if (clusterCenterVector == null) {
			clusterCenterVectorLength = 0;
		}
		return "HACTreeNode{" +
				"pruningVectorPart1.length=" + pruningVectorLength +
				", pruningVectorPart2.length=" + pruningVectorLength +
				", clusterCenterVector.length=" + clusterCenterVectorLength +
				", numberOfNodeInCurrentCluster=" + numberOfNodeInCurrentCluster +
				", left=" + left +
				", right=" + right +
				", fileDescriptor='" + fileDescriptor + '\'' +
				", digest=" + digest +
				'}';
	}*/

	/*public HACTreeNode(Matrix pruningVector, HACTreeNode left, HACTreeNode right, String fileDescriptor, String signature) {
		this.pruningVector = pruningVector;
		this.left = left;
		this.right = right;
		this.fileDescriptor = fileDescriptor;
		this.signature = signature;
	}

	public HACTreeNode(Matrix pruningVector, Matrix clusterCenterVector, int numberOfNodeInCurrentCluster, HACTreeNode left, HACTreeNode right, String fileDescriptor, String signature) {
		this.pruningVector = pruningVector;
		this.clusterCenterVector = clusterCenterVector;
		this.numberOfNodeInCurrentCluster = numberOfNodeInCurrentCluster;
		this.left = left;
		this.right = right;
		this.fileDescriptor = fileDescriptor;
		this.signature = signature;
	}*/

	/*@Override
	public String toString() {
		int pruningVectorLength = Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER + 1;
		int clusterCenterVectorLength = Initialization.DICTIONARY_SIZE + Initialization.DUMMY_KEYWORD_NUMBER + 1;
		if (clusterCenterVector == null) {
			clusterCenterVectorLength = 0;
		}
		return "HACTreeNode{" +
				"pruningVector.length=" + pruningVectorLength +
				", clusterCenterVector.length=" + clusterCenterVectorLength +
				", numberOfNodeInCurrentCluster=" + numberOfNodeInCurrentCluster +
				", left=" + left +
				", right=" + right +
				", fileDescriptor='" + fileDescriptor + '\'' +
				", signature='" + signature + '\'' +
				'}';
	}*/
}

/*
D:\MrDarcy\ForGraduationWorks\Code\SSE\doc\muse\encrypted40\encrypted_00083697263e215e5e7eda753070f08aa374dd45.dat
encrypted_00083697263e215e5e7eda753070f08aa374dd45
 */