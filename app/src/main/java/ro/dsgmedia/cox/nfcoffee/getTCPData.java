package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 31-May-17.
 */

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;


public class getTCPData {
    public static final String SERVER_IP = "192.168.4.1";
    public static final int SERVER_PORT = 31415;
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    //private PrintWriter mBufferOut;
    private DataOutputStream mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public getTCPData(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    /**
     * Sends a byte array
     *
     * @param message is the char array
     */
    public void sendBytes(byte[] message) {
        if (mBufferOut != null) {
            try{
                mBufferOut.write(message, 0, 8);
                mBufferOut.flush();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {
        mRun = false;

        if (mBufferOut != null) {
            try {
                mBufferOut.flush();
                mBufferOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            Log.e("TCP Client", "C: Connecting to ... " + serverAddr);

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVER_PORT);

            try {

                //sends the message to the server
                mBufferOut = new DataOutputStream(socket.getOutputStream());
                //mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // send current date
                Calendar c = Calendar.getInstance();
                short cYear = (short)c.get(Calendar.YEAR);
                char cMonth = (char)c.get(Calendar.MONTH);
                char cDay = (char)c.get(Calendar.DAY_OF_MONTH);
                short cCheckSum = (short)(cYear + (short)cMonth + (short)cDay);

                byte[] secretKey = {0, 1, 2, 3, 4, 5, 6, 7};
                secretKey[0] = (byte) ((cCheckSum >> 8) & 0xFF);
                secretKey[1] = (byte)(cCheckSum & 0xFF);
                secretKey[2] = (byte)((cYear >> 8) & 0xFF);
                secretKey[3] = (byte)(cYear & 0xFF);
                secretKey[4] = (byte)(cMonth & 0xFF);
                secretKey[5] = (byte)(cDay & 0xFF);
                secretKey[6] = secretKey[1];
                secretKey[7] = secretKey[0];
                sendBytes(secretKey);
                //sendMessage(Constants.LOGIN_NAME);

                while (mRun) {

                    mServerMessage = mBufferIn.readLine();

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(mServerMessage);
                    }

                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}
