# 书名

《Netty,Redis,Zookeeper 高并发实战》
# 第一章

Netty，Redis，Zookeeper的简介。

Zookeeper，名字由来，雅虎内部的应用习惯用动物命名，这时候就需要一个动物园管理员的角色来"管理"这些动物，所有就有了这个名字。

# 第二章

## IO读写的逻辑

读:硬件设备（ex:网卡）→ 内核 → 用户空间。实际开发只涉及内核到用户空间的部分。

写:用户空间 → 内核 → 硬件设备。实际开发只涉及用户空间到内核的部分。

简单来说，平时开发中的读写只有`用户空间`和`内核`之间的交互，和硬件的实际交互由操作系统自己去完成，用户无感。

## 四种IO模型

### 基本概念

+ 阻塞和非阻塞

  表示的是程序的`执行状态`。比如调用了Read，线程就需要一直等待read到东西，这种就是阻塞。如果直接返回结果，哪怕是没有结果的结果，那么属于非阻塞。

+ 同步和异步 

  表示IO的主动发起方是谁。同步就是用户发起。异步就是内核发起。

### 具体模型

1. 同步阻塞

   java的OIO（OldIO)。用户发起read操作，线程被卡住，一直等到有结果。

   + 优点：简单。阻塞的时候cpu能做其他事情。
   + 缺点：一般做成一个连接一个线程。这样在高并发的情况下会需要很多线程。

2. 同步非阻塞

   Noblocking IO。用户调用read，操作系统立马告诉现在的情况，如果能read，用户就能开始read，否则就去做别的事情。

   + 优点：及时性比较好，立马就有结果。
   + 缺点：为了能读到数据，就需要用户自己不断的轮询，看是否有可操作的资源。

3. 多路复用(异步阻塞？)

   **重点**，**重点**，**重点**。java的NIO就是这种模型。用户需要读取数据，就开一个选择器，告诉系统，自己对哪些东西感兴趣（ex：新连接，可读连接，可写连接等)。系统通过自带的select/epoll(前者升级版)来获取到可操作的东西。用户通过阻塞调用Selector.select()查询方法获取到自己的想要的连接，之后用户线程就能开始操作。

   + 优点：对比第一种，避免了创建大量的线程，对比第二种，轮询由系统完成了，不需要自己维护。
   + 缺点：系统在自己select/epoll的时候是阻塞的。读写的操作本身也是阻塞的。

   异步体现在哪里？状态的轮询由系统完成，相当于内核完成。

   阻塞在哪里？epoll的时候阻塞，后续的read/write也是阻塞。

4. 异步非阻塞

   用户注册io操作，操作系统去执行，执行完成后通知用户，用户去做后面的事情。

   linux基于epoll实现，所以本质还是多路复用，并不是真正的异步非阻塞。

   win基于IOCP实现，属于异步非阻塞。

### 生产环境配置

增加linux环境的文件句柄来增加最大连接数。

```bash
# 编辑/etc/rc.local文件，增加下面内容 S表示软极限，超过这个会警告。
ulimit -SHn 1000000 # 最大1000000个连接。
# 或者编辑/etc/security/limits.conf文件,增加下面内容
soft nofile 1000000
hard nofile 1000000
```

# 第三章

jdk 1.4 版本之前是叫OIO(Old IO)，阻塞。后面增加了NIO(New IO，No-blocking IO)，非阻塞。

## Buffer，Channel，Selector 

NIO的3个核心组件。一句话来说就是Selector告诉你放到Buffer里面需要通过Channel发出去的数据怎么样了，有没有通过Channel收到新数据。

### Buffer

非线程安全类。具体有ByteBuffer,CharBuffer,FloatBuffer,DoubleBuffer,ShortBuffer,IntBuffer,LongBuffer,MappedByteBuffer。

有两个状态，读or写。

```java
public class BufferApplication {
    public static void main(String[] args) {
      /*
新分配的Buffer写模式，最多能写10个数字
Buffer情况:[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
capacity:10,limit:10,position:0
写入4个int后Buffer情况
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:10,position:4
调用flip转成读模式,Buffer情况
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:0
调用get方法读取2次
第一次读到11
第二次读到22
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:2
读完2次后使用mark做一下标记
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:2
第三次读取到33
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:3
调用reset
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:2
再读一个数,读取到33
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:3
调用rewind开始重读
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:0
读到数为11
Buffer情况:[11, 22, 33, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:4,position:1
调用compact方法后
Buffer情况:[22, 33, 44, 44, 0, 0, 0, 0, 0, 0]
capacity:10,limit:10,position:3
compact后为写模式,写入一个数55
Buffer情况:[22, 33, 44, 55, 0, 0, 0, 0, 0, 0]
capacity:10,limit:10,position:4
*/

    }

    private static void printDetail(IntBuffer intBuffer) {
        System.out.println("Buffer情况:" + Arrays.toString(intBuffer.array()));
        System.out.println("capacity:" + intBuffer.capacity() + ",limit:" + intBuffer.limit() + ",position:" + intBuffer.position());
    }
}
```

