package tcp;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class CalculatorClient {

    private final String hostname;
    private final int port;
    private String userName;

    public CalculatorClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void execute() {
        try {
            Socket socket = new Socket(hostname, port);

            System.out.println("Connected to the Calculator Server:" + socket);

            new CalculatorClient.ReadThread(socket).start();
            new CalculatorClient.WriteThread(socket).start();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
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
            System.out.println("Error: Use 'java tcp.CalculatorClient <host> <port>'");
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
        private PrintWriter writer;
        private final Socket socket;
        private final CalculatorClient client;

        public WriteThread(Socket socket) {
            this.socket = socket;
            this.client = CalculatorClient.this;

            try {
                OutputStream output = socket.getOutputStream();
                writer = new PrintWriter(output, true);
            } catch (IOException ex) {
                System.out.println("Error getting output stream: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        public void run() {

            Console console = System.console();

            String username = console.readLine("\n ### Welcome to Calculator Server ### \n Enter your Nickname to continue: ");
            client.setUserName(username);
            writer.println(username);

            String text;

            do {
                text = console.readLine();
                writer.println(text);

            } while (!text.equals("quit"));

            try {
                socket.close();
            } catch (IOException ex) {

                System.out.println("Error writing to server: " + ex.getMessage());
            }
        }
    }

    /**
     * Reds the input message
     */
    class ReadThread extends Thread {
        private BufferedReader reader;

        public ReadThread(Socket socket) {

            try {
                InputStream input = socket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(input));
            } catch (IOException ex) {
                System.out.println("Error getting input stream: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        public void run() {
            while (true) {
                try {
                    String response = reader.readLine();
                    System.out.print("server> "+response+"\n");
                    System.out.print(CalculatorClient.this.getUserName()+"> ");

                } catch (IOException ex) {
                    System.out.println("Error reading from server: " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }
}
