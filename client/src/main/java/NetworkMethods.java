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

    public static void openConnection(String rcvrAddress, int port){ //using a String for the address cuts down on imports in classes that call this
        try {
            sock = new Socket(rcvrAddress, port);
            objOut = new ObjectOutputStream(sock.getOutputStream());
            objIn = new ObjectInputStream(sock.getInputStream()); //construct the input stream after the output stream in case the server constructed the input stream first
        }catch(IOException ex){
            //error handling
        }
    }
    public static void closeConnection() throws IOException{
		//The server needs to know that the client is disconnecting
		objOut.writeUTF("disconnect");
		objOut.flush();
		sock.shutdownOutput();
        sock.close();
    }
    /*
    * Should be called in backend.Run(Message m)
    * Returns true if the send was successful
    * @param: m will be passed from backend
    * */
    public static boolean sendMessage(Message[] m){ //use an array parameter to send many messages at once
		boolean wasSuccess = false;
        try {
			if(m.length > 0) {
				objOut.reset();
            	objOut.writeUTF("message");
            	objOut.writeObject(m);
				wasSuccess = true;
			} else {
				objOut.writeUTF("none"); //use none instead of None for consistency
				wasSuccess = true;
			}
        }catch(IOException ex){
        	wasSuccess = false;
        }
		return wasSuccess;
    }

    public static void receiveMessage() { //not sure why you had a parameter here, Messages contain the sender
        try {
			Message received = (Message) objIn.readObject();
			backend.receiveMessage(received);
            /*String msg = objIn.readUTF();
            switch (msg) {
                case "none":
                    break;
                case "message":
                    Message[] received = (Message[]) objIn.readObject();
                    backend.receiveMessages(received);
                break;
            }*/
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
