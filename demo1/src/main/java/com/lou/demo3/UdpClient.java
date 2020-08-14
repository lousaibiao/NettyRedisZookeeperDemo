package com.lou.demo3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class UdpClient {
    public static void main(String[] args) {
        String ip = "127.0.0.1";
        int port = 8234;
        try (final DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);
            final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            final Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                final String input = scanner.next();
                if (input.equals("exit")) {
                    System.out.println("结束");
                    return;
                }
                final byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                System.out.println("输入" + bytes.length + "字节的数据");
                byteBuffer.put(bytes);
                byteBuffer.flip();
                System.out.println("转为读模式,开始发送");
                final int sendBytes = channel.send(byteBuffer, new InetSocketAddress(ip, port));
                byteBuffer.clear();
                System.out.println("发送" + sendBytes + "字节的数据");
            }
            channel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
