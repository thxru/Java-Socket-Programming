package tcp;

import util.RSA;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SecureClient {

    private final String hostname;
    private final int port;
    private String userName;

    private PublicKey serverPubKey;

    public SecureClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void execute() {
        try {
            Socket socket = new Socket(hostname, port);

            System.out.println("Connected to the Calculator Server:" + socket);

            new SecureClient.ReadThread(socket).start();
            new SecureClient.WriteThread(socket).start();

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

        SecureClient client = new SecureClient(hostname, port);
        client.execute();
    }

    /**
     * This class reads user's input and send it
     */
    class WriteThread extends Thread {
        private PrintWriter writer;
        private final Socket socket;
        private final SecureClient client;

        public WriteThread(Socket socket) {
            this.socket = socket;
            this.client = SecureClient.this;

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

            String username = console.readLine("\n ### Welcome to Secure Calculator Server ### \n Enter your Nickname to continue: ");
            client.setUserName(username);
            writer.println(username);

            String text;

            do {
                text = console.readLine();
                String base64encoded;
                try {

                    base64encoded = Base64.getEncoder().encodeToString(RSA.encrypt(serverPubKey,text.getBytes(StandardCharsets.UTF_8)));
                    writer.println(base64encoded);

                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {

                    System.out.println("Error in encryption");
                    e.printStackTrace();
                }

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

            try {
                String response1 = reader.readLine();
                System.out.println("server >" +response1);

                String response2 = reader.readLine();

                byte[] bytes = Base64.getDecoder().decode(response2);
                serverPubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));

                System.out.println("Received RSA key in base64 \n Public key: ");
                System.out.println("server> " + response2);
                System.out.println("Saved server's public key");
                System.out.println("Enter an expression, This program will encrypt the expression with severs public key. In the server, " +
                        "expression is decrypted and send back the result");
                System.out.print(">");

            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    String response = reader.readLine();
                    System.out.print("server> "+response+"\n");
                    System.out.print(SecureClient.this.getUserName()+"> ");

                } catch (IOException ex) {
                    System.out.println("Error reading from server: " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }
}
