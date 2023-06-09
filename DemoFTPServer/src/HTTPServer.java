import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {
    private static final int PORT = 5055;
    public static void main(String[] args){
        ServerSocket HTTPServerSocket;
        File file = new File("log.txt");
        FileOutputStream fos;
        PrintStream ps;
        try{
            HTTPServerSocket = new ServerSocket(PORT);
            fos = new FileOutputStream(file);
            ps = new PrintStream(fos);
            System.setOut(ps);
        }catch (IOException e){
            System.out.println("Failed to create a server socket");
            System.out.println("Error msg: " + e);
            return;
        }
        while(true){
            System.out.println("Listening for Connection on port 5055...");
            server_loop(HTTPServerSocket);
        }
    }

    private static void server_loop(ServerSocket HTTPServerSocket){
        Socket socket;
        try{
            socket = HTTPServerSocket.accept();
        }catch (IOException e){
            System.out.println("Failed to connect to a client");
            System.out.println("Error msg: " + e);
            return;
        }
        System.out.println("Connection Established with: " + socket.getInetAddress());

        Thread serverThread = new ServerThread(socket);
        serverThread.start();
    }

}
