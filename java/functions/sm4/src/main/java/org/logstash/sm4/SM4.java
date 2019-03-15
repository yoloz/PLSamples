package org.logstash.sm4;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SM4 {

    private final SM4Impl sm4Impl = new SM4Impl();

    private final String key;   //密钥key长度16或32或大于32
    private final String iv;    //初始化向量,cbc方式需要,长度16或32或大于32
    private final int mode;    //加密1,解密0
    private final SM4Context ctx;

    private byte[] ivBytes;

    public SM4(String key, String iv, int mode) {
        this(key, iv, mode, true);
    }

    SM4(String key, String iv, int mode, boolean padding) {
        this.key = key;
        this.iv = iv;
        this.mode = mode;
        this.ctx = new SM4Context(mode, padding);
        this.init();
    }

    public String process(String text) throws IOException {
        if (iv == null || iv.isEmpty()) {
            if (mode == SM4Impl.ENCRYPT)
                return encrypt_ecb(text);
            else return decrypt_ecb(text);
        } else {
            if (mode == SM4Impl.ENCRYPT) return encrypt_cbc(text);
            else return decrypt_cbc(text);
        }
    }

    private void init() {
        int kl = key.getBytes(StandardCharsets.UTF_8).length;
        byte[] keyBytes;
        if (kl > 16) keyBytes = this.hexStringToBytes(key);
        else keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (mode == SM4Impl.ENCRYPT) sm4Impl.sm4_setkey(ctx.getSk(), keyBytes);
        else {
            int i;
            sm4Impl.sm4_setkey(ctx.getSk(), keyBytes);
            for (i = 0; i < 16; i++) sm4Impl.SWAP(ctx.getSk(), i);
        }

        int ivl = iv.getBytes(StandardCharsets.UTF_8).length;
        if (ivl > 16) {
            ivBytes = this.hexStringToBytes(iv);
        } else {
            ivBytes = iv.getBytes(StandardCharsets.UTF_8);
        }
    }

    private String encrypt_ecb(String plainText) throws IOException {
        if (plainText == null || plainText.isEmpty()) return "";
        byte[] encrypted = sm4Impl.sm4_crypt_ecb(ctx, plainText.getBytes(StandardCharsets.UTF_8));
        String cipherText = Base64.getEncoder().encodeToString(encrypted);
        if (cipherText != null && cipherText.trim().length() > 0) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(cipherText);
            cipherText = m.replaceAll("");
        }
        return cipherText;
    }

    private String decrypt_ecb(String cipherText) throws IOException {
        byte[] decrypted = sm4Impl.sm4_crypt_ecb(ctx, Base64.getDecoder().decode(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String encrypt_cbc(String plainText) throws IOException {
        byte[] encrypted = sm4Impl.sm4_crypt_cbc(ctx, ivBytes, plainText.getBytes(StandardCharsets.UTF_8));
        String cipherText = Base64.getEncoder().encodeToString(encrypted);
        if (cipherText != null && cipherText.trim().length() > 0) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(cipherText);
            cipherText = m.replaceAll("");
        }
        return cipherText;
    }

    private String decrypt_cbc(String cipherText) throws IOException {
        byte[] decrypted = sm4Impl.sm4_crypt_cbc(ctx, ivBytes, Base64.getDecoder().decode((cipherText)));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Convert hex string to byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) return null;
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}