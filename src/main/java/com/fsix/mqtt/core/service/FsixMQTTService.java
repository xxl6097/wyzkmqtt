package com.fsix.mqtt.core.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;

import com.fsix.mqtt.bean.MQBean;
import com.fsix.mqtt.bean.MqttConnBean;
import com.fsix.mqtt.core.callback.FSixMqttCallback;
import com.fsix.mqtt.util.ATil;
import com.fsix.mqtt.util.Logc;
import com.fsix.mqtt.util.MQConst;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * ————————————————————————————————
 * Copyright (C) 2014-2017, by het, Shenzhen, All rights reserved.
 * ————————————————————————————————
 * <p>FsixMQTTService
 * <p>描述：</p>
 * 名称:  <br>
 * 作者: uuxia<br>
 * 版本: 1.0<br>
 * 日期: 2017/5/11 20:09<br>
 **/
public class FsixMQTTService extends Service {

    public static final String TAG = FsixMQTTService.class.getSimpleName();
    private MqttAndroidClient client = null;
    private MqttConnectOptions conOpt;
    private static MqttConnBean connBean;
    private boolean isConnectFlag = false; //是否连接
    private static final String SERVICE_NAME = "org.eclipse.paho.android.service.MqttService";
    private final Messenger server = new Messenger(new IncomingHandler());
    private ArrayList<Messenger> clients = new ArrayList<Messenger>();


    @Override
    public void onCreate() {
        super.onCreate();
        Logc.e("onCreate");
//        String uuid = ATil.getDeviceId(this);
//        MqttConnBean mqttConnBean = new MqttConnBean();
//        mqttConnBean.setBrokerUrl("tcp://120.77.252.48:1883");
//        mqttConnBean.setClientId(uuid);
//        mqttConnBean.setUserName("admin");
//        mqttConnBean.setPassword("public");
//        mqttConnBean.setTopic("airmonitordata");
//        connBean = mqttConnBean;
//        init();
    }

    @Override
    public IBinder onBind(Intent intent) {
        MqttConnBean bean = (MqttConnBean) intent.getSerializableExtra(MQConst.MSG_SERVICE_INIT);
        if (bean != null) {
            Logc.e("onBind " + bean.toString());
            connBean = bean;
            init();
        }
        return server.getBinder();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Logc.e("onStartCommand");
        return START_NOT_STICKY;
    }

    public void publish(String msg) {
        String topic = connBean.getTopic();
        Integer qos = connBean.getQos();
        Boolean retained = connBean.getRetain() == null ? false : true;
        try {
            client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            Logc.e(TAG, e.toString());
        }
    }

