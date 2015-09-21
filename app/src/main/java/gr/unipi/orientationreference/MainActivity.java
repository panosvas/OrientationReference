package gr.unipi.orientationreference;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MainActivity extends Activity {

    private TextView view;
    private SensorManager sensorManager;
    private Sensor sensor;
    private float azimuth = -1;
    UDPServer udpServer = new UDPServer();
    DatagramSocket socket;// = new DatagramSocket(SERVER_PORT);
    private final static int SERVER_PORT = 37767;
    private ImageView image;
    private float currentDegree = 0f;
    private String broadcastAddress = "192.168.1.255";

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            view.setText(String.valueOf(azimuth));

            new Thread(udpServer).start();

            timerHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String broadcastAddressTemp = getBroadcastAddress();
            if (broadcastAddressTemp != null && !broadcastAddressTemp.equals("")){
                broadcastAddress = broadcastAddressTemp;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket = new DatagramSocket(SERVER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        image = (ImageView) findViewById(R.id.imageViewCompass);


        view = (TextView) findViewById(R.id.textView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(mySensorEventListener, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        timerHandler.postDelayed(timerRunnable, 0);

    }

    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // angle between the magnetic north direction
            // 0=North, 90=East, 180=South, 270=West
            azimuth = event.values[0];

            float degree = Math.round(event.values[0]);

            // create a rotation animation (reverse turn degree degrees)
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -degree,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            // how long the animation will take place
            ra.setDuration(210);

            // set the animation after the end of the reservation status
            ra.setFillAfter(true);

            // Start the animation
            image.startAnimation(ra);

            currentDegree = -degree;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public class UDPServer implements Runnable {


        @Override
        public void run() {
            try {
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
                String messageStr = "azimuth=" + String.valueOf(azimuth);

                InetAddress local = InetAddress.getByName(broadcastAddress);
                int msg_length=messageStr.length();
                byte[] message = messageStr.getBytes();
                DatagramPacket p = new DatagramPacket(message, msg_length, local, SERVER_PORT);
                socket.send(p);
                //socket.close();

            } catch (Exception e) {
                Log.e("UDP", "Send error", e);
            }
        }
    }

    String getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        System.out.println("Broadcast Address: " + InetAddress.getByAddress(quads).getHostAddress());
        return InetAddress.getByAddress(quads).getHostAddress();
    }

}
