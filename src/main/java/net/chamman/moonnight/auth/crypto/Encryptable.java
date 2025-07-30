package net.chamman.moonnight.auth.crypto;

public interface Encryptable<T extends Encryptable<T>> {
	T encrypt(AesProvider aesProvider);
	T decrypt(AesProvider aesProvider);
}
