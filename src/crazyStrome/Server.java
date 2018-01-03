package crazyStrome;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.System.out;
import static java.lang.System.runFinalizersOnExit;

public class Server {
    /**
     * 服务端口
     */
    private final static int PORT = 30000;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private Charset charset = Charset.forName("utf-8");
    /**
     * 记录每个客户端对应的用户名
     */
    public static CrazyStromeMap<String, SocketChannel> clients
            = new CrazyStromeMap<>();

    private void startListening() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (selector.select() > 0) {
                for (SelectionKey sk : selector.selectedKeys()) {
                    selector.selectedKeys().remove(sk);
                    if (sk.isAcceptable()) {
                        /**
                         * 客户端链接到服务器
                         */
                        SocketChannel socket = serverChannel.accept();
                        out.println(socket.getLocalAddress() + " 连接到服务器");
                        socket.write(ByteBuffer.wrap("连接成功,请输入用户名和密码，以':'分割".getBytes(charset)));
                        socket.configureBlocking(false);
                        socket.register(selector, SelectionKey.OP_READ);
                        sk.interestOps(SelectionKey.OP_ACCEPT);
                    }
                    if (sk.isReadable()) {
                        SocketChannel socket = (SocketChannel) sk.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        String content = "";
                        try {
                            while (socket.read(buffer) > 0) {
                                buffer.flip();
                                content += charset.decode(buffer);
                                buffer.clear();
                            }
                            new Thread(new ServerThread(content, socket)).start();
                            out.println("读取的数据: " + content);
                            sk.interestOps(SelectionKey.OP_READ);
                        } catch (Exception e) {
                            sk.cancel();
                            if (sk.channel() != null) {
                                sk.channel().close();
                            }
                        }

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private class ServerThread implements Runnable {

        private String content;
        private SocketChannel socketChannel;

        public ServerThread(String content, SocketChannel socketChannel) {
            this.content = content;
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                if (content.startsWith(CrazyStromeProtocol.LOGIN_ROUND)
                        && content.endsWith(CrazyStromeProtocol.LOGIN_ROUND)) {
                    /**
                     * 信息头为登陆头
                     */
                    String userName = getRealMsg(content).split(CrazyStromeProtocol.SPLIT_SIGN)[0];
                    String password = getRealMsg(content).split(CrazyStromeProtocol.SPLIT_SIGN)[1];
                    System.out.println(userName + password + "正在登陆");
                    if (Server.clients.map.containsKey(userName)) {
                        System.out.println("用户登陆重复: " + userName);
                        socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.NAME_REP.getBytes(charset)));
                    } else {
                        int result = MySQLExcutor.CheckAccount(userName, password);
                        if (result == MySQLExcutor.NOT_EXIST) {
                            System.out.println("用户不存在: " + userName);
                            socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.NAME_NOT_EXIST.getBytes(charset)));
                        } else if (result == MySQLExcutor.FAILS) {
                            System.out.println("用户登录失败: " + userName);
                            socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.LOGIN_FAILED.getBytes(charset)));
                        } else if (result == MySQLExcutor.SUCCESSED) {
                            System.out.println("用户登陆成功: " + userName);
                            Server.clients.map.put(userName, socketChannel);
                            socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.LOGIN_SUCCESS.getBytes(charset)));
                        }
                    }
                } else if (content.startsWith(CrazyStromeProtocol.REGISTER_ROUND)
                        && content.endsWith(CrazyStromeProtocol.REGISTER_ROUND)) {
                    /**
                     * 信息头为注册头
                     */
                    String userName = getRealMsg(content).split(CrazyStromeProtocol.SPLIT_SIGN)[0];
                    String password = getRealMsg(content).split(CrazyStromeProtocol.SPLIT_SIGN)[1];
                    int result = MySQLExcutor.CreateAccount(userName, password);
                    if (result == MySQLExcutor.SUCCESSED) {
                        System.out.println("用户注册成功: " + userName);
                        socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.REGISTER_SUCCESS.getBytes(charset)));
                        Server.clients.map.put(userName, socketChannel);
                    } else if (result == MySQLExcutor.FAILS) {
                        System.out.println("用户注册失败: " + userName);
                        socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.REGISTER_FAILED.getBytes(charset)));
                    }
                } else if (content.startsWith(CrazyStromeProtocol.PRIVATE_ROUND)
                        && content.endsWith(CrazyStromeProtocol.PRIVATE_ROUND)) {
                    /**
                     * 私人信息头处理
                     */
                    String userAndMsg = getRealMsg(content);
                    String user = userAndMsg.split(CrazyStromeProtocol.SPLIT_SIGN)[0];
                    String msg = userAndMsg.split(CrazyStromeProtocol.SPLIT_SIGN)[1];
                    Server.clients.map.get(user).write(ByteBuffer.wrap((Server.clients.getKeyByValue(socketChannel)
                            + " 悄悄对你说: " + msg).getBytes(charset)));
                } else if (content.startsWith(CrazyStromeProtocol.ROOT_ROUND) &&
                    content.endsWith(CrazyStromeProtocol.ROOT_ROUND)) {
                    /**
                     * root信息头处理
                     */
                    if (Server.clients.getKeyByValue(socketChannel) != null && Server.clients.getKeyByValue(socketChannel).equals("root")) {
                        String command = getRealMsg(content);
                        if (command.toLowerCase().equals("ls")) {
                            String result = MySQLExcutor.ShowAllDatas();
                            if (result == null) {
                                socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.QUERY_FAIL.getBytes(charset)));
                            } else {
                                socketChannel.write(ByteBuffer.wrap(result.getBytes(charset)));
                            }
                        } else {
                            String[] res = new String[command.split("\\s+").length-1];
                            for (int i = 0; i < res.length; i ++) {
                                res[i] = command.split("\\s+")[i + 1];
                            }
                            int result = MySQLExcutor.DeleteByName(res);
                            if (result == MySQLExcutor.SUCCESSED) {
                                socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.DEL_SUCCESS.getBytes(charset)));
                            } else if (result == MySQLExcutor.FAILS) {
                                socketChannel.write(ByteBuffer.wrap(CrazyStromeProtocol.DEL_FAIL.getBytes(charset)));
                            }
                        }
                    } else {
                        socketChannel.write(ByteBuffer.wrap("你没有root权限".getBytes(charset)));
                    }
                } else {
                    /**
                     * 给每个人发送的信息头
                     */
                    String msg = getRealMsg(content);
                    for (SocketChannel s : Server.clients.valueSet()) {
                        s.write(ByteBuffer.wrap((Server.clients.getKeyByValue(socketChannel) + "说: "
                                + msg).getBytes(charset)));
                    }
                }
            } catch (IOException e) {
                Server.clients.removeByValue(socketChannel);
                System.out.println("当前剩余客户端数量: " + Server.clients.map.size());
                try {
                    if (socketChannel != null) {
                        socketChannel.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 去掉信息头获得实际的信息
     * @param content
     * @return 实际的信息
     */
    private String getRealMsg(String content) {
        return content.substring(CrazyStromeProtocol.PROTOCOL_LEN,
                content.length() - CrazyStromeProtocol.PROTOCOL_LEN);
    }
    public static void main(String[] args) throws IOException{
        if (args.length > 0) {
            if (args[0].toLowerCase().matches("-n\\S*")) {
                NIOClient.main(new String[]{});
            } else {
                new Server().startListening();
            }
        } else {
            AIOClient.main(new String[]{});
        }
    }
}
