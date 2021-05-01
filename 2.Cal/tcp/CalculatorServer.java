package tcp;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class CalculatorServer {

    //Server port
    private final int port;

    // Connected userName set
    private final Set<String> userNames = new HashSet<>();

    //Communication threads for each user
    private final Set<CalculatorServer.UserThread> userThreads = new HashSet<>();

    public CalculatorServer(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("tcp.CalculatorServer is listening on port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected: " + socket.toString());

                CalculatorServer.UserThread newUser = new CalculatorServer.UserThread(socket);
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

        CalculatorServer server = new CalculatorServer(port);
        server.execute();
    }

    /**
     * Delivers a message from one user to others (broadcasting)
     */
    void broadcast(String message, CalculatorServer.UserThread excludeUser) {
        for (CalculatorServer.UserThread aUser : userThreads) {
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
    void removeUser(String userName, CalculatorServer.UserThread aUser) {
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
        private final CalculatorServer server;
        private PrintWriter writer;

        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine;

        public UserThread(Socket socket) {
            this.socket = socket;
            this.server = CalculatorServer.this;
            engine = mgr.getEngineByName("JavaScript");
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

                String serverMessage = "New user connected: " + username;
                server.broadcast(serverMessage, this);

                this.sendMessage("Hi "+username+ "! Enter your expression");
                String clientMessage;


                do {
                    clientMessage = reader.readLine();

                    try {
                        String a ="Result= " + Calculator.eval(clientMessage) ;
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
