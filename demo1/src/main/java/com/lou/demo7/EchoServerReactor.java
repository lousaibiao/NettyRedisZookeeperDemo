package com.lou.demo7;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EchoServerReactor implements Runnable {//这个Runnable是有用的，因为要开一个线程来处理。
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    /**
     * 初始化配置相关
     * @throws Exception
     */
    public EchoServerReactor() throws Exception {
        //实例化selector和serverSocketChannel
        selector = Selector.open();//全局都用这个,
        serverSocketChannel = ServerSocketChannel.open();
        //非阻塞
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 8234), 1000);
        //把serverSocketChannel注册到selector
        final SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //附加上AcceptHandler，以便accept的时候能使用
        selectionKey.attach(new AcceptHandler(serverSocketChannel, selector));

    }

    @Override
    public void run() {//服务启动进的这个方法。
        while (!Thread.interrupted()) {
            try {
                final int selectedCount = selector.select();//阻塞选择，select注册过accept，所以会进去
                System.out.println("得到"+selectedCount+"个事件");
                final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                final Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    final SelectionKey selectionKey = it.next();
                    //找到一个,这个Dispatch可能是echo,也可能是accept，因为在echoHandler里面这个select注册了Read事件，然后wakeup了起来。就触发了上面的selector.select()方法。
                    dispatch(selectionKey);
                    System.out.println("分发完1个");
                }
                selectionKeys.clear();//选完一遍要clear掉，避免重复
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void dispatch(SelectionKey selectionKey) {
        //拿里面的Handler
        final Runnable handler = (Runnable) selectionKey.attachment();
        System.out.println("拿到runnable为" + handler.getClass().getName() + "|");
        if (handler != null) {
            //这个run方法只是一个方法，和Thread的run没有关系。
            handler.run();//用了多态的性质，attach进来的对象都实现了Runnable接口，所以可以直接调用run方法。这个和thread的多线程没有关系，单纯调用run方法还是阻塞的。
            //这里也能用其他的接口来

            //这个不行，因为new EchoHandler会调用wakeUp方法，立即激活了阻塞的select方法，导致accept到N个null的连接
//            new Thread(handler).start();
        }
    }


    public static void main(String[] args) throws Exception {
        //开线程做启动server
        new Thread(new EchoServerReactor()).start();
    }


}
