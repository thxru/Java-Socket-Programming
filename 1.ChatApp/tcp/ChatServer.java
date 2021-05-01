package tcp;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    //Server port
    private final int port;

    // Connected userName set
    private final Set<String> userNames = new HashSet<>();

    //Communication threads for each user
    private final Set<UserThread> userThreads = new HashSet<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("tcp.ChatServer is listening on port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected: " + socket.toString());

                UserThread newUser = new UserThread(socket);
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
    void broadcast(String message, UserThread excludeUser) {
        for (UserThread aUser : userThreads) {
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
    void removeUser(String userName, UserThread aUser) {
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
        private final ChatServer server;
        private PrintWriter writer;

        public UserThread(Socket socket) {
            this.socket = socket;
            this.server = ChatServer.this;
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

                String indexNo = reader.readLine();
                server.addUserName(indexNo);

                String serverMessage = "New user connected: " + indexNo;
                server.broadcast(serverMessage, this);

                String clientMessage;

                do {
                    clientMessage = reader.readLine();
                    serverMessage =  clientMessage + " [" + indexNo + "]";
                    server.broadcast(serverMessage, this);

                } while (!clientMessage.equals("quit"));

                server.removeUser(indexNo, this);
                socket.close();

                serverMessage = indexNo + " has quited.";
                server.broadcast(serverMessage, this);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

//        /**
//         * Sends a list of online users to the newly connected user.
//         */
//        void printUsers() {
//            if (server.hasUsers()) {
//                writer.println("Connected users: " + server.getUserNames());
//            }
//        }

        /**
         * Sends a message to the client.
         */
        void sendMessage(String message) {
            writer.println(message);
        }
    }
}