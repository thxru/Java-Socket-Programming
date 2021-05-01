package udp;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Chat client class
 */
public class CalculatorClient {
    private final String hostname;
    private InetAddress address;
    private final int port;
    private String userName;


    public CalculatorClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;

        try {
            this.address = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void execute() {
        try {
            DatagramSocket socket = new DatagramSocket();
            System.out.println("Started sending udp packets to host: "+hostname+ " on port: " +port );

            new ReadThread(socket).start();
            new WriteThread(socket).start();

        } catch (IOException ex) {
            System.out.println("IO Error: " + ex.getMessage());
        }

    }

    void setUserName(String userName) {
        this.userName = userName;
    }

    String getUserName() {
        return this.userName;
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Error: Use 'java tcp.ChatClient <host> <port>'");
            System.exit(1);
            return;
        };

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        CalculatorClient client = new CalculatorClient(hostname, port);
        client.execute();
    }

    /**
     * This class reads user's input and send it
     */
    class WriteThread extends Thread {
        //private PrintWriter writer;
        private final DatagramSocket socket;

        public WriteThread(DatagramSocket socket) {
            this.socket = socket;
        }

        public void run() {

            Console console = System.console();

            System.out.println("Welcome to Calculator Server");
            System.out.println("Enter an expression to get the result");
            System.out.println("Ex: 2*67+ (67-2)");

            String text;
            byte[] buffer;
            DatagramPacket request;


            do {
                text = console.readLine(">");

                buffer = text.getBytes(StandardCharsets.UTF_8);
                request = new DatagramPacket(buffer, buffer.length, CalculatorClient.this.address, CalculatorClient.this.port);
                try {
                    socket.send(request);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } while (!text.equals("quit"));

            socket.close();
        }
    }

    /**
     * Reds the input message
     */
    class ReadThread extends Thread {
        //private BufferedReader reader;
        DatagramSocket socket;

        public ReadThread(DatagramSocket socket) {
            this.socket= socket;
        }

        public void run() {
            while (true) {
                try {

                    byte[] buffer = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, CalculatorClient.this.address, CalculatorClient.this.port);
                    socket.receive(packet);

                    InputStream input = new DataInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    String response = reader.readLine();
                    System.out.print("\n< " + response +"\n>");

                } catch (IOException ex) {
                    System.out.println("Error reading from server: " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }
}