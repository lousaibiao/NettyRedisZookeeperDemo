package com.lou.demo6;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class AcceptHandler implements Runnable {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public AcceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) {
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            final SocketChannel acceptSocketChannel = serverSocketChannel.accept();
            acceptSocketChannel.configureBlocking(false);
            final SelectionKey readAndWriteKey = acceptSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            readAndWriteKey.attach(new IOHandler(acceptSocketChannel, readAndWriteKey));
            selector.select();
            final Set<SelectionKey> selectionKeys = selector.selectedKeys();
            final Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                final SelectionKey next = iterator.next();
                final Runnable ioHandler = (Runnable) next.attachment();
                ioHandler.run();
            }
            selectionKeys.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
