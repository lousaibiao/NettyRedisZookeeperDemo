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





