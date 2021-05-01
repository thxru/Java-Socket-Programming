package udp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;



public class ChatServer {

    //Server port
    private final int port;

    private final Set<UDPClient> clients = new HashSet<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void execute() {

        try {
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("udp.ChatServer is listening on port: " + port);

            while (true) {
                DatagramPacket response = new DatagramPacket(new byte[400], 400);
                socket.receive(response);

                UDPClient udpClient = new UDPClient(response.getAddress(), response.getPort(), socket);

                if(!clients.contains(udpClient))
                    System.out.println("New user connected : "+ udpClient);

                clients.add(udpClient);

                InputStream input = new DataInputStream(new ByteArrayInputStream(response.getData(), response.getOffset(), response.getLength()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                broadcast(reader.readLine(),udpClient).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Error: Use 'java tcp.ChatServer <port>'");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        ChatServer server = new ChatServer(port);
        server.execute();
    }

    /**
     * Delivers a message from one user to others (broadcasting)
     */
    Thread broadcast(String message,UDPClient excludeUser) throws IOException {

        Runnable task = () -> {
            for (UDPClient udpClient : clients) {
                if (!udpClient.equals(excludeUser)) {
                    try {
                        udpClient.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        return new Thread(task);
    }


    static class UDPClient{
        int port;
        InetAddress addr;
        DatagramSocket socket;
        String indexNo;

        UDPClient(InetAddress addr,int port,DatagramSocket socket){
            this.addr = addr;
            this.port=port;
            this.socket = socket;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UDPClient udpClient = (UDPClient) o;
            return port == udpClient.port && addr.equals(udpClient.addr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(port, addr);
        }

        @Override
        public String toString() {
            return "UDPClient{" +
                    "port=" + port +
                    ", addr=" + addr +
                    '}';
        }

        void sendMessage(String message) throws IOException {
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket response = new DatagramPacket(buffer, buffer.length,this.addr, this.port);
            socket.send(response);
        }
    }

}