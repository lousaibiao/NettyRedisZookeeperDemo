package com.lou.demo2;

import java.nio.IntBuffer;
import java.util.Arrays;

public class BufferApplication {
    public static void main(String[] args) {
        final IntBuffer intBuffer = IntBuffer.allocate(10);
        System.out.println("新分配的Buffer写模式，最多能写10个数字");
        printDetail(intBuffer);
        intBuffer.put(11);
        intBuffer.put(22);
        intBuffer.put(33);
        intBuffer.put(44);
        System.out.println("写入4个int后Buffer情况");
        printDetail(intBuffer);
        intBuffer.flip();
        System.out.println("调用flip转成读模式,Buffer情况");
        printDetail(intBuffer);
        System.out.println("调用get方法读取2次");
        int i = intBuffer.get();
        System.out.println("第一次读到" + i);
        i = intBuffer.get();
        System.out.println("第二次读到" + i);
        printDetail(intBuffer);
        System.out.println("读完2次后使用mark做一下标记");
        intBuffer.mark();
        printDetail(intBuffer);
        i = intBuffer.get();
        System.out.println("第三次读取到" + i);
        printDetail(intBuffer);
        intBuffer.reset();
        System.out.println("调用reset");
        printDetail(intBuffer);
        i = intBuffer.get();
        System.out.println("再读一个数,读取到" + i);
        printDetail(intBuffer);
        intBuffer.rewind();
        System.out.println("调用rewind开始重读");
        printDetail(intBuffer);
        i = intBuffer.get();
        System.out.println("读到数为" + i);
        printDetail(intBuffer);
        final IntBuffer compactBuffer = intBuffer.compact();
        System.out.println("调用compact方法后");
        printDetail(intBuffer);
        System.out.println("compact后为写模式,写入一个数55");
        intBuffer.put(55);
        printDetail(intBuffer);
//        intBuffer.clear();
//        printDetail(intBuffer);


    }

    private static void printDetail(IntBuffer intBuffer) {
        System.out.println("Buffer情况:" + Arrays.toString(intBuffer.array()));
        System.out.println("capacity:" + intBuffer.capacity() + ",limit:" + intBuffer.limit() + ",position:" + intBuffer.position());
    }
}