`allocate(x)`方法分配一个的Buffer，初始为**写**状态，初始化`capacity`（容量）10，`position`为0。然后开始`put()`写入，每写一个数据，position加1。写的时候不改变`limit`的值，一直为`capacity`。调用`flip()`方法把Buffer转成**读**模式。能读的数据上限`limit`就是之前写的N个数据。position为0，`get()`读一个加一，上限就是N。读到中间某个位置，n1，调用`mark()`标记一下，然后继续`get()`,之后可以调用`reset()`回到之前`mark`的点，重新读，也可以使用`rewind()`回到最初开始重新读。rewind相当于mark和reset的一种特例。读"完"之后需要变成写模式，可以使用`clear`或者`compact`。clear就认为已经完全读完，之后就是从头开始写。compact就是记录position之前的东西算读完的，可以不要，但之后的会给保留。

flip用于写转读。

clear为变成写模式，在读的时候可以传成写模式，在写的时候变成从头开始写。

compact变成写模式。忽略掉已经读过的部分。

属性&方法

| 属性&方法  | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| capacity   | 初始化时创建，Buffer的上限。最大能写入的个数。构造完成就不可变。 |
| limit      | 写模式下，最多能写入的个数。读模式下，最多能读取的个数。多写一个才能多读一个。 |
| position   | 当前的位置，写一个+1，读一个+1                               |
| mark()     | mark在n1位置做一下标记，然后再读几个，再通过reset退到n1位置重新读。 |
| allocate() | 分配Buffer                                                   |
| put()      | 放入一个数                                                   |
| get()      | 获取一个数                                                   |
| reset()    | 读模式下退到mark()标记的位置                                 |
| rewind()   | 重读                                                         |
| clear()    | 已经读完，可以重新开始从头开始写                             |
| compact()  | 读了一部分，读过的可以不要，后面的保留。                     |

### Channel

通道是一个底层文件的描述，可以表示一个硬件设备，文件，网络连接等。

重要的4个通道有FileChannel，ServerSocketChannel，SocketChannel，DatagramChannel。

+ FileChannel

  文件读写。

  ```java
  public class ChannelApplication {
      public static void main(String[] args) throws Exception {
          final Path pathSrc = Paths.get(System.getProperty("user.dir"), "1.txt");
          final Path pathDest = Paths.get(System.getProperty("user.dir"), "2.txt");
          copyFileTest(pathSrc.toString(), pathDest.toString());
  
      }
  
      /**
       * 文件拷贝
       *
       * @param pathSrc  源路径
       * @param pathDest 目的路径
       * @throws Exception
       */
      private static void copyFileTest(String pathSrc, String pathDest) throws Exception {
          try (RandomAccessFile readFile = new RandomAccessFile(pathSrc, "rw");
               RandomAccessFile writeFile = new RandomAccessFile(pathDest, "rw")) {
              final FileChannel readFileChannel = readFile.getChannel();
              final FileChannel writeFileChannel = writeFile.getChannel();
              final ByteBuffer byteBuffer = ByteBuffer.allocate(2);//每次就写2个字节
              int readLength = -1;
              int writeLength = 0;
              while ((readLength = readFileChannel.read(byteBuffer)) != -1) {
                  System.out.println("读取到" + readLength + "字节数据");
                  byteBuffer.flip();//写模式转为读模式
                  writeFile.seek(writeFile.length());//到文件尾
                  while ((writeLength = writeFileChannel.write(byteBuffer)) != 0) {
                      System.out.println("写入" + writeLength + "字节数据");
                  }
                  byteBuffer.clear();//清掉，不然byteBuffer就无法继续写入
              }
          }
      }
  }
  
  ```

+ DatagramChannel

  Udp连接的数据读写。

  ```java
  public class UdpClient {
      public static void main(String[] args) {
          String ip = "127.0.0.1";
          int port = 8234;
          try (final DatagramChannel channel = DatagramChannel.open()) {
              channel.configureBlocking(false);
              final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
              final Scanner scanner = new Scanner(System.in);
              while (scanner.hasNext()) {
                  final String input = scanner.next();
                  if (input.equals("exit")) {
                      System.out.println("结束");
                      return;
                  }
                  final byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                  System.out.println("输入" + bytes.length + "字节的数据");
                  byteBuffer.put(bytes);
                  byteBuffer.flip();
                  System.out.println("转为读模式,开始发送");
                  final int sendBytes = channel.send(byteBuffer, new InetSocketAddress(ip, port));
                  byteBuffer.clear();
                  System.out.println("发送" + sendBytes + "字节的数据");
              }
              channel.close();
  
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }
  ```

  把byteBuffer内的内容通过DatagramChannel.open出来的channel用send方法发送到指定的地址。接收部分的demo以及Tcp部分的知识点在选择器部分整理。

