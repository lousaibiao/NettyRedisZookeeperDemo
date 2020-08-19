package com.lou.demo5;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class Reactor implements Runnable {

    Selector selector;
    ServerSocketChannel serverSocketChannel;

    Reactor() throws Exception {

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        final SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//        selectionKey.attach(new AcceptorHandler() )
    }

    @Override
    public void run() {

    }
}
