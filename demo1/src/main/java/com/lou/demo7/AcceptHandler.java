package com.lou.demo7;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler implements Runnable {//这个Runnable和EchoHandler实现的Runnable没有必要，可以用其他接口代替，都实现只是为了调用run方法。

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;

    public AcceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) {
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
    }

    @Override
    public void run() {
        System.out.println("进入AcceptHandler.run");
        try {
            final SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {//因为配置的是非阻塞的socket，所以可能会得到null的连接
                new EchoHandler(socketChannel, selector);//实例化一个EchoHandler，表面上是实例化，实际上有wakeup的方法调用，会再触发一次。
            } else {
                System.out.println("accept 得到null的连接");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
