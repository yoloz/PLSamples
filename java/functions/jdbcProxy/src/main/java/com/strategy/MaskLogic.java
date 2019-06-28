package com.strategy;

import com.handler.IOHandler;
import com.util.InnerDb;
import org.apache.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;

public class MaskLogic {

    private static final Logger logger = Logger.getLogger(MaskLogic.class);

    public static Map<String, Map<String, Object>> getMaskPolicy(String ak, String username, String dbname,
                                                                 String tablename) throws SQLException {
        String sql = "select m.colname,r.name,r.type,r.srctype,m.param " +
                "from data_mask_temp m, data_maskrule_temp r " +
                "where ak=? and username=? and dbname=? and tablename=? and m.ruleid = r.id";
//        logger.info(sql + "==" + ak + "==" + username + "==" + dbname + "==" + tablename);
        List<Map<String, Object>> list = InnerDb.query(sql, ak, username, dbname, tablename);
        Map<String, Map<String, Object>> mmap = new HashMap<>(list.size());
        for (Map<String, Object> map : list) {
            String col = String.valueOf(map.remove("colname"));
            mmap.put(col, map);
        }
//        for (String s : mmap.keySet()) {
//            logger.info(s + "==>");
//            mmap.get(s).forEach((k, v) -> logger.info(k + "===" + v));
//        }
        return mmap;
    }

