package com.lou.demo8;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestServer {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
//        serverSocketChannel.listen()//没有这个方法
//        Executors.newFixedThreadPool(4);//最多4个线程
    }
}
