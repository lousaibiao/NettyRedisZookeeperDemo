package com.lou.demo7;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class EchoHandler implements Runnable {
    final SocketChannel socketChannel;
    final SelectionKey selectionKey;
    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    static final int Receiving = 0, Sending = 1;
    int state = Receiving;

    public EchoHandler(SocketChannel socketChannel, Selector selector) throws Exception {
        this.socketChannel = socketChannel;
        //配置非阻塞。
        socketChannel.configureBlocking(false);
        //把连接进来的socket也注册进去。selector一直是同一个。
        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(this);// 这里也会把需要的handler赋值给selectionKey。
        selectionKey.interestOps(SelectionKey.OP_READ);//然后告诉selectionKey自己还对read完成事件感兴趣。
//        selector.wakeup();//这里会触发第一个select方法返回，也就是到了EchoServerReactor中的select方法，会得到0个事件。
    }

    @Override
    public void run() {//注册了上面的read完成，然后附加了自己，等外面select到，就会执行这个Runnable.run()方法。
        System.out.println("进入EchoHandler.run");

        try {
            if (state == Receiving) {
                int length = 0;
                while ((length = socketChannel.read(byteBuffer)) > 0) {//把收到的东西放到byteBuffer里面
                    System.out.println("收到" + new String(byteBuffer.array(), 0, length));
                }
                if (length < 0) {
                    System.out.println("收到-1字节数据，断开");
                    socketChannel.close();
                    return;
                }
                byteBuffer.flip();//放完之后把byteBuffer变成读模式，以便后面的去读取
                //已经写完了，这时候需要让SelectionKey去注册一下，表示自己对write感兴趣，也就是说可以写就绪
                selectionKey.interestOps(SelectionKey.OP_WRITE);//interest 感兴趣
                state = Sending;//状态转为send，发送状态。
            } else if (state == Sending) {//
                //开始读取byteBuffer里面的东西，然后回写到socket中去
                socketChannel.write(byteBuffer);
                //写完socket也就是读完byteBuffer里面的内容后，转为写模式，以便后面能继续写进去
                byteBuffer.clear();
                //现在又表示自己对read就绪比较感兴趣
                selectionKey.interestOps(SelectionKey.OP_READ);
                state = Receiving;
            }
//            selectionKey.cancel();//如果写上这个就不能在Receive之后Send回去了。
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
