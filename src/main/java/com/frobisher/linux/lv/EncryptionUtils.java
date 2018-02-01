package com.frobisher.linux.lv;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;

/*
 * author: darcy
 * date: 2017/11/8 15:59
 * description: 
*/
public class EncryptionUtils {

	/**
	 * 加密字符数组.
	 * @param toBeEncrypted
	 * @return
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	public static byte[] encrypt(byte[] toBeEncrypted) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
		Initialization.cipher.init(Cipher.ENCRYPT_MODE, Initialization.secretKey);
		byte[] encryptedBytes = Initialization.cipher.doFinal(toBeEncrypted);
		return encryptedBytes;
	}

	/**
	 * 解密字节数代表的密文数组.
	 * @param toBeDecrypted
	 * @return
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	public static byte[] decrypt(byte[] toBeDecrypted) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
		Initialization.cipher.init(Cipher.DECRYPT_MODE, Initialization.secretKey);
		byte[] decrytedBytes = Initialization.cipher.doFinal(toBeDecrypted);
		return decrytedBytes;
	}

	public static void main(String[] args) throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
		String message = "Searchable Encryption.";
		byte[] encrypt = encrypt(message.getBytes());
		byte[] decrypt = decrypt(encrypt);
		System.out.println(new String(decrypt));
	}

}
