package com.espressif.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.ScheduledExecutorService;


public class MainActivity extends AppCompatActivity {
    public String TAG="MyTAG";
    //private String host = "tcp://内网ip:服务器端口号";
    private String host = "tcp://www.dongvdong.top:1883";    // ip:端口号www.dongvdong.top:1883http://www.dongvdong.top:1883/
    private String userName = "dongdong";
    private String passWord = "dongdong";
    private String clientId="AndroidClient1";
    private int i = 1;
    private MqttClient client;
    private String myTopic = "test/key1";
    private MqttConnectOptions options;
    private ScheduledExecutorService scheduler;
    public Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    Toast.makeText(MainActivity.this,"Success",Toast.LENGTH_SHORT).show();
                    try {
                        client.subscribe(myTopic, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else if(msg.what==2){
                    Toast.makeText(MainActivity.this,"fail",Toast.LENGTH_SHORT).show();
                }else if(msg.what==3){
                    Toast.makeText(MainActivity.this,(String)msg.obj,Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "handleMessage");
                }
            }
        };
        Button button1=(Button)findViewById(R.id.button1);
        Button button2=(Button)findViewById(R.id.button2);
        button1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                class MyThread extends Thread{
                    @Override
                    public void run(){
                        init();
                        try {
                            client.connect(options);
                            Message msg = new Message();
                            msg.what = 1;
                            handler.sendMessage(msg);//连接成功

                        } catch (Exception e) {
                            e.printStackTrace();
                            Message msg = new Message();
                            msg.what = 2;
                            handler.sendMessage(msg);
                            //连接失败
                        }
                    }
                }
                new MyThread().start();
            }
        });
        button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){


                publish("on");
            }
        });
    }


    private void init() {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, clientId,
                    new MemoryPersistence());
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调
            client.setCallback(mqttCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {//This method is called when a message arrives from the server

            String str1 = new String(message.getPayload());
            Log.d(TAG, "messageArrived: "+str1);
            Message msg=new Message();
            msg.what=3;
            msg.obj=str1;
            handler.sendMessage(msg);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            //Called when delivery for a message has been completed
        }

        @Override
        public void connectionLost(Throwable arg0) {
            // This method is called when the connection to the server is lost.
            Log.d(TAG, "connectionLost: ");
        }
    };



    public  void publish(String msg){
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        try {
            client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            scheduler.shutdown();
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}