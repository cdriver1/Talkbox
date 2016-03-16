package talkbox.client;

import talkbox.lib.*;
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
    static Backend backend;

    public NetworkMethods(){
    }

    public static Client openConnection(String rcvrAddress, int port){ //using a String for the address cuts down on imports in classes that call this
        try {
            sock = new Socket(rcvrAddress, port);
			sock.setSoTimeout(250); //block on reads for this many ms
            objOut = new ObjectOutputStream(sock.getOutputStream());
            objIn = new ObjectInputStream(sock.getInputStream()); //construct the input stream after the output stream in case the server constructed the input stream first
			return (Client)objIn.readObject();
        }catch(IOException | ClassNotFoundException ex){
            //error handling
            ex.printStackTrace();
			return null;
        }
    }
    public static void closeConnection() throws IOException{
		//The server needs to know that the client is disconnecting
		objOut.writeUTF("disconnect");
		objOut.flush();
        sock.close();
    }
    /*
    * Should be called in backend.Run(Message m)
    * Returns true if the send was successful
    * @param: m will be passed from backend
    * */
    public static boolean sendMessage(Message[] m){ //use an array parameter to be consistent with receiveMessage
		boolean wasSuccess = false;
        try {
            objOut.writeUTF("message");
            objOut.writeUnshared(m);
			objOut.flush();
			wasSuccess = true;
        }catch(IOException ex){
        	wasSuccess = false;
        }
		return wasSuccess;
    }

    public static Message[] receiveMessage() { //not sure why you had a parameter here, Messages contain the sender
        try {
            return (Message[]) objIn.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            //error handling
        }
		return null;
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
