package com.scl.mgr.data

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Password-based AES-256-GCM encryption for Google Drive backups. */
object BackupCrypto {
    const val MAGIC = "SMDBENC1"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 600_000
    private val random = SecureRandom()

    fun isEncrypted(file: File): Boolean {
        if (!file.isFile || file.length() < MAGIC.length) return false
        return try {
            FileInputStream(file).use { input ->
                val magic = ByteArray(MAGIC.length)
                input.readFully(magic)
                String(magic, Charsets.US_ASCII) == MAGIC
            }
        } catch (_: Exception) {
            false
        }
    }

    fun encrypt(input: File, output: File, password: CharArray) {
        validatePassword(password)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, deriveKey(password, salt), iv)

        output.parentFile?.mkdirs()
        if (output.exists()) output.delete()
        DataOutputStream(FileOutputStream(output)).use { raw ->
            raw.write(MAGIC.toByteArray(Charsets.US_ASCII))
            raw.write(salt)
            raw.write(iv)
            CipherOutputStream(raw, cipher).use { encrypted ->
                FileInputStream(input).use { source -> source.copyTo(encrypted) }
            }
        }
    }

    fun decrypt(input: File, output: File, password: CharArray) {
        validatePassword(password)
        try {
            DataInputStream(FileInputStream(input)).use { raw ->
                val magic = ByteArray(MAGIC.length)
                raw.readFully(magic)
                if (String(magic, Charsets.US_ASCII) != MAGIC) {
                    throw InvalidBackupException("This backup is not a School Manager encrypted backup.")
                }
                val salt = ByteArray(SALT_BYTES).also(raw::readFully)
                val iv = ByteArray(IV_BYTES).also(raw::readFully)
                val cipher = cipher(Cipher.DECRYPT_MODE, deriveKey(password, salt), iv)

                output.parentFile?.mkdirs()
                if (output.exists()) output.delete()
                CipherInputStream(raw, cipher).use { encrypted ->
                    FileOutputStream(output).use { destination -> encrypted.copyTo(destination) }
                }
            }
        } catch (e: InvalidBackupException) {
            output.delete()
            throw e
        } catch (e: Exception) {
            output.delete()
            throw InvalidBackupException("Backup password is incorrect or the backup is damaged.", e)
        }
    }

    private fun validatePassword(password: CharArray) {
        if (password.size < 8) {
            throw InvalidBackupException("Backup password must be at least 8 characters.")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun cipher(mode: Int, key: SecretKey, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }

    private fun java.io.InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val count = read(buffer, offset, buffer.size - offset)
            if (count < 0) throw InvalidBackupException("Encrypted backup is incomplete.")
            offset += count
        }
    }

    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
