import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

public class ServerThread extends Thread{
    static final int maxDataLimit = 4096;
    Socket socket;
    String rootDirectory;
    String currentDirectory;
    ServerThread(Socket socket){
        this.socket = socket;
        this.rootDirectory = System.getProperty("user.dir");
    }

    @Override
    public void run() {
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String request = in.readLine();
            System.out.println(socket.getInetAddress() + ":\tReceived request: " + request );

            //get out if no request received
            if(request == null){
                System.out.println(socket.getInetAddress() + ":\tNo Request Received");
                socket.close();
                return;
            }

            String[] requestWords = request.split(" ");

            if(Objects.equals(requestWords[0], "GET") && requestWords.length == 3){
                response_to_GET(requestWords[1]);
            }else if(Objects.equals(requestWords[0], "UPLOAD") && requestWords.length == 2){
                response_to_UPLOAD(requestWords[1]);
            } else {
                System.out.println(socket.getInetAddress() + ":\tFailed to understand request");
            }

            socket.close();

        } catch (IOException e) {
            System.out.println(socket.getInetAddress() + ":\t" + e);
        }
    }

    private void response_to_UPLOAD(String filename) throws IOException{
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream(rootDirectory + "\\root\\upload\\" + filename);

        long size = dataInputStream.readLong();

        byte[] buffer = new byte[maxDataLimit];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;
        }

        System.out.println(socket.getInetAddress() + ":\t" + filename + " is Received");
        fileOutputStream.close();
    }

    private void response_to_GET(String url) throws IOException {

        //get directory pathname
        currentDirectory = url;
        if(url.equals("/")) currentDirectory = "";
        File dir = new File(rootDirectory + currentDirectory);

        if(dir.isFile()) download_file(dir); //client wants a file... send him that file
        else list_file(dir); //client wants to enter a file... send him the directory list
    }

    private void list_file(File dir) throws IOException {
        File[] files = dir.listFiles();
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        if(files == null){
            System.out.println(socket.getInetAddress() + ":\tRequested for a non existing file " + dir.getName());
            printWriter.write("HTTP/1.1 400 Not Found\r\n");
            printWriter.write("Server: Java HTTP Server: 1.0\r\n");
            printWriter.write("Date: " + new Date() + "\r\n");
            printWriter.write("Content-Type: text/plain\r\n");
            printWriter.write("\r\n");
            printWriter.write("Directory not found or not readable");
            printWriter.flush();
        }else{
            System.out.println(socket.getInetAddress() + ":\tShown content of " + dir.getName());
            printWriter.write("HTTP/1.1 200 OK\r\n");
            printWriter.write("Server: Java HTTP Server: 1.0\r\n");
            printWriter.write("Date: " + new Date() + "\r\n");
            printWriter.write("Content-Type: text/html\r\n");
            printWriter.write("\r\n");
            printWriter.write(file_list_to_html_list(files));
            printWriter.flush();
        }
        printWriter.close();
    }

    private String file_list_to_html_list(File[] files){
        boolean rootFlag = currentDirectory.equals("");
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><ul>");
        for(File file: files){
            // if in root directory only show root...
            // essentially a cheap filter in place of a better code design
            if(rootFlag && !file.getName().equals("root")) continue;
            sb.append("<li><a href=");
            sb.append(currentDirectory);
            sb.append("/");
            sb.append(file.getName());
            sb.append(">");
            sb.append(file.getName());
            sb.append("</a></li>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    private void download_file(File file) throws IOException {
        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        if(extension.equals("txt")) download_file_text();
        else if(extension.toLowerCase().matches("jpg|jpeg|png|gif|webp")) download_file_image(extension);
        else download_file_other();
    }

    private void download_file_other() throws IOException {
        File file = new File(rootDirectory + currentDirectory);
        System.out.println(socket.getInetAddress() + ":\tSending file " + file.getName());
        InputStream fileInputStream = new FileInputStream(file);

        OutputStream outputStream = socket.getOutputStream();

        outputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
        outputStream.write(("Content-Length: " + file.length() + "\r\n").getBytes());
        outputStream.write("Content-Type: application/octet-stream\r\n".getBytes());
        outputStream.write(("Content-Disposition: attachment; filename=\"" + file.getName() + "\"\r\n").getBytes());
        outputStream.write("\r\n".getBytes());

        byte[] buffer = new byte[maxDataLimit];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            outputStream.flush();
        }

        outputStream.flush();
        outputStream.close();
    }

    private void download_file_image(String filetype) throws IOException {
        InputStream fileInputStream = new FileInputStream(rootDirectory + currentDirectory);
        System.out.println(socket.getInetAddress() + ":\tSending image " + currentDirectory);

        // reading the image data
        ByteArrayOutputStream imageData = new ByteArrayOutputStream();
        byte[] buffer = new byte[maxDataLimit];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            imageData.write(buffer, 0, bytesRead);
        }

        // writing image data in an html using base64 encoding
        String html = "<html><body><img src='data:image/" + filetype + ";base64," +
                Base64.getEncoder().encodeToString(imageData.toByteArray()) + "'></body></html>";

        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Server: Java HTTP Server: 1.0\r\n");
        out.write("Date: " + new Date() + "\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");
        out.write(html);
        out.flush();

        // Close the input and output streams and the socket connection
        fileInputStream.close();
    }

    private void download_file_text() throws IOException {
        InputStream fileInputStream = new FileInputStream(rootDirectory + currentDirectory);
        System.out.println(socket.getInetAddress() + ":\tSending text " + currentDirectory);
        // Read the contents of the file into a string
        StringBuilder fileContents = new StringBuilder();
        Scanner scanner = new Scanner(fileInputStream);
        while (scanner.hasNextLine()) {
            fileContents.append(scanner.nextLine());
        }

        PrintWriter out = new PrintWriter(socket.getOutputStream());
        // Set the content type of the response to "text/html"
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Server: Java HTTP Server: 1.0\r\n");
        out.write("Date: " + new Date() + "\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");

        // Write the contents of the file to the response
        out.println(fileContents);

        // Close the input stream and flush the response
        fileInputStream.close();
        out.flush();
        out.close();
    }

}
