package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    //static final String REMOTE_PORT0 = "11108";
    //static final String REMOTE_PORT1 = "11112";
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    private  Uri mUri;
    private static int i=0;
    private  ContentValues mContentValues;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";


    //private final ContentResolver mContentResolver;
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final TextView tv = (TextView) findViewById(R.id.textView1);

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
         final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e("Server", "Can't create a ServerSocket");
            return;
        }


    /*
     * TODO: Use the TextView to display your messages. Though there is no grading component
     * on how you display the messages, if you implement it, it'll make your debugging easier.
     */
//        TextView tv = (TextView) findViewById(R.id.textView1);
        //tv.setMovementMethod(new ScrollingMovementMethod());

    /*
     * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
     * OnPTestClickListener demonstrates how to access a ContentProvider.
     */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()

                ));

    /*
     * TODO: You need to register and implement an OnClickListener for the "Send" button.
     * In your implementation you need to get the message from the input box (EditText)
     * and send it to other AVDs.
     */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
                                                          public void onClick (View v){
                                                              String msg = editText.getText().toString();
                                                              editText.setText("");
                                                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,myPort);
//                                                              return true;
                                                                putValues(msg);
                                                          }
                                                      }

        );
    }

    class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while (true) {


                    Socket s1 = serverSocket.accept();
                    DataInputStream dataInputStream = new DataInputStream(s1.getInputStream());

                    String s = dataInputStream.readUTF();
                    DataOutputStream dataOutputStream = new DataOutputStream(s1.getOutputStream());
                   dataOutputStream.writeUTF("PA1-OK");
                    dataOutputStream.close();
                    s1.close();
                    publishProgress(s);



                }

            } catch (IOException e) {
                Log.e("ServerTask", "ClientTask socket IOException");

            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();

            final TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");

            tv.append("\n");

           putValues(strReceived);
        }
    }
    public  void putValues(String string){

        mContentValues=new ContentValues();
        mContentValues.put(KEY_FIELD,Integer.toString(i++));
        mContentValues.put(VALUE_FIELD,string);

        getContentResolver().insert(mUri,mContentValues);

    }
    class ClientTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                for (String str : REMOTE_PORTS) {
                    if (str.equals(msgs[1])){

                        continue;
                    }


                    Log.d("MSGS",msgs[0]);
                    Log.d("str",str);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(str));

                    String msgToSend = msgs[0];

                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    OutputStream out = socket.getOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(out);

                    dataOutputStream.writeUTF(msgToSend);

                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    String s1 = dataInputStream.readUTF();
                    //s1.equals("PA1-OK")


                    socket.close();
                }
            } catch (UnknownHostException e) {
                //Log.e("Client", "ClientTask UnknownHostException");
                e.printStackTrace();
            } catch (IOException e) {
                 e.printStackTrace();
            }

            return null;
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}