import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

/**
 * Created by Charles on 3/9/2016.
 */
public class NetworkMethods {
    static Socket sock;
    static InetAddress myInAddress;
    static int myPort;
    static ObjectInputStream objIn;
    static ObjectOutputStream objOut;
    static Backend backend = new Backend();

    public NetworkMethods(){
    }

    public static void openConnection(InetAddress rcvrAddress, int port){
        try {
            sock = new Socket(rcvrAddress, port);
            objIn = new ObjectInputStream(sock.getInputStream());
            objOut = new ObjectOutputStream(sock.getOutputStream());
        }catch(IOException ex){
            //error handling
        }
    }
    public static void closeConnection() throws IOException{
        sock.close();
    }
    /*
    * Should be called in backend.Run(Message m)
    * Returns true if the send was successful
    * @param: m will be passed from backend
    * */
    public static boolean sendMessage(Message m){
        boolean wasSuccess = false;
        try {
            if (m.text != null) {
                objOut.writeUTF("message");
                objOut.writeObject(m);
                wasSuccess = true;
            } else {
                objOut.writeUTF("None");
                wasSuccess = true;
            }
        }catch(IOException ex){
            wasSuccess = false;
        }
        return wasSuccess;
    }

    public static void receiveMessage(Client senderId) {
        try {
            String msg = objIn.readUTF();
            switch (msg) {
                case "none":
                    break;
                case "message":
                    Message[] received = (Message[]) objIn.readObject();
                    backend.receiveMessages(received);
                break;
            }
        } catch (IOException ex) {
            //error handling
        } catch (ClassNotFoundException ex){
            //error handling
        }
    }

    public static boolean sendFile(String fileToSend){
        boolean wasSuccess;
        try {
            if (fileToSend != null) {
                File myFile = new File(fileToSend);
                byte[] fileBytes = new byte[(int) myFile.length()];
                FileOutputStream fileOut = new FileOutputStream(myFile);
                fileOut.write(fileBytes, 0, fileBytes.length);
                wasSuccess = true;
            }else
                wasSuccess = true;

        }catch(Exception ex){
            wasSuccess = false;
        }
        return wasSuccess;
    }

    public static boolean receiveFile(){
        boolean wasSuccess = false;
        try {
            //ToDo: Add code to receive file.
        }catch (Exception ex){

        }
        return wasSuccess;
    }
}