+ ServerSocketChannel

  服务器套接字，监听请求，accept后获取下面的SocketChannel，然后通过SocketChannel进行通信

+ SocketChannel

  Tcp连接的数据读写。

### Selector

这个最**重要**。

#### DiscardClient & DiscardServer

`com.lou.demo4.NioDiscardServerApplication`

```bash
开启一个tcp服务器
配置非阻塞
绑定端口ip
注册accept事件
select方法阻塞调用
有事件进来后遍历事件
第1个事件
获取事件
判断事件类型
新连接进来
accept得到连接，配置非阻塞
给socketChannel注册数据可读
移出一次select到的所有事件避免重复读取
有事件进来后遍历事件
第1个事件
获取事件
判断事件类型
可读事件
socketChannel写入了3字节的之后把byteBuffer转为读取
解码byteBuffer内的数据
读取到3个字符,汉字编码方式要匹配
StringBuilder:楼
清理byteBuffer以便重新写入
读取完毕后关闭
读取到0字节，关闭
移出一次select到的所有事件避免重复读取

```

`com.lou.demo4.NioDiscardClientApplication`

```bash
连接成功
写入socket【3】字节数据

```

## 小结

Buffer用来读写数据，通过flip和clear（compact）做状态的转换。

Channel为读写数据的通道，把buffer中的数据通过read/write方法处理掉。实现了`SelectedableChannel`的Channel可以被selector监控，从而高效的获取对应的事件。

Selector是重点，相较于OIO，内部使用epoll的方式实现多路复用。

1. `open`实例化出一个选择器。
2. `open`出一个ServerSocketChannel。
3. 配置非阻塞（NIO）。
4. 通过`SelectionKey`参数绑定两者。并且返回一个SelectionKey对象。
5. select()方法为阻塞调用。
6. 遍历选择器选中的selectedKeys，判断类型，完成对应的操作。

# 第四章

## Reactor反应器模型

nginx，redis，netty都是基于这个模型。

由**两**部分构成

1. 反应器
2. Handlers，不同的处理者，用来处理不同的事情。

为什么要用这个模型。

+ OIO，早期都是OIO的开发方式。
  + 单线程：是阻塞的，一般while循环里面，先accept再handle，handle没有结束，其他accept是进不来的。
  + 多线程：一个connection就开一个thread，连接数多了之后thread会比较多，导致系统开销比较严重。早期的tomcat是用这种方式。所以，其实如果连接不是太多，`问题也不大的`。
    + 为了解决线程过多的问题，可以让一个线程处理多个连接。但是，OIO本身是阻塞的，在处理一个的时候其他是无法处理的。
+ NIO，NIO出来之后可以用他来实现，有了选择器等，效率上来了，不过还是while循环selector的就绪事件，所以代码结构不好。

这个模型相当于要解决几个问题。

1. 线程不能太多。
2. 要尽可能的高效。
3. 代码要好看

### 单线程版本

#### 具体代码

首先需要一个反应器。`EchoServerReactor`。

```java
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
                selector.select();//阻塞选择，select注册过accept，所以会进去
                final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                final Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    final SelectionKey selectionKey = it.next();
                    //找到一个,这个Dispatch可能是echo,也可能是accept，因为在echoHandler里面这个select注册了Read事件，然后wakeup了起来。就触发了上面的selector.select()方法。
                    dispatch(selectionKey);
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
```

两个Handler。

+ AcceptHandler，用来接收连接

  ```java
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
  ```

+ EchoHandler，用来回写消息

  ```java
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
          //selector.wakeup();//这里会触发第一个select方法返回，也就是到了EchoServerReactor中的select方法，会得到0个事件。
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
  ```

#### 重点小结

流程：

1. main方法开线程启动server（实现了Runnable接口），
2. server的构造函数里面做配置，比如绑定的端口，需要注册的Accept选择键，Accept之后的Handler等。
3. server实现了Runnable接口，在run方法里面while(...)的方式去执行select（整个程序只有一个select），选完之后做Dispatch（派发）
4. 第一次选中的只有Accept就绪事件。AcceptHandler来处理。这个Handler会调用serverSocketChannel的accept方法，得到一个socketChannel，然后会用这个socketChannel去new一个EchoHandler。（相当于连接管理）
5. 实例化EchoHandler的重点是1.实现Runnable接口。2.注册read就绪，也就是client有数据发过来。3.把自己attach进`selectionKey`，以便在select里面做Dispatch的调用。
6. 需要实现的业务放到run方法里面。

缺点：

1. 单线程。main方法里面只启动了一个Thread，虽然几个Handler都实现了Runnable接口，但本质上还只是run方法的阻塞调用。handler的方法阻塞会导致系统卡住。
2. 没有利用好多核cpu的特点。

结论：

基本不用。需要用多线程版本。