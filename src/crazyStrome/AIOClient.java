package crazyStrome;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

public class AIOClient {

    private static final int SERVER_PORT = 30000;
    private Charset charset = Charset.forName("utf-8");
    private AsynchronousSocketChannel clientChannel;
    private JFrame mainWin = new JFrame("多人聊天窗口");
    private JTextArea jta = new JTextArea(16, 48);
    private JTextField jtf = new JTextField(40);
    private JButton sendBn = new JButton("发送");
    private String name;

    private void init() {
        /**
         * 使整个UI和平台相关
         */
        if (UIManager.getLookAndFeel().isSupportedLookAndFeel()) {
            final String platform = UIManager.getSystemLookAndFeelClassName();
            if (!UIManager.getLookAndFeel().getName().equals(platform)) {
                try {
                    UIManager.setLookAndFeel(platform);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mainWin.setLayout(new BorderLayout());
        Font font = new Font("微软雅黑", 0, 13);
        jta.setEnabled(false);
        jta.setFont(font);
        mainWin.add(new JScrollPane(jta), BorderLayout.CENTER);
        JPanel jp = new JPanel();
        jtf.setFont(font);
        jp.add(jtf);
        sendBn.setFocusPainted(false);
        sendBn.setFont(font);
        jp.add(sendBn);

        Action sendAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = jtf.getText();
                if (content.trim().length() > 0) {
                    handleSendMsg(content.trim());
                }
                jtf.setText("");
            }
        };

        sendBn.addActionListener(sendAction);
        jtf.getInputMap().put(KeyStroke.getKeyStroke('\n'
                , InputEvent.CTRL_MASK), "send");
        jtf.getActionMap().put("send", sendAction);
        mainWin.add(jp, BorderLayout.SOUTH);
        mainWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWin.pack();
        /**
         * 设置主窗口在屏幕中间
         */
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screen.getWidth();
        int screenHeigh = (int) screen.getHeight();
        mainWin.setLocation((screenWidth - mainWin.getWidth())/2,
                (screenHeigh - mainWin.getHeight())/2);

        mainWin.setResizable(false);
        mainWin.setVisible(true);
    }
    private void handleSendMsg(String content) {
        System.out.println(content);
        try {
            if (content.indexOf(":") > 0 || content.indexOf("：") > 0) {
                if (content.startsWith("//")) {
                    /**
                     * 发送给特定的人
                     */
                    String toName = content.substring(2).split("\\s*[:：]\\s*")[0];
                    String msg = content.substring(2).split("\\s*[:：]\\s*")[1];
                    String toSend = CrazyStromeProtocol.PRIVATE_ROUND + toName + CrazyStromeProtocol.SPLIT_SIGN
                            + msg + CrazyStromeProtocol.PRIVATE_ROUND;
                    clientChannel.write(ByteBuffer.wrap(toSend.getBytes(charset))).get();
                } else if (content.startsWith("root")) {
                    /**
                     * root用户的命令
                     * 目前只有ls、del两条命令
                     * ls:列出现在数据库中的所有用户和密码
                     * del name1 name2 ...:根据名字删除数据库中相应的条目
                     */
                    String command = content.split("\\s*[:：]\\s*")[1];
                    String toSend = CrazyStromeProtocol.ROOT_ROUND + command + CrazyStromeProtocol.ROOT_ROUND;
                    clientChannel.write(ByteBuffer.wrap(toSend.getBytes(charset))).get();

                } else {
                    /**
                     * 获取是Register还是Login开头
                     */
                    String method = content.split("\\s*[:：]\\s*")[0];
                    String msg = content.split("\\s*[:：]\\s*")[1];
                    String name = msg.split("\\s+")[0];
                    String pass = msg.split("\\s+")[1];

                    this.name = name;

                    if (method.toLowerCase().startsWith("re")) {
                        /**
                         * 注册
                         */
                        String toSend = CrazyStromeProtocol.REGISTER_ROUND + name + CrazyStromeProtocol.SPLIT_SIGN
                                + pass + CrazyStromeProtocol.REGISTER_ROUND;
                        clientChannel.write(ByteBuffer.wrap(toSend.getBytes(charset))).get();
                    } else if (method.toLowerCase().startsWith("lo")) {
                        /**
                         * 登陆
                         */
                        System.out.println(content + ":" + method);
                        String toSend = CrazyStromeProtocol.LOGIN_ROUND + name + CrazyStromeProtocol.SPLIT_SIGN
                                + pass + CrazyStromeProtocol.LOGIN_ROUND;
                        System.out.println(toSend);
                        clientChannel.write(ByteBuffer.wrap(toSend.getBytes(charset))).get();
                    }
                }
            } else {
                /**
                 * 发送给全部成员
                 */
                clientChannel.write(ByteBuffer.wrap((
                        CrazyStromeProtocol.MAG_ROUND + content + CrazyStromeProtocol.MAG_ROUND
                ).getBytes(charset))).get();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            jta.append("服务器出错或网络连接中断..\n");
        } catch (InterruptedException e) {
            e.printStackTrace();
            jta.append("服务器出错或网络连接中断..\n");
        }
    }

    private void connect() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            clientChannel = AsynchronousSocketChannel.open();
            clientChannel.connect(new InetSocketAddress("127.0.0.1", SERVER_PORT)).get();
            jta.append("---与服务器连接成功---\n");
            buffer.clear();
            clientChannel.read(buffer, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    buffer.flip();
                    String content = charset.decode(buffer).toString();
                    handleReceiveMsg(content.trim());
                    buffer.clear();
                    clientChannel.read(buffer, null, this);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.out.println("数据读取失败: " + exc);
                }
            });
        } catch (IOException e) {
            jta.append("服务器出错或网络连接中断..\n");
            e.printStackTrace();
        } catch (ExecutionException ee) {
            jta.append("服务器出错或网络连接中断..\n");
            ee.printStackTrace();
        } catch (InterruptedException e) {
            jta.append("服务器出错或网络连接中断..\n");
            e.printStackTrace();
        }
    }
    private void handleReceiveMsg(String content) {
        System.out.println(content);
        switch (content) {
            case CrazyStromeProtocol.NAME_REP :
                jta.append("用户已登陆，请登录其他用户!\n");
                break;
            case CrazyStromeProtocol.NAME_NOT_EXIST:
                jta.append("当前用户名不存在!\n");
                break;
            case CrazyStromeProtocol.LOGIN_FAILED:
                jta.append("登录失败，请检查密码!\n");
                break;
            case CrazyStromeProtocol.LOGIN_SUCCESS:
                jta.append("登陆成功...\n");
                mainWin.setTitle("当前用户: " + name);
                break;
            case CrazyStromeProtocol.REGISTER_FAILED:
                jta.append("注册失败\n");
                break;
            case CrazyStromeProtocol.REGISTER_SUCCESS:
                jta.append("注册成功\n");
                mainWin.setTitle("当前用户: " + name);
                break;
            case CrazyStromeProtocol.QUERY_FAIL:
                jta.append("查询失败\n");
                break;
            case CrazyStromeProtocol.DEL_SUCCESS:
                jta.append("数据库删除成功\n");
                break;
            case CrazyStromeProtocol.DEL_FAIL:
                jta.append("数据库删除失败\n");
                break;
            default:
                jta.append(content + "\n");
                break;
        }
    }
    public static void main(String[] args) {
        AIOClient client = new AIOClient();
        client.init();
        client.connect();
    }
}
