import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by Charles on 3/9/2016.
 */
public class TCPClient {
    static Socket sock;
    InetAddress inAddress;

    public static boolean sendMessage(InetAddress rcvrAddress, int port, String msg){
        boolean wasSuccess;
        try {
            if (rcvrAddress != null && port > 0 && msg != null) {
                sock = new Socket(rcvrAddress, port);
                DataOutputStream dataOut = new DataOutputStream(sock.getOutputStream());
                dataOut.writeUTF(msg);
                wasSuccess = true;
            } else
                wasSuccess = false;

        }catch(IOException ex){
            wasSuccess = false;
        }
        return wasSuccess;
    }

    public static String receiveMessage(InetAddress senderAddress, int port){
        String msg ="No message.";
        try{
            if (senderAddress != null && port > 0) {
                sock = new Socket(senderAddress, port);
                DataInputStream dataIn = new DataInputStream(sock.getInputStream());
                msg = dataIn.readUTF();
            }else{
                msg = "Unable to connect.";
            }

        }catch(EOFException ex){
            msg = "End of file error";
        } catch(IOException ex){
            msg = "I/O error has occurred";
        }
        return msg;
    }

    public static boolean sendFile(InetAddress rcvAddress, int port, String fileToSend){
        boolean wasSuccess;
        try {
            if (rcvAddress != null && port > 0 && fileToSend != null) {
                sock = new Socket(rcvAddress, port);
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
}
