package com.lou.demo4;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NioDiscardServerApplication {
    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        Integer port = 8234;
        final Selector selector = Selector.open();
        System.out.println("开启一个tcp服务器");
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        System.out.println("配置非阻塞");
        serverSocketChannel.configureBlocking(false);
        System.out.println("绑定端口ip");
        serverSocketChannel.bind(new InetSocketAddress(ip, port));
        System.out.println("注册accept事件");
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("select方法阻塞调用");
        while (selector.select() > 0) {
            System.out.println("有事件进来后遍历事件");
            Integer i = 1;
            final Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                System.out.println(("第" + i++ + "个事件"));
                System.out.println("获取事件");
                final SelectionKey selectionKey = selectedKeys.next();
                System.out.println("判断事件类型");
                if (selectionKey.isAcceptable()) {
                    System.out.println("新连接进来");
                    final SocketChannel socketChannel = serverSocketChannel.accept();
                    System.out.println("远程地址为" + socketChannel.getRemoteAddress());
                    System.out.println("accept得到连接，配置非阻塞");
                    socketChannel.configureBlocking(false);
                    System.out.println("给socketChannel注册数据可读");
                    socketChannel.register(selector, selectionKey.OP_READ);
                } else if (selectionKey.isReadable()) {
                    System.out.println("可读事件");
                    final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    Integer len = 0;
                    while ((len = socketChannel.read(byteBuffer)) > 0) {
                        System.out.println("socketChannel写入了" + len + "字节的之后把byteBuffer转为读取");
                        byteBuffer.flip();
                        System.out.println("解码byteBuffer内的数据");
                        final char[] chars = StandardCharsets.UTF_8.decode(byteBuffer).array();
                        final StringBuilder builder = new StringBuilder();
                        System.out.println("读取到" + chars.length + "个字符,汉字编码方式要匹配");
                        builder.append(chars);
                        System.out.println("StringBuilder:" + builder.toString());
                        System.out.println("清理byteBuffer以便重新写入");
                        byteBuffer.clear();
                    }
                    System.out.println("读取完毕后关闭");
                    if (len == -1) {
                        System.out.println("读取到0字节，关闭");
                        socketChannel.close();
                    }
                }
            }
            System.out.println("移出一次select到的所有事件避免重复读取");
            selectedKeys.remove();
        }
        System.out.println("关闭服务器server连接");
        serverSocketChannel.close();
    }
}
