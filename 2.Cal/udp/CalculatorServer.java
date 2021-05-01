package udp;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class CalculatorServer {

    //Server port
    private final int port;

    private final Set<UDPClient> clients = new HashSet<>();

    public CalculatorServer(int port) {
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

                try {
                    String result = "Result = "+Calculator.eval(reader.readLine());
                    sendMessageInNewThread(result,udpClient).start();
                }
                catch (Exception e){
                    sendMessageInNewThread("Invalid expression",udpClient).start();
                }

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

        CalculatorServer server = new CalculatorServer(port);
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

    Thread sendMessageInNewThread(String message,UDPClient udpClient){
        Runnable task = () -> {

            try {
                udpClient.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
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

class Calculator {

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }


            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    switch (func) {
                        case "sqrt":
                            x = Math.sqrt(x);
                            break;
                        case "sin":
                            x = Math.sin(Math.toRadians(x));
                            break;
                        case "cos":
                            x = Math.cos(Math.toRadians(x));
                            break;
                        case "tan":
                            x = Math.tan(Math.toRadians(x));
                            break;
                        default:
                            throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}