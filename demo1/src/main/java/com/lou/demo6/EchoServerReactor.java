package com.lou.demo6;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EchoServerReactor implements Runnable {
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    public EchoServerReactor() throws Exception {
        //开启selector和serverSocketChannel
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();

        //配置非阻塞
        serverSocketChannel.configureBlocking(false);
        // 注册accept事件
        final SelectionKey acceptKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 8234));
        acceptKey.attach(new AcceptHandler(serverSocketChannel, selector));// 把Accept需要用的先attach进去，等到真正accept的取出来用
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                selector.select();
                final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    final SelectionKey acceptKey = iterator.next();
                    if (acceptKey.isAcceptable()) {// 肯定是这个
                        final AcceptHandler acceptHandler = (AcceptHandler) acceptKey.attachment();
                        acceptHandler.run();
                    }
                }
                selectionKeys.clear();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Exception {
        new Thread(new EchoServerReactor()).run();
    }
}
