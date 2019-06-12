package com.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IOHandler {

    public final static byte OK = (byte) 0x0;
    public final static byte ERROR = (byte) 0x1;

    public static String readByteLen(ByteBuf buf) {
        short length = buf.readByte();
        String str = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), length), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + length);
        return str;
    }

    public static String readShortLen(ByteBuf buf) {
        int length = buf.readShort();
        String str = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), length), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + length);
        return str;
    }


    public static String readIntLen(ByteBuf buf) {
        int length = buf.readInt();
        String str = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), length), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + length);
        return str;
    }


    public static int[] readInt(int size, ByteBuf buf) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = buf.readInt();
        }
        return arr;
    }

    public static byte[] readBytes(int size, ByteBuf buf) {
        byte[] bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), size);
        buf.readerIndex(buf.readerIndex() + size);
        return bytes;
    }

    public static String[] readIntLen(int size, ByteBuf buf) {
        String[] arr = new String[size];
        for (int i = 0; i < size; i++) {
            arr[i] = readIntLen(buf);
        }
        return arr;
    }

    public static String[] readShortLen(int size, ByteBuf buf) {
        String[] arr = new String[size];
        for (int i = 0; i < size; i++) {
            arr[i] = readShortLen(buf);
        }
        return arr;
    }

    public static ByteBuf writeByte(byte cmd) {
        ByteBuf buf = Unpooled.buffer(1);
        buf.writeByte(cmd);
        return buf;
    }

    public static ByteBuf writeShort(int num) {
        ByteBuf buf = Unpooled.buffer(2);
        buf.writeShort(num);
        return buf;
    }

    public static ByteBuf writeShortStr(byte cmd, boolean bool) {
        return writeShortStr(cmd, bool ? "true" : "false");
    }

    public static ByteBuf writeShortStr(byte cmd, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(3 + bytes.length);
        buf.writeByte(cmd);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
        return buf;
    }

    public static ByteBuf writeInt(byte cmd, int code) {
        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte(cmd);
        buf.writeInt(code);
        return buf;
    }

    public static ByteBuf writeInt(byte cmd, int[] code) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(cmd);
        buf.writeShort(code.length);
        for (int value : code) {
            buf.writeInt(value);
        }
        return buf;
    }

    /**
     * length -1 represent str is null
     */
    public static void writeShortString(String str, ByteBuf buf) {
        if (str == null) buf.writeShort(~0);
        else {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            buf.writeShort(bytes.length);
            buf.writeBytes(bytes);
        }
    }

    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


}
