import java.io.File;
import java.util.Scanner;

public class Client {
    public static final int serverPort = 5055;
    public static void main(String[] args){
        Scanner in = new Scanner(System.in);

        while(true){
            String filename = in.nextLine();
            if(filename.equalsIgnoreCase("quit")){
                break;
            }
            String rootDirectory = System.getProperty("user.dir");
            File file = new File(rootDirectory + "/client/" + filename);
            if(!file.exists()) {
                System.out.println("File does not exist");
                continue;
            }
            UploadThread uploadThread = new UploadThread(serverPort, filename);
            uploadThread.start();
        }
    }
}
