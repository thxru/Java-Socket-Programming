package tcp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class GradeServer {

    //Server port
    private final int port;

    // Connected userName set
    private final Set<String> userNames = new HashSet<>();

    //Communication threads for each user
    private final Set<GradeServer.UserThread> userThreads = new HashSet<>();

    public GradeServer(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("tcp.GradeServer is listening on port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected: " + socket.toString());

                GradeServer.UserThread newUser = new GradeServer.UserThread(socket);
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

        GradeServer server = new GradeServer(port);
        server.execute();
    }

    /**
     * Delivers a message from one user to others (broadcasting)
     */
    void broadcast(String message, GradeServer.UserThread excludeUser) {
        for (GradeServer.UserThread aUser : userThreads) {
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
    void removeUser(String userName, GradeServer.UserThread aUser) {
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
        private final GradeServer server;
        private PrintWriter writer;


        public UserThread(Socket socket) {
            this.socket = socket;
            this.server = GradeServer.this;
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

                // String serverMessage = "New user connected: " + username;
                // server.broadcast(serverMessage, this);
                this.sendMessage("Enter registration No");
                String regNo = reader.readLine();

                this.sendMessage("Enter degree program");
                String degPro = reader.readLine();

                String clientMessage="c";
                do {
                    this.sendMessage("Enter marks of 3 subjects \nEnter mark of subject A ");
                    String subA = reader.readLine();
                    this.sendMessage("Enter mark of subject B");
                    String subB = reader.readLine();
                    this.sendMessage("Enter mark of subject C");
                    String subC = reader.readLine();


                    try {
                        GradeCalculator gradeCalculator = new GradeCalculator(Integer.parseInt(subA),Integer.parseInt(subB),Integer.parseInt(subC));
                        String a ="Your grade: " + gradeCalculator.getGrade() +" \n" + " Name: "+username+", RegNo: "+regNo+", Degree program: "+degPro;
                        this.sendMessage(a);

                    } catch (Exception e) {
                        this.sendMessage("Invalid subject marks");
                    }

                    this.sendMessage("Enter C to continue! you can enter 3 more subjects again or say 'quit' to exit!");

                    clientMessage = reader.readLine();

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

class GradeCalculator{

    double subA;
    double subB;
    double subC;
    GradeCalculator(double subA,double subB, double subC){

        this.subA = subA;
        this.subB = subB;
        this.subC = subC;
    }

    double getAverage(){
        return (subA+subB+subC)/3;
    }

    String getGrade(){

        double ave = getAverage();

        if (ave >=85)
            return "A+";
        if (ave>=75)
            return "A";
        if (ave>=70)
            return "A-";
        if (ave>=65)
            return "B+";
        if (ave>=60)
            return "B";
        if (ave>=55)
            return "B-";
        if (ave>=50)
            return "C+";
        if (ave>=40)
            return "C";
        else
            return "D";
    }
}