    private static byte[] aes(byte[] content, String password, int mode) {
        try {
            if (Cipher.DECRYPT_MODE == mode) content = IOHandler.hexToByte(new String(content,
                    StandardCharsets.UTF_8));
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8));
            kgen.init(128, secureRandom);
            SecretKey secretKey = kgen.generateKey();
            SecretKeySpec key = new SecretKeySpec(secretKey.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(mode, key);
            byte[] bytes = cipher.doFinal(content);
            if (Cipher.ENCRYPT_MODE == mode) bytes = IOHandler.byteToHex(bytes).getBytes(StandardCharsets.UTF_8);
            return bytes;
        } catch (Exception e) {
            logger.error(e);
        }
        return content;
    }

    public static byte[] decrypt(byte[] bytes, Map<String, Object> mask_policy) {
        if (mask_policy == null || mask_policy.isEmpty()) return bytes;
        String mask_name = String.valueOf(mask_policy.get("name"));
        String srcType = String.valueOf(mask_policy.get("srctype"));
        String param = String.valueOf(mask_policy.get("parma"));
        if (srcType.equalsIgnoreCase("STRING")) {
            if ("STRING_ENCRY_AES".equalsIgnoreCase(mask_name))
                return aes(bytes, param, Cipher.DECRYPT_MODE);
        }
        return bytes;
    }

    public static byte[] encrypt(byte[] bytes, Map<String, Object> mask_policy) {
        if (mask_policy == null || mask_policy.isEmpty()) return bytes;
        String mask_name = String.valueOf(mask_policy.get("name"));
        String srcType = String.valueOf(mask_policy.get("srctype"));
        String param = String.valueOf(mask_policy.get("parma"));
        if (srcType.equalsIgnoreCase("STRING")) {
            if ("STRING_ENCRY_AES".equalsIgnoreCase(mask_name))
                return aes(bytes, param, Cipher.ENCRYPT_MODE);
        }
        return bytes;
    }


    public static byte[] getMaskResult(byte[] bytes, Map<String, Object> mask_policy) {
        if (mask_policy == null || mask_policy.isEmpty()) return bytes;
        String mask_name = String.valueOf(mask_policy.get("name"));
        String srcType = String.valueOf(mask_policy.get("srctype"));
        String param = String.valueOf(mask_policy.get("parma"));
        if (srcType.equalsIgnoreCase("STRING")) {
            return getStringMask(bytes, mask_name, param);
        } else if (srcType.equalsIgnoreCase("TINYINT")
                || srcType.equalsIgnoreCase("SMALLINT")
                || srcType.equalsIgnoreCase("INT")
                || srcType.equalsIgnoreCase("BIGINT")
                || srcType.equalsIgnoreCase("FLOAT")
                || srcType.equalsIgnoreCase("DOUBLE")
                || srcType.equalsIgnoreCase("DECIMAL")) {
            return getNumberMask(bytes, mask_name);
        } else if (srcType.equalsIgnoreCase("DATE")) {
            return getDateMask(bytes, mask_name);
        } else if (srcType.equalsIgnoreCase("TIMESTAMP")) {
            return getTimeStampMask(bytes, mask_name);
        } else if (srcType.equalsIgnoreCase("BOOLEAN")) {
            return getBooleanMask(bytes, mask_name);
        } else if (srcType.equalsIgnoreCase("USERUDF")) {
            return getUserUDF(bytes, mask_name);
        }
        return bytes;
    }

    private static byte[] getStringMask(byte[] bytes, String mask_name, String param) {
        if (bytes == null) return null;
        String value = new String(bytes, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        if ("STRING_X".equalsIgnoreCase(mask_name)) {
            for (int i = 0; i < value.length(); i++) sb.append("*");
        } else if ("STRING_ZJHM".equalsIgnoreCase(mask_name)) {
            sb.append(value, 0, 4).append("**************");
        } else if ("STRING_NAME".equalsIgnoreCase(mask_name)) {
            if (value.length() < 3) sb.append(value, 0, 1).append("*");
            else {
                sb.append(value, 0, 1);
                for (int i = 0; i < value.length() - 2; i++) sb.append("*");
                sb.substring(value.length() - 1);
            }
        } else if ("STRING_MAIL".equalsIgnoreCase(mask_name)) {
            int atPosition = value.indexOf("@");
            if (atPosition == -1) return bytes;
            for (int i = 0; i < atPosition; i++) sb.append("*");
            sb.append(value.substring(atPosition));
        } else if ("STRING_FULLMAIL".equalsIgnoreCase(mask_name)) {
            int atPosition = value.indexOf("@");
            if (atPosition == -1) return bytes;
            sb.append(value, 0, 1);
            for (int i = 0; i < atPosition - 1; i++) sb.append("*");
            sb.append(value.substring(atPosition));
        } else if ("STRING_MOBILE".equalsIgnoreCase(mask_name)) {
            sb.append(value, 0, 3);
            for (int i = 0; i < value.length() - 5; i++) sb.append("*");
            sb.append(value.substring(value.length() - 2));
        } else if ("STRING_8X".equalsIgnoreCase(mask_name)) {
            sb.append("********");
        } else if ("STRING_HEAD_TAIL".equalsIgnoreCase(mask_name)) {
            sb.append("*").append(value, 1, value.length() - 1).append("*");
        } else if ("STRING_HEAD".equalsIgnoreCase(mask_name)) {
            sb.append("*").append(value.substring(1));
        } else if ("STRING_TAIL".equalsIgnoreCase(mask_name)) {
            sb.append(value, 0, value.length() - 1).append("*");
        } else if ("STRING_MIDDLE".equalsIgnoreCase(mask_name)) {
            sb.append(value, 0, 1);
            for (int i = 0; i < value.length() - 2; i++) sb.append("*");
            sb.append(value.substring(value.length() - 1));
        } else if ("STRING_BANK".equalsIgnoreCase(mask_name)) {
            for (int i = 0; i < value.length() - 4; i++) sb.append("*");
            sb.append(value.substring(value.length() - 4));
        } else if ("STRING_DATEYEAR".equalsIgnoreCase(mask_name)) {
            sb.append(value, 0, 4);
        } else if ("STRING_ENCRY_AES".equalsIgnoreCase(mask_name)) {
            return aes(bytes, param, Cipher.ENCRYPT_MODE);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getNumberMask(byte[] b, String mask_name) {
        if (b == null) return null;
        if ("TINYINT_0".equalsIgnoreCase(mask_name) || "SMALLINT_0".equalsIgnoreCase(mask_name)
                || "INT_0".equalsIgnoreCase(mask_name) || "BIGINT_0".equalsIgnoreCase(mask_name)
                || "FLOAT_0".equalsIgnoreCase(mask_name) || "DOUBLE_0".equalsIgnoreCase(mask_name)
                || "DECIMAL_0".equalsIgnoreCase(mask_name)) {
            return "0".getBytes(StandardCharsets.UTF_8);
        } else if ("TINYINT_RANDOM".equalsIgnoreCase(mask_name) || "SMALLINT_RANDOM".equalsIgnoreCase(mask_name)
                || "INT_RANDOM".equalsIgnoreCase(mask_name) || "BIGINT_RANDOM".equalsIgnoreCase(mask_name)
                || "FLOAT_RANDOM".equalsIgnoreCase(mask_name) || "DOUBLE_RANDOM".equalsIgnoreCase(mask_name)
                || "DECIMAL_RANDOM".equalsIgnoreCase(mask_name)) {
            return String.valueOf(new Random().nextInt(10)).getBytes(StandardCharsets.UTF_8);
        }
        return b;
    }

    private static byte[] getDateMask(byte[] b, String mask_name) {
        // TODO Auto-generated method stub
        return null;
    }

    private static byte[] getTimeStampMask(byte[] b, String mask_name) {
        // TODO Auto-generated method stub
        return null;
    }

    private static byte[] getBooleanMask(byte[] b, String mask_name) {
        if (b == null) return null;
        if ("BOOLEAN_FALSE".equalsIgnoreCase(mask_name)) {
            return "false".getBytes(StandardCharsets.UTF_8);
        } else if ("BOOLEAN_RANDOM".equalsIgnoreCase(mask_name)) {
            return (new Random().nextInt(2) == 1 ? "true" : "false").getBytes(StandardCharsets.UTF_8);
        }
        return b;
    }

    private static byte[] getUserUDF(byte[] b, String mask_name) {
        // TODO Auto-generated method stub
        return null;
    }


}
