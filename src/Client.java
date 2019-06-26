import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class Client{

    private JFrame frame;
    private JList userList;
    private JTextArea textArea;
    private JTextField textField;
    private JTextField txt_hostIp;
    private JTextField txt_name;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;
    private JPanel northPanel;
    private JPanel southPanel;
    private JScrollPane rightScroll;
    private JScrollPane leftScroll;
    private JSplitPane centerSplit;

    private DefaultListModel listModel;
    private boolean isConnected = false;

    private boolean ready = false;
    private byte[] salt = new byte[]{62,34,-11,-31,124,114,70,83,37,-96,-61,98,105,-17,87,34,1,-51,
            64,-50,-68,72,-107,27,-106,4,-82,89,-65,31,-15,58,40,-113,28,-32,104,-12,-57,-95,
            -14,22,82,-69,32,-13,-95,125,97,-29,20,124,76,-127,-30,-21,-65,-105,-117,51,-100,-11
            ,106,-99};
    private String password;
    private User ClientUser = new User();
    private Socket socket;
    private MessageThread messageThread;// 负责接收消息的线程
    private Map<String, User> onLineUsers = new HashMap<String, User>();// 所有在线用户
    private InputStream inputStream;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] byteKey;
    PBECoder pbe = new PBECoder();
    // 主方法,程序入口
    public static void main(String[] args) {
        new Client();
    }

    // 执行发送
    public void send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "还没有连接服务器，无法发送消息！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String message = textField.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if(ready){
                //String encryptData = pbe.byteArrayToStr(pbe.encrypt(message.getBytes(), password, salt));
                sendMessage("MESSAGE");
                sendMessage(frame.getTitle());
                sendMessage(ClientUser.encryptAndSendMessage(message));
                //salt = pbe.encrypt(salt,password,salt);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        textField.setText(null);
    }

    // 构造方法
    public Client() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setForeground(Color.blue);
        textField = new JTextField();
        //txt_hostIp = new JTextField("129.28.157.193");
        txt_hostIp = new JTextField("127.0.0.1");
        txt_name = new JTextField("");
        btn_start = new JButton("连接");
        btn_stop = new JButton("断开");
        btn_send = new JButton("发送");
        listModel = new DefaultListModel();
        userList = new JList(listModel);

        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 5));
        northPanel.add(new JLabel("服务器IP"));
        northPanel.add(txt_hostIp);
        northPanel.add(new JLabel("姓名"));
        northPanel.add(txt_name);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("连接信息"));

        rightScroll = new JScrollPane(textArea);
        rightScroll.setBorder(new TitledBorder("消息显示区"));
        leftScroll = new JScrollPane(userList);
        leftScroll.setBorder(new TitledBorder("在线用户"));
        southPanel = new JPanel(new BorderLayout());
        southPanel.add(textField, "Center");
        southPanel.add(btn_send, "East");
        southPanel.setBorder(new TitledBorder("写消息"));

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,
                rightScroll);
        centerSplit.setDividerLocation(100);

        frame = new JFrame("客户机");
        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        frame.add(southPanel, "South");
        frame.setSize(600, 400);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(true);

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                send();
            }
        });

        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        btn_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;
                if (isConnected) {
                    JOptionPane.showMessageDialog(frame, "已处于连接上状态，不要重复连接!",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    port = 14477;
                    String hostIp = txt_hostIp.getText().trim();
                    String name = txt_name.getText().trim();
                    if (name.equals("") || hostIp.equals("")) {
                        throw new Exception("姓名、服务器IP不能为空!");
                    }
                    boolean flag = connectServer(port, hostIp, name);
                    if (flag == false) {
                        throw new Exception("与服务器连接失败!");
                    }
                    frame.setTitle(name);
                    listModel.addElement(name);
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isConnected) {
                    JOptionPane.showMessageDialog(frame, "已处于断开状态，不要重复断开!",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    boolean flag = closeConnection();
                    if (flag == false) {
                        throw new Exception("断开连接发生异常！");
                    }
                    JOptionPane.showMessageDialog(frame, "成功断开!");
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    closeConnection();
                }
                System.exit(0);
            }
        });
    }

    public boolean connectServer(int port, String hostIp, String name) {
        // 连接服务器
        try {
            socket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
            //writer = new PrintWriter(socket.getOutputStream());
            inputStream = socket.getInputStream();
            //reader = new BufferedReader(new InputStreamReader(inputStream));
            os = new DataOutputStream(socket.getOutputStream());
            is = new DataInputStream(inputStream);

            sendMessage(name);
            sendMessage(socket.getLocalAddress().toString());
            // 开启接收消息的线程
            messageThread = new MessageThread(is, textArea);
            messageThread.start();
            isConnected = true;

            ClientUser.generateKeys();
            sendKey(ClientUser.publicKey.getEncoded());
            //os.write(ClientUser.getPublicKey().getEncoded());
            return true;
        } catch (Exception e) {
            textArea.append("与端口号为：" + port + "    IP地址为：" + hostIp
                    + "   的服务器连接失败!" + "\r\n");
            isConnected = false;// 未连接上
            return false;
        }
    }

    public byte[] getKey(){
        try{
            byte[] finished = "finished".getBytes(StandardCharsets.UTF_8);
            byte[] send = new byte[1024];
            for (int i = 0; i < finished.length; i++) {
                send[i] = finished[i];
            }
            byte[] buffer = new byte[1024];
            byte[] key = new byte[1024];
            ByteArrayOutputStream fos = new ByteArrayOutputStream();
            int flag = -1;
            while((flag = is.read(buffer)) != -1 && !new String(buffer,
                    StandardCharsets.UTF_8).equals(new String(send, StandardCharsets.UTF_8))){
                fos.write(buffer);
                buffer = new byte[1024];
            }
            key = fos.toByteArray();
            fos.close();
            return key;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void sendKey(byte[] key){
        try{
            BufferedInputStream bf = new BufferedInputStream(new ByteArrayInputStream(key));
            byte[] buffer = new byte[1024];
            int flag = -1;
            while ((flag = bf.read(buffer)) != -1) {
                os.write(buffer);
            }
            byte[] finished = "finished".getBytes(StandardCharsets.UTF_8);
            byte[] send = new byte[1024];
            for (int i = 0; i < finished.length; i++) {
                send[i] = finished[i];
            }
            os.write(send);
            os.flush();
            bf.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void sendMessage(String message){
        try {
            os.writeUTF(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public synchronized boolean closeConnection() {
        try {
            sendMessage("CLOSE");
            listModel.removeAllElements();
            messageThread.stop();
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            isConnected = true;
            return false;
        }
    }


    class MessageThread extends Thread {
        //private BufferedReader reader;
        private DataInputStream is;
        private JTextArea textArea;

        public MessageThread(DataInputStream is, JTextArea textArea) {
            //this.reader = reader;
            this.is = is;
            this.textArea = textArea;
        }

        // 被动的关闭连接
        public synchronized void closeCon() throws Exception {
            // 清空用户列表
            listModel.removeAllElements();
            // 被动的关闭连接释放资源
            if (is != null) {
                is.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;// 修改状态为断开
        }

        public void run() {
            while (true) {
                try {
                    String message = is.readUTF();
//                    byte[] getMessage = getKey();
                    if (message.equals("CLOSE"))// 服务器已关闭命令
                    {
                        textArea.append("服务器已关闭!\r\n");
                        closeCon();// 被动的关闭连接
                        return;// 结束线程
                    } else if (message.equals("ADD")) {// 有用户上线更新在线列表
                        String username = "";
                        String userIp = "";
                        if ((username = is.readUTF()) != null
                                && (userIp = is.readUTF()) != null) {
                            //生成用户时
                            User user = new User(username, userIp);
                            onLineUsers.put(username, user);
                            listModel.addElement(username);
                            textArea.append(username+userIp + "上线\r\n");
                            //ObjectInput is = new ObjectInputStream(inputStream);
                            //byte[] byteKey = new byte[1024];
                            //is.read(byteKey);
                            ClientUser.receivedPublicKey = user.byteToKey(getKey());
                            ClientUser.generateCommonSecretKey();
                            password = ClientUser.stringToAscii(ClientUser.byteArrayToStr(ClientUser.secretKey));
                            ready = true;
                        }
                    } else if (message.equals("DELETE")) {// 有用户下线更新在线列表
                        String username = is.readUTF();
                        User user = (User) onLineUsers.get(username);
                        ready = false;
                        textArea.append(user.getName()+user.getIp()+"下线\r\n");
                        onLineUsers.remove(user);
                        listModel.removeElement(username);

                    } else if (message.equals("USERLIST")) {// 加载在线用户列表

                        int size = is.readInt();
                        String username = null;
                        String userIp = null;
                        for (int i = 0; i < size; i++) {
                            username = is.readUTF();
                            userIp = is.readUTF();
                            User user = new User(username, userIp);

                            onLineUsers.put(username, user);
                            listModel.addElement(username);
                            ClientUser.receivedPublicKey = user.byteToKey(getKey());
                            ClientUser.generateCommonSecretKey();
                            password = ClientUser.stringToAscii(ClientUser.byteArrayToStr(ClientUser.secretKey));
                            ready = true;
                        }

                    }
                    else if(message.equals("MESSAGE")){// 普通消息
                        if(ready){
                            textArea.append(is.readUTF());
                            String newMessage = ClientUser.receiveAndDecryptMessage(is.readUTF());
                            textArea.append(newMessage + "\r\n");
                          }
                        else {
                            textArea.append(is.readUTF());
                            textArea.append(is.readUTF()+"读取消息失败" + "\r\n");
                        }

                    }
                    else {
                        textArea.append(message + "\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}