import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class newServer {
    private int port = 14477;
    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private ArrayList<ClientThread> clients;
    public static void main(String[] args){
        new newServer();
    }

    public newServer(){
        try {
            serverStart(port);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    class ServerThread extends Thread{
        private ServerSocket serverSocket;
        public ServerThread(ServerSocket serverSocket){
            this.serverSocket = serverSocket;
        }
        public void run(){
            while(true){
                try{
                    Socket socket = serverSocket.accept();
                    ClientThread client = new ClientThread(socket);
                    client.start();
                    clients.add(client);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //为一个客户端服务的进程
    class ClientThread extends Thread{
        private Socket socket;
        //private BufferedReader reader;
        //private PrintWriter writer;
        private User user;
        private DataOutputStream os;
        private DataInputStream is;

        public User getUser() {
            return user;
        }

        public ClientThread(Socket socket){
            try {
                this.socket = socket;
                os = new DataOutputStream(socket.getOutputStream());
                is = new DataInputStream(socket.getInputStream());

                //reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //writer = new PrintWriter(socket.getOutputStream());
                //String inf = is.readUTF();
                //byte[] getInf = getKey();
                //String inf = new String(getInf);

                //StringTokenizer stringTokenizer = new StringTokenizer(inf,"@");
                user = new User(is.readUTF(), is.readUTF());
                //is.read(user.publickey);

                user.publicKey = user.byteToKey(getKey());
                System.out.println(user.getName() + user.getIp() + "与服务器连接成功");
                os.writeUTF(user.getName() + user.getIp() + "与服务器连接成功");

                if(clients.size()>0){
                    os.writeUTF("USERLIST");
                    os.writeInt(clients.size());
                    for(int i = clients.size() - 1; i>=0;i--){
                        os.writeUTF(clients.get(i).getUser().getName());
                        os.writeUTF(clients.get(i).getUser().getIp());
                    }

                    sendKey(os,clients.get(0).getUser().publicKey.getEncoded());
                }

                if(clients.size()>0){
                    clients.get(0).os.writeUTF("ADD");
                    clients.get(0).os.writeUTF(user.getName());
                    clients.get(0).os.writeUTF(user.getIp());

                    sendKey(clients.get(0).os,user.publicKey.getEncoded());
                }

            }catch (Exception e){
                e.printStackTrace();
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

        public void sendKey(DataOutputStream os,byte[] key){
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

        @SuppressWarnings("deprecation")
        public void run(){
            while (true){
                try{
                    String message = is.readUTF();
                    //byte[] getMessage = getKey();
                    //String message = new String(getMessage);
                    if(message.equals("CLOSE")){
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            clients.get(i).os.writeUTF("DELETE");
                            clients.get(i).os.writeUTF(user.getName());
                            //clients.get(i).getWriter().flush();
                        }
                        is.close();
                        os.close();
                        socket.close();
                        System.out.println(user.getName() + user.getIp() + "已退出");
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            if (clients.get(i).getUser() == user) {
                                ClientThread temp = clients.get(i);
                                clients.remove(i);// 删除此用户的服务线程
                                temp.stop();// 停止这条服务线程
                                return;
                            }
                        }

                    }else if(message.equals("MESSAGE")){

                        dispatcherMessage(is.readUTF(),is.readUTF());// 转发消息
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        public void dispatcherMessage(String username,String Message){

                for (int i = 0; i<clients.size(); i++) {
                    try{
                        clients.get(i).os.writeUTF("MESSAGE");
                        clients.get(i).os.writeUTF(username+" : ");
                        System.out.println(Message);

                        clients.get(i).os.writeUTF(Message);
                        //clients.get(i).sendKey(clients.get(i).os,getMessage);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    //clients.get(i).getWriter().flush();
                }
        }
    }


    public void serverStart(int port) throws Exception{
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket);
            serverThread.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
