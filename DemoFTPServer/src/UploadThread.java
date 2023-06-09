import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;

public class UploadThread extends Thread{

    static final int maxDataLimit = 4096;
    private final int port;
    private final String filename;
    private final String rootDirectory;

    UploadThread(int port, String filename){
        this.port = port;
        this.filename = filename;
        this.rootDirectory = System.getProperty("user.dir");
    }

    @Override
    public void run() {
        try(Socket socket = new Socket("localhost", port)){
            System.out.println("Connected to HTTP server localhost:" + port);
            OutputStream outputStream = socket.getOutputStream();

            //first check if the file exist...
            File file = new File(rootDirectory + "/client/" + filename);

            outputStream.write(("UPLOAD " + file.getName() + "\r\n").getBytes());
            outputStream.flush();

            sendFile(new DataOutputStream(outputStream), file);
            outputStream.close();
        }catch (NullPointerException e){
            System.out.println("File does not exist");
        }catch (IOException e){
            System.out.println("Socket Connection Failed");
        }

    }

    private void sendFile(DataOutputStream dataOutputStream, File file) throws IOException {
        int bytes = 0;

        FileInputStream fileInputStream = new FileInputStream(file);

        dataOutputStream.writeLong(file.length());
        System.out.println(file.length());
        byte[] buffer = new byte[maxDataLimit];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
        dataOutputStream.close();
    }
}
