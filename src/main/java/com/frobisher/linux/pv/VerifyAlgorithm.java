package com.frobisher.linux.pv;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.PriorityQueue;

/*
 * author: darcy
 * date: 2017/12/19 20:25
 * description: 
*/
public class VerifyAlgorithm {

	/**
	 * 根据返回结果计算MessageDigest的时候，首先需要根据文档的密文解密得到明文，然后结合密钥来更新目标
	 * targetMessageDigest对象，迭代完返回的所有文档后，更新完targetMessageDigest，然后判定服务器返回的
	 * messageDigest和MessageDigest是否相等。
	 *
	 * @param maxHeap
	 * @param messageDigest
	 * @param requestNumber
	 * @return
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchAlgorithmException
	 */
	public boolean verify(PriorityQueue<HACTreeNode> maxHeap, MessageDigest messageDigest, int requestNumber) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException {
		if (maxHeap.size() != requestNumber) {
			return false;
		} else {
			// 对于返回结果中的每一个Node，计算如下值，Kf代表密钥，Fi代表关联的文档.
			// H(Kf || Fi)
			// 然后判断requestNumber个 H(Kf||Fi)的结果和返回的vo对象是否相等.
			byte[] keyBytes = Initialization.secretKey.getEncoded();
			MessageDigest targetMessageDigest = MessageDigest.getInstance("SHA-256");
			for (HACTreeNode node : maxHeap) {
				String fileName = Initialization.BASE + "\\doc\\muse\\encrypted40\\encrypted_"
						+ node.fileDescriptor.substring(0, node.fileDescriptor.lastIndexOf('.')) + ".dat";
				byte[] encryptedBytes = Files.readAllBytes(new File(fileName).toPath());
				byte[] decrypt = EncryptionUtils.decrypt(encryptedBytes);
				byte[] bytes = new byte[keyBytes.length + decrypt.length];
				System.arraycopy(keyBytes, 0, bytes, 0, keyBytes.length);
				System.arraycopy(decrypt, 0, bytes, keyBytes.length, decrypt.length);
				// bytes中是密钥和明文文档内容的值。
				// 以此更新targetMessageDigest对象.
				targetMessageDigest.update(bytes);
			}
			return Arrays.equals(messageDigest.digest(), targetMessageDigest.digest());
		}
	}
}
