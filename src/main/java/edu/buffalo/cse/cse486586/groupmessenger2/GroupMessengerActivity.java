package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.ContentResolver;
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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.max;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {


    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static double initID = 0.0;
    private Uri mUri;
    private int i = 0;
    private String oPort;
    private String myPort;
    private String msgToSend;

    private ContentValues mContentValues;
    private static Set<Integer> deadAvd = new HashSet<Integer>();


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

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e("Server", "Can't create a ServerSocket");
            return;
        }


        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()

                ));

    /*
     * TODO: You need to register and implement an OnClickListener for the "Send" button.
     * In your implementation you need to get the message from the input box (EditText)
     * and send it to other AVDs.
     */


        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
                                                          public void onClick(View v) {
                                                              String msg = editText.getText().toString();
                                                              editText.setText("");
                                                              new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                                                              tv.append("\t" + msg);
                                                          }
                                                      }

        );
    }

    public void putValues(String string) {

        mContentValues = new ContentValues();
        mContentValues.put(KEY_FIELD, Integer.toString(i++));
        mContentValues.put(VALUE_FIELD, string);

        getContentResolver().insert(mUri, mContentValues);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    class Message implements Comparable {
        private String message;
        private Double mID;
        private Integer oPort;

        public Message(String m, Double id, Integer port) {
            this.message = m;
            this.mID = id;
            this.oPort = port;
        }

        public double getId() {
            return this.mID;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "message='" + message + '\'' +
                    ", mID=" + mID +
                    ", oPort=" + oPort +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            Message m = (Message) o;
            System.out.println("equals   " + m.message.equals(this.message));
            return m.message.equals(this.message);
        }

        @Override
        public int compareTo(Object m2) {
            Message m = (Message) m2;
            if (this.getId() > m.getId())
                return 1;
            else
                return -1;
        }

        @Override
        public int hashCode() {
            System.out.println("hash   " + this.message.hashCode());
            return this.message.hashCode();
        }
    }

    class MyNameComp implements Comparator<Message> {

        @Override
        public int compare(Message m1, Message m2) {
            if (m1.getId() > m2.getId())
                return 1;
            else
                return -1;

        }

    }

    class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            String s = null;
            List<Message> Confirmed = new ArrayList<Message>();
            List<Message> Proposed = new ArrayList<Message>();
            List<Message> Dead = new ArrayList<Message>();
            double sid = 0;

            try {

                while (true) {
                    Socket s1 = serverSocket.accept();

                    DataInputStream dataInputStream = new DataInputStream(s1.getInputStream());

                    s = dataInputStream.readUTF();
                    Log.e("SERVER- ID Recieved", s);
                    String[] rArr;
                    rArr = s.split(",");
                    String identifier = rArr[0];
                    String port = rArr[1];
                    String rid = rArr[2];
                    String msgRcvd = rArr[3];

                    DataOutputStream dataOutputStream = new DataOutputStream(s1.getOutputStream());

                    if (identifier.equals("0000")) {

                        sid = ++initID + 0.00001 * Integer.parseInt(myPort);
                        Message m1 = new Message(msgRcvd, sid, Integer.parseInt(port));
                        Proposed.add(m1);
                        Log.e("SERVER- ID Proposed", "" + m1);
                        System.out.println("Proposed Q" + Proposed);
                        dataOutputStream.writeDouble(sid);

                    } else if (identifier.equals("1111")) {
                        Message m1 = new Message(msgRcvd, Double.parseDouble(rid), Integer.parseInt(port));
                        Confirmed.add(m1);

                        Collections.sort(Confirmed, new MyNameComp());
                        Log.e("SERVER- FinalMsg + ID", m1.message + ":" + m1.mID);
                        System.out.println("Confirmed Q" + Confirmed);

                        initID = Math.max(Double.parseDouble(rid) + 1, initID);
                        dataOutputStream.writeUTF("OK");
                        Message m = null;
                        for (Message mp : Proposed) {
                            System.out.println("------->");
                            if (mp.message.equals(msgRcvd)) {
                                System.out.println("+++++++++>");
                                m = mp;
                            }
                        }
                        Proposed.remove(m);

                        System.out.println("Confirmed Q" + Confirmed);
                        System.out.println("Proposed Q" + Proposed);

                        for (Message mp : Proposed) {
                            System.out.println("size of deadavd" + deadAvd.size());
                            for (Integer i : deadAvd) {
                                System.out.println("PortVerify--------" + mp.oPort + ":" + i);
                                if (mp.oPort.compareTo(i) == 0) {
                                    System.out.println("AddDead");
                                    Dead.add(mp);
                                }
                            }

                            System.out.println("Confirmed Q" + Confirmed);
                            System.out.println("Proposed Q" + Proposed);
                        }

                        System.out.println("size of dead" + Dead.size());
                        for (Message msg : Dead) {
                            Log.e("DeadList Messages", msg.message + ":" + msg.oPort);
                            Proposed.remove(msg);
                        }

                        for (Message prop : Proposed) {
                            System.out.println("Proposed List Messages" + "" + prop.message);
                        }

                        if ((Proposed.isEmpty())) {
                            Collections.sort(Confirmed, new MyNameComp());
                            System.out.println("CSize" + Confirmed.size());
                            ListIterator lit = Confirmed.listIterator();
                            while (lit.hasNext()) {
                                Message msg = (Message) lit.next();
                                System.out.println("Proposed Q" + Proposed);
                                System.out.println("Confirmed Q" + Confirmed);
                                System.out.println("SERVER- SortedList" + msg.toString());
                                //Log.e("SERVER- SortedList", msg.toString());
                                publishProgress(msg.message);
                                lit.remove();
                            }

                        }

                    }

//                    System.out.println(Confirmed);
//                    System.out.println(Proposed);
                    dataOutputStream.close();
                    dataInputStream.close();
                    s1.close();

                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SERVER- ", "ClientTask socket IOException");
            }


            return null;
        }

        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0].trim();

            final TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");

            tv.append("\n");

            putValues(strReceived);
        }
    }

    class ClientTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            Double id = 0.0;
            double idRecieved = 0.0;
            double maxid = 0;
            msgToSend = msgs[0];
            oPort = msgs[1];
            Set<Double> count = new HashSet<Double>();
            for (String Port : REMOTE_PORTS) {
                Socket socket = new Socket();
                try {

                    Log.d("CLIENT- msgToSend", msgToSend);
                    Log.d("CLIENT- ToPort#", Port);
                    InetAddress addr = InetAddress.getByAddress(new byte[]{10, 0, 2, 2});
                    SocketAddress sockaddr = new InetSocketAddress(addr, Integer.parseInt(Port));
                    socket.connect(sockaddr, 10000);

                    Log.e("CLIENT- Socket Created", "Created");

                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    id = initID + 0.00001 * Integer.parseInt(Port);
                    String str = "0000" + "," + oPort + "," + id + "," + msgToSend;
                    dataOutputStream.writeUTF(str);
                    Log.e("CLIENT- ID sent ", "" + str);

                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    idRecieved = dataInputStream.readDouble();
                    count.add(idRecieved);


                    Log.e("CLIENT- id", " " + id);
                    initID = Math.max(Collections.max(count) + 1, initID);

                    Log.e("CLIENT- ID recived ", "" + idRecieved + "," + msgToSend);
                    Log.e("CLIENT- Max idRecieved", " " + count + ":::" + Collections.max(count));
                    Log.e("CLIENT- NewinitID", " " + initID);

                    dataOutputStream.close();
                    dataInputStream.close();
                    socket.close();

                } catch (Exception e) {

                    e.printStackTrace();
                    deadAvd.add(Integer.valueOf(Port));

                    for (Integer dead1 : deadAvd) {
                        System.out.println("AVD" + " " + dead1);
                    }
                }

            }
            maxid = Math.max(Collections.max(count), id);

            Log.e("CLIENT- New maxID", "" + maxid);
            System.out.println("Set " + count + "::::" + maxid);

            //initID = Math.max((int) id + 1, initID);


            for (String Port : REMOTE_PORTS) {
                try {
                    Socket socket1 = new Socket();
                    InetAddress addr = InetAddress.getByAddress(new byte[]{10, 0, 2, 2});
                    SocketAddress sockaddr = new InetSocketAddress(addr, Integer.parseInt(Port));
                    socket1.connect(sockaddr, 10000);

                    DataOutputStream dataOutputStream = new DataOutputStream(socket1.getOutputStream());
                    dataOutputStream.writeUTF("1111" + "," + oPort + "," + maxid + "," + msgToSend);
                    DataInputStream dataInputStream = new DataInputStream(socket1.getInputStream());
                    Log.e("CLIENT- FinalMsg + ID", msgToSend + " : " + maxid);

                    String w = dataInputStream.readUTF();
                    dataOutputStream.close();
                    dataInputStream.close();
                    socket1.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    deadAvd.add(Integer.valueOf(Port));
                    for (Integer dead1 : deadAvd) {
                        System.out.println("AVD" + " " + dead1);
                    }
                }
            }
            return null;
        }

    }
}