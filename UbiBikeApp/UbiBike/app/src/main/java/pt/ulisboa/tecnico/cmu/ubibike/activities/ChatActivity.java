package pt.ulisboa.tecnico.cmu.ubibike.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.ulisboa.tecnico.cmu.ubibike.R;
import pt.ulisboa.tecnico.cmu.ubibike.UbiApp;
import pt.ulisboa.tecnico.cmu.ubibike.data.DatabaseManager;
import pt.ulisboa.tecnico.cmu.ubibike.data.UserLoginData;
import pt.ulisboa.tecnico.cmu.ubibike.domain.User;
import pt.ulisboa.tecnico.cmu.ubibike.services.UserSynchronizeService;
import pt.ulisboa.tecnico.cmu.ubibike.services.WifiDirectService;

public class ChatActivity extends BaseDrawerActivity {

    public static final String TAG = "ubibike";
    private SimWifiP2pSocket mCliSocket = null;
    private String otherIP = null;
    private String otherUsername = null;
    private String username = null;

    private long lastUpdated = 0;

    @Override
    protected int getPosition() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        TextView chatShowView = (TextView) findViewById(R.id.chatTextShow);
        chatShowView.setMovementMethod(new ScrollingMovementMethod());



        final TextView pointsView = (TextView) findViewById(R.id.chatPointsInput);

        pointsView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    pointsView.setHint("");
                else
                    pointsView.setHint("Points");
            }
        });

        otherIP = (String) getIntent().getSerializableExtra("OtherIP");
        otherUsername = (String) getIntent().getSerializableExtra("OtherUsername");
        username = UbiApp.getInstance().getUser().getUsername();
        Log.d(TAG, "onCreate: otherIP:" + otherIP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WifiDirectService.getInstance().setChatListener(this);
        updateChat();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WifiDirectService.getInstance().setChatListener(null);
    }

    public void sendMessage(View view) {
        TextView inputView = (TextView) findViewById(R.id.chatTextInput);
        String input = inputView.getText().toString();
        if (input == null || input.equals("")){
            return;
        }
        inputView.setText("");
        String content = "[Protocol]MESSAGE " + username + " " + input;
        new SendMessageTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                content, otherIP);
        DatabaseManager.getInstance(this).insertMessage(username, input);
        updateChat();

    }

    public void updateChat() {
        Cursor res =  DatabaseManager.getInstance(this).getMessagesSinceTime(username, otherUsername, lastUpdated);
        lastUpdated = System.currentTimeMillis();
        Log.d(TAG, "updateChat: " + res.getCount());
        final TextView output = (TextView) findViewById(R.id.chatTextShow);
        if(res.moveToFirst()) {
            do {
                String sender = res.getString(res.getColumnIndex(DatabaseManager.USERNAME));
                String content = res.getString(res.getColumnIndex(DatabaseManager.CONTENT));
                final String line = "\n\n" + (sender.equals(username) ? "Me" : sender) + ": " + content;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        output.append(line);
                    }
                });

            } while (res.moveToNext());
        }
        /*
        final int scrollAmount = output.getLayout().getLineTop(output.getLineCount()) - output.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0) {
            output.scrollTo(0, scrollAmount);
        }
        else{
            output.scrollTo(0, 0);
        }*/
    }

    public void sendPoints(View view) {
        TextView textView = (TextView) findViewById(R.id.chatPointsInput);
        if (textView.getText() == null || textView.getText().toString().equals("Points")) {
            Toast.makeText(this, "Please insert the number of points to be sent", Toast.LENGTH_SHORT).show();
            return;
        }

        Long input = Long.parseLong(textView.getText().toString());

        textView.setText("");
        textView.setHint("Points");

        if (input <= 0) {
            Toast.makeText(this, "Please a positive number of points to be sent", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = UbiApp.getInstance().getUser();

        if (user == null) {return;} //should not happen

        long currentPoints = user.getPoints();

        if (input > currentPoints) {
            Toast.makeText(this, "Can't send more points than those you have, sorry", Toast.LENGTH_SHORT).show();
            return;
        }

        long newPoints = currentPoints - input;

        user.setPoints(newPoints);

        UserLoginData.setUser(this, user);

        String content = "[Protocol]POINTS " + username + " " + input;
        new SendMessageTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                content, otherIP);

        sendBroadcast(new Intent().setAction(UserSynchronizeService.SYNCHRONIZE_USER_INTENT));
        Toast.makeText(this, "Sent " + input + " points to " + otherUsername, Toast.LENGTH_SHORT).show();

        return;
    }

    public class SendMessageTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... msg) {
            Log.d(TAG, "SendMessageTask: sending message: <heheh>");
            try {
                mCliSocket = new SimWifiP2pSocket(msg[1],
                        Integer.parseInt(getString(R.string.port)));

                mCliSocket.getOutputStream().write((msg[0] + "\n").getBytes());
                BufferedReader sockIn = new BufferedReader(
                        new InputStreamReader(mCliSocket.getInputStream()));
                String response = sockIn.readLine();
                Log.d(TAG, "exchangeIdTask: received response: " + response);
                publishProgress(response, msg[1]);

                mCliSocket.close();


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mCliSocket = null;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected void onPostExecute(Void result) {
        }

    }
}
