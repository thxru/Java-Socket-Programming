package tcp;

import util.Calculator;
import util.RSA;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class SecureServer {

    //Server port
    private final int port;

    // Connected userName set
    private final Set<String> userNames = new HashSet<>();

    //Communication threads for each user
    private final Set<SecureServer.UserThread> userThreads = new HashSet<>();

    KeyPair serverKeyPair;

    public SecureServer(int port) {
        this.port = port;
        serverKeyPair = RSA.genKeyPair(2048);

    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("tcp.CalculatorServer is listening on port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected: " + socket.toString());

                SecureServer.UserThread newUser = new SecureServer.UserThread(socket);
                userThreads.add(newUser);
                newUser.start();

            }

        } catch (IOException ex) {
            System.out.println("Error in the server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Error: Use 'java tcp.CalculatorServer <port>'");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        SecureServer server = new SecureServer(port);
        server.execute();
    }

    /**
     * Delivers a message from one user to others (broadcasting)
     */
    void broadcast(String message, SecureServer.UserThread excludeUser) {
        for (SecureServer.UserThread aUser : userThreads) {
            if (aUser != excludeUser) {
                aUser.sendMessage(message);
            }
        }
    }

    /**
     * Stores username of the newly connected client.
     */
    void addUserName(String userName) {
        userNames.add(userName);
    }

    /**
     * When a client is disconnected, removes the associated username and UserThread
     */
    void removeUser(String userName, SecureServer.UserThread aUser) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(aUser);
            System.out.println("The user " + userName + " quited");
        }
    }

    Set<String> getUserNames() {
        return this.userNames;
    }

    /**
     * Returns true if there are other users connected (not count the currently connected user)
     */
    boolean hasUsers() {
        return !this.userNames.isEmpty();
    }


    /**
     * Class to manage each users connection
     */
    class UserThread extends Thread {
        private final Socket socket;
        private final SecureServer server;
        private PrintWriter writer;

        private PublicKey userPublicKey;


        public UserThread(Socket socket) {
            this.socket = socket;
            this.server = SecureServer.this;
        }

        /**
         * Start the thread
         */
        public void run() {
            try {
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                OutputStream output = socket.getOutputStream();
                writer = new PrintWriter(output, true);

                //printUsers();

                String username = reader.readLine();
                server.addUserName(username);


                this.sendMessage("Hi "+username+ "! Next message will send the server public key in base64 format. Use this to encrypt your messages!" +
                        "Encrypted message should be in base64 format. Then sever will decrypt the message");

                String serverPublicKeyInBase64 = RSA.getPublicKeyAsBase64Encoded(SecureServer.this.serverKeyPair.getPublic());
                this.sendMessage(serverPublicKeyInBase64);


                String clientMessage;


                do {
                    clientMessage = reader.readLine();

                    System.out.println("Server received encrypted message: "+ clientMessage);
                    byte[] decoded = Base64.getDecoder().decode(clientMessage);

                    byte[] decrypted;
                    try {
                        decrypted = RSA.decrypt(SecureServer.this.serverKeyPair.getPrivate(),decoded);
                    } catch (NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchPaddingException e) {
                        this.sendMessage("Decryption error");
                        e.printStackTrace();
                        continue;
                    }

                    try {
                        String original = new String(decrypted, StandardCharsets.UTF_8);
                        System.out.println("Decrypted message "+ original);
                        String a ="Result from encrypted expression= " + Calculator.eval(original) ;
                        this.sendMessage(a);
                    } catch (Exception e) {
                        this.sendMessage("Invalid expression");
                    }


                } while (!clientMessage.equals("quit"));

                server.removeUser(username, this);
                socket.close();


            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }



        /**
         * Sends a message to the client.
         */
        void sendMessage(String message) {
            writer.println(message);
        }
    }
}
