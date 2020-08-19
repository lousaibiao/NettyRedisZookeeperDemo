package com.lou.demo6;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class IOHandler implements Runnable {
    private SocketChannel socketChannel;
    private SelectionKey selectionKey;

    public IOHandler(SocketChannel socketChannel, SelectionKey selectionKey) {
        this.socketChannel = socketChannel;
        this.selectionKey = selectionKey;
    }

    @Override
    public void run() {
        try {
            selectionKey.selector().select();
            final Set<SelectionKey> selectionKeys = selectionKey.selector().selectedKeys();
            while (selectionKeys.iterator().hasNext()) {
                final SelectionKey next = selectionKeys.iterator().next();
                if (next.isReadable()) {
                    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    final StringBuilder builder = new StringBuilder();
                    final CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
                    builder.append(charBuffer.array());
                    System.out.println("收到" + builder.toString());
                    byteBuffer.clear();
                }
            }
            selectionKeys.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