    private void init() {
        Logc.e("init ");
        // 服务器地址（协议+地址+端口号）
        isConnectFlag = false;
        String uri = connBean.getBrokerUrl();
        if (!TextUtils.isEmpty(uri)) {
            if (client == null) {
                client = new MqttAndroidClient(getBaseContext(), uri, connBean.getClientId());
                // 设置MQTT监听并且接受消息
                client.setCallback(new FSixMqttCallback(connBean.getTopic()));
                conOpt = new MqttConnectOptions();
                // 清除缓存
                conOpt.setCleanSession(true);
                // 设置超时时间，单位：秒
                conOpt.setConnectionTimeout(10);
                // 心跳包发送间隔，单位：秒
                conOpt.setKeepAliveInterval(20);
                // 用户名
                conOpt.setUserName(connBean.getUserName());
                // 密码
                conOpt.setPassword(connBean.getPassword().toCharArray());
                Logc.e("init1 ");
                // last will message
//                boolean doConnect = true;
//                String message = "{\"terminal_uid\":\"" + connBean.getClientId() + "\"}";
//                String topic = connBean.getTopic();
//                Integer qos = connBean.getQos();
//                Boolean retained = connBean.getRetain() == null ? false : true;
//                if ((!message.equals("")) || (!topic.equals(""))) {
//                    // 最后的遗嘱
//                    try {
//                        conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
//                    } catch (Exception e) {
//                        Logc.e(TAG,e.toString());
//                        doConnect = false;
//                        iMqttActionListener.onFailure(null, e);
//                    }
//                }
//                if (doConnect) {
//                    doClientConnection();
//                }
                doClientConnection();
            }

        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (client != null) {
            if (client.isConnected()) {
                //注销mqtt相关资源
                try {
                    client.unsubscribe(connBean.getTopic());
                    client.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
//                client.close();
//                try {
//                    client.disconnect(getApplicationContext(), new IMqttActionListener() {
//                        @Override
//                        public void onSuccess(IMqttToken asyncActionToken) {
//
//                        }
//
//                        @Override
//                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//
//                        }
//                    });
//                    client=null;
//                } catch (MqttException e) {
//                    e.printStackTrace();
//                }
            }
            if (isConnectFlag) {
                isConnectFlag = false;
            }
        }

    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNomarl()) {
            try {
                Logc.e("doClientConnection ");
                client.connect(conOpt, null, iMqttActionListener);
                Logc.e("doClientConnection1 ");
            } catch (MqttException e) {
                Logc.e(TAG, e.toString());
                Logc.e("doClientConnection2 " + e.getMessage());
            }
        }

    }

    /**
     * 订阅主题
     */
    public void subscribe() {
        try {
            // 订阅myTopic话题
            if (connBean != null) {
                String topic = connBean.getTopic();
                int qas = connBean.getQos();
                if (!TextUtils.isEmpty(topic)) {
                    client.subscribe(topic, qas);
                }
            }
        } catch (MqttException e) {
            Logc.e(TAG, e.toString());
        }
    }

    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            if (client != null && client.isConnected()) {
                if (!isConnectFlag) {
                    isConnectFlag = true;
                }
                subscribe();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Logc.e(TAG, "连接失败 " + arg1.getMessage());
            if (isConnectFlag) {
                isConnectFlag = false;
            }
        }
    };

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Logc.d(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Logc.d(TAG, "MQTT 没有可用网络");
            return false;
        }
    }

    public void receive(MQBean mqBean) {
        if (mqBean != null && server != null) {
            Message msg = Message.obtain();
            msg.what = MQConst.MSG_SET_VALUE;
            Bundle bundle = new Bundle();
            bundle.putSerializable(MQConst.MSG_DATA, mqBean);
            msg.obj = bundle;
            try {
                server.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                Logc.e("receive data failed:receive=" + e.getMessage());
            }
        }
    }

    private void fromClient(MQBean mqBean) {
        Logc.e("IncomingHandler.." + mqBean.toString());
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MQConst.MSG_REGISTER_CLIENT:
                    Bundle obj = (Bundle) msg.obj;
                    if (obj != null) {
                        Serializable data = obj.getSerializable(MQConst.MSG_DATA);
                        if (data != null && data instanceof MqttConnBean) {
                            try {
                                MqttConnBean ss = (MqttConnBean) data;
//                                connBean = ss;
//                                init();
                                Logc.e("IncomingHandler.." + ss.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    clients.add(msg.replyTo);
                    break;
                case MQConst.MSG_UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    break;
                case MQConst.MSG_SEND_DATA:
                    Bundle bb = (Bundle) msg.obj;
                    if (bb != null) {
                        Serializable data = bb.getSerializable(MQConst.MSG_DATA);
                        if (data != null && data instanceof MQBean) {
                            try {
                                MQBean mqBean = (MQBean) data;
                                fromClient(mqBean);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case MQConst.MSG_SET_VALUE:
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        try {
                            clients.get(i).send(Message.obtain(null, MQConst.MSG_SET_VALUE, msg.obj));
                        } catch (RemoteException e) {
                            clients.remove(i);
                        }
                    }
                    if (clients.size() == 0) {

                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
