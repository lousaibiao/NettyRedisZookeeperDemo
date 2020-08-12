package com.lou.demo3;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 *
 */
public class ReadCharacterApplication {
    /**
     * 读汉字，utf-8解码
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final Charset utf8 = StandardCharsets.UTF_8;

        final Path filePath = Paths.get(System.getProperty("user.dir"), "1.txt");
        try (RandomAccessFile file = new RandomAccessFile(filePath.toString(), "rw")) {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            final FileChannel channel = file.getChannel();
            final int readCount = channel.read(byteBuffer);
            System.out.println("读到" + readCount + "字节");
            byteBuffer.flip();
            final CharBuffer charBuffer = utf8.decode(byteBuffer);
            System.out.println("解码出来总共" + charBuffer.length() + "个字");
            for (int i = 0; i < charBuffer.length(); i++) {
                System.out.print(charBuffer.get(i));
            }
//            System.out.println(Arrays.toString(charBuffer.array()));
        }

    }
}
