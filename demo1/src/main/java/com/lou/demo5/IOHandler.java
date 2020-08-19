package com.lou.demo5;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class IOHandler implements Runnable {
    public IOHandler(SocketChannel socketChannel, SelectionKey selectionKey) throws IOException {
        this.socketChannel = socketChannel;
        this.selectionKey = selectionKey;

        socketChannel.configureBlocking(false);
    }

    SocketChannel socketChannel;
    SelectionKey selectionKey;

    @Override
    public void run() {

    }
}
