package com.lou.demo4;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioDiscardClientApplication {
    public static void main(String[] args) throws Exception {
        final SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 8234));
        while (!socketChannel.finishConnect()) {

        }
        System.out.println("连接成功");
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        String str = "楼";
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        final int writeBytes = socketChannel.write(byteBuffer);
        System.out.println("写入socket【" + writeBytes + "】字节数据");
        socketChannel.close();
    }
}
