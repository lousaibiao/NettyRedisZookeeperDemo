package com.lou.demo5;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class AcceptorHandler implements Runnable{
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    public AcceptorHandler(Selector selector, ServerSocketChannel serverSocketChannel) {
        this.selector = selector;
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public void run() {

    }
}
