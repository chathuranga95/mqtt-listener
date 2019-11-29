package com.example.adlnotifier;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.adlnotifier.helpers.MqttHelper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    static NotificationManager mNotificationManager;
    MqttHelper mqttHelper;
    TextView dataReceived;
    ListView recentActivities;
    int recentItemCount = 10;
    String[] activityList = new String[recentItemCount];

    //push notifications to the notification area
    public static void pushNotification(String msg, Context context) {
        // TODO: 04/11/19 check on vibration
        //vibrator pattern
        long[] patt = new long[3];
        patt[0] = 200;
        patt[1] = 100;
        patt[2] = 200;

        // TODO: 04/11/19 set click event to fill the app's state either by local file memory or
        //  parameter parse
        //push msg to the notification area.
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,
                "ADL_NOTI_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(msg)
                .setContentText("Touch to open app")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setVibrate(patt)
                .setAutoCancel(true);
        Intent[] intents = new Intent[1];
        intents[0] = new Intent(context, MainActivity.class);
        PendingIntent PI = PendingIntent.getActivities(context, 0, intents, 0);
        mBuilder.setContentIntent(PI);
        mNotificationManager.notify(1, mBuilder.build());

    }

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataReceived = findViewById(R.id.dataReceived);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        recentActivities = findViewById(R.id.recentActivities);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("ADL_NOTI_CHANNEL",
                    "ADL_NOTIFICATIONS",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notify ADL critical messages");
            mNotificationManager.createNotificationChannel(channel);
        }

        startMqtt();
        startBackgroundService(); // TODO: 04/11/19 start only when app minimizes or exits

        getSupportActionBar().hide();

//        for(int i=0; i<11;i++){
//            updateDisplay("msg ".concat(Integer.toString(i)));
//        }
    }

    private void startBackgroundService() {
        Intent intent = new Intent(MainActivity.this, ADLListenerService.class);
        startService(intent);
        // TODO: 04/11/19 start as a foreground activity
//        ContextCompat.startForegroundService(MainActivity.this, intent);
    }

    private void startMqtt() {
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                String msg = mqttMessage.toString();
                Log.w("Debug", msg);
                updateDisplay(msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    public void updateDisplay(String msg){
        dataReceived.setText(msg);

        //shift list first
        int i = recentItemCount-2;
        while(i>=0){
            activityList[i+1] = activityList[i];
            i--;
        }

        //append the activity at the front
        activityList[0] = msg;

        //update the listview
        ArrayList<String> list = new ArrayList<>();
        for(int j= 1; j<recentItemCount;j++){
            list.add(activityList[j]);
        }

        if(msg.equals("Falling")){
            dataReceived.setBackgroundResource(R.color.warningColor);
        } else{
            dataReceived.setBackgroundResource(R.color.defaultBack);
        }


        ArrayAdapter adapter = new ArrayAdapter(this,
                R.layout.list_text, R.id.list_content, list);
        recentActivities.setAdapter(adapter);
    }
}
