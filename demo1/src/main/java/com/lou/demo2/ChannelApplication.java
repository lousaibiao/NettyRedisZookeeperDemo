package com.lou.demo2;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ChannelApplication {
    public static void main(String[] args) throws Exception {
        final Path pathSrc = Paths.get(System.getProperty("user.dir"), "1.txt");
        final Path pathDest = Paths.get(System.getProperty("user.dir"), "2.txt");
        copyFileTest(pathSrc.toString(), pathDest.toString());

    }

    /**
     * 文件拷贝
     *
     * @param pathSrc  源路径
     * @param pathDest 目的路径
     * @throws Exception
     */
    private static void copyFileTest(String pathSrc, String pathDest) throws Exception {
        try (RandomAccessFile readFile = new RandomAccessFile(pathSrc, "rw");
             RandomAccessFile writeFile = new RandomAccessFile(pathDest, "rw")) {
            final FileChannel readFileChannel = readFile.getChannel();
            final FileChannel writeFileChannel = writeFile.getChannel();
            final ByteBuffer byteBuffer = ByteBuffer.allocate(2);//每次就写2个字节
            int readLength = -1;
            int writeLength = 0;
            while ((readLength = readFileChannel.read(byteBuffer)) != -1) {
                System.out.println("读取到" + readLength + "字节数据");
                byteBuffer.flip();//写模式转为读模式
                writeFile.seek(writeFile.length());//到文件尾
                while ((writeLength = writeFileChannel.write(byteBuffer)) != 0) {
                    System.out.println("写入" + writeLength + "字节数据");
                }
                byteBuffer.clear();//清掉，不然byteBuffer就无法继续写入
            }
        }
    }

    /**
     * 写文件
     *
     * @param path
     * @throws Exception
     */
    private static void writeFileChannelTest(String path) throws Exception {
        final RandomAccessFile accessFile = new RandomAccessFile(path, "rw");
        final FileChannel filechannel = accessFile.getChannel();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.put((byte) 'a');
        byteBuffer.put((byte) 'b');
        byteBuffer.put((byte) 'c');
        byteBuffer.put((byte) 'd');
        byteBuffer.flip();//转成读模式才能让数据写进去
        accessFile.seek(accessFile.length());//seek到结尾，相当于append
        final int i = filechannel.write(byteBuffer);
        filechannel.force(true);
        System.out.println("写入" + i + "个字节");
        filechannel.close();
    }

    /**
     * 读文件
     *
     * @param path
     * @throws Exception
     */
    private static void readFileChannelTest(String path) throws Exception {

        final RandomAccessFile accessFile = new RandomAccessFile(path, "r");
        final FileChannel fileChannel = accessFile.getChannel();
        //每次读一个，一般都是设置成一次读N个。
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        int length = -1;
        while ((length = fileChannel.read(byteBuffer)) != -1) {
            System.out.println(length);//读到1个字节
            System.out.println(Arrays.toString(byteBuffer.array()));
            if (!byteBuffer.hasRemaining()) {//没有空位的说明buffer已经读完，需要clear掉以便他能重新写。
                byteBuffer.clear();
            }
        }
        System.out.println("读完成");
        fileChannel.close();


    }
}
