package com.fsix.mqtt;

import android.content.Context;
import android.text.TextUtils;

import com.fsix.mqtt.bean.MqttConnBean;
import com.fsix.mqtt.core.callback.FSixMqttCallback;
import com.fsix.mqtt.util.Logc;
import com.fsix.mqtt.util.NetworkUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQ {
    public static final String TAG = "NETWORK_TAG";//MQ.class.getSimpleName();
    private MqttAndroidClient client = null;
    private MqttConnectOptions conOpt;
    private MqttConnBean connBean;
    public volatile boolean isConnectFlag = false; //是否连接
    private boolean cancleConnectFlag = false; //断开连接标志
    private Context mContext;
    private static MQ instances = null;
    private static final String SERVICE_NAME = "org.eclipse.paho.android.service.MqttService";
    private IMqttActionListener mqttActionListener;

    /**
     * 订阅主题
     */
    public void subscribe() {
//        String topic = connBean.getTopic();
//        Integer qos = connBean.getQos();
//        Boolean retained = connBean.getRetain() == null ? false : true;
//        try {
//            client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
//        } catch (MqttException e) {
//            Logc.e(TAG, e.toString());
//        }
        try {
            // 订阅myTopic话题
            if (connBean != null) {
                String topic = connBean.getTopic();
                Logc.d("========>subscribe " + topic);
                int qas = connBean.getQos();
                if (!TextUtils.isEmpty(topic)) {
                    client.subscribe(topic, qas);
                }
            }


        } catch (MqttException e) {
            Logc.e(TAG, e.toString());
        }
    }

    public void subscribe(String topic, int qos) {
        try {
            // 订阅myTopic话题
            Logc.d("========>subscribe " + topic);
            if (!TextUtils.isEmpty(topic)) {
                client.subscribe(topic, qos);
            }


        } catch (MqttException e) {
            Logc.e(TAG, e.toString());
        }
    }

    /**
     * 是否已连上mqtt服务器
     *
     * @return
     */
    public boolean isConnectFlag() {
        if (client != null && client.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void publish(byte[] data) {
        publish(connBean.getTopic(), data);
    }

    public void publish(String topic, byte[] data) {
        if (isConnectFlag()) {
            MqttMessage mqttMessage = new MqttMessage();
            try {
                mqttMessage.setPayload(data);
                client.publish(topic, mqttMessage);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void publish(byte[] data, IMqttActionListener actionListener) {
        publish(connBean.getTopic(), data, actionListener);
    }

    public void publish(String topic, byte[] data, IMqttActionListener actionListener) {
        if (isConnectFlag()) {
            MqttMessage mqttMessage = new MqttMessage();
            try {
                mqttMessage.setPayload(data);
                client.publish(topic, mqttMessage, null, actionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void start(Context mContext, MqttConnBean bean) {
        this.connBean = processMqtt(bean);
        this.mContext = mContext;
        init();
    }

    public void start(Context mContext, MqttConnBean bean, IMqttActionListener listener) {
        this.connBean = processMqtt(bean);
        this.mContext = mContext;
        this.onMqttConnectListener = listener;
        init();
    }

    public void stop() {
        if (client != null) {
            if (client.isConnected()) {
                //注销mqtt相关资源
                try {
                    client.disconnect();
                    client = null;
                    Logc.d(TAG, "mqtt server close");
                } catch (MqttException e) {
                    Logc.d(TAG, e.toString());
                }

                if (isConnectFlag) {
                    isConnectFlag = false;
                }
            }
        }


    }

    private void init() {
        // 服务器地址（协议+地址+端口号）
        isConnectFlag = false;
        String uri = connBean.getBrokerUrl();
        if (!TextUtils.isEmpty(uri)) {
            if (client == null) {
                client = new MqttAndroidClient(mContext, uri, connBean.getClientId());
                //client.registerResources(mContext);
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
                // last will message

                boolean doConnect = true;
                String message = "{\"terminal_uid\":\"" + connBean.getClientId() + "\"}";
                String topic = connBean.getTopic();
                Integer qos = connBean.getQos();
                Boolean retained = connBean.getRetain() == null ? false : true;
                if ((!message.equals("")) || (!topic.equals(""))) {
                    // 最后的遗嘱
                    try {
                        conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
                    } catch (Exception e) {
                        Logc.e(TAG,e.toString());
                        doConnect = false;
                        iMqttActionListener.onFailure(null, e);
                    }
                }
                if (doConnect) {
                    doClientConnection();
                }
//                doClientConnection();
            }

        }


    }


    /*boolean doConnect = true;
    String message1 = "{\"terminal_uid\":\"" + clientId + "\"}";
    String topic = clientId + "/client";//connOpts.getTopic();
    Integer qos = 0;//connOpts.getQos();
    Boolean retained = true;//connBean.getRetain() == null ? false : true;
            if ((!message1.equals("")) || (!topic.equals(""))) {
        try {
            connOpts.setWill(topic, message1.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (Exception e) {
            doConnect = false;
        }
    }
            if (doConnect) {
        sampleClient.connect(connOpts);
    }*/

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!client.isConnected() && NetworkUtil.isConnected(mContext)) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                Logc.e(TAG, e.toString());
            }
        }

    }

    public void reConnect() {
        doClientConnection();
    }


    private IMqttActionListener onMqttConnectListener;
    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Logc.d("========>mq.onSuccess " + arg0.toString());
            if (client != null && client.isConnected()) {
                if (!isConnectFlag) {
                    isConnectFlag = true;
                }
                //subscribe();
            }
            if (onMqttConnectListener != null) {
                onMqttConnectListener.onSuccess(arg0);
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Logc.e(TAG, "=======>连接失败 " + arg1.getMessage());
            if (isConnectFlag) {
                isConnectFlag = false;
            }

            if (onMqttConnectListener != null) {
                onMqttConnectListener.onFailure(arg0, arg1);
            }
        }
    };

    private MqttConnBean processMqtt(MqttConnBean mqttConnBean) {
        try {
            MqttConnBean mqttConnBeanData = new MqttConnBean();
            String clientId = mqttConnBean.getClientId();
            mqttConnBeanData.setClientId(clientId);
            mqttConnBeanData.setPassword(mqttConnBean.getPassword());
            mqttConnBeanData.setUserName(mqttConnBean.getUserName());
            mqttConnBeanData.setProtocolVersion(4);
            mqttConnBeanData.setKeepAlive((long) 30);
            mqttConnBeanData.setRetain(0);
            mqttConnBeanData.setQos(1);
            mqttConnBeanData.setCleanSession(1);
            //设置主题
            mqttConnBeanData.setTopic(mqttConnBean.getTopic());
            //设置请求地址
            mqttConnBeanData.setBrokerUrl(mqttConnBean.getBrokerUrl());
            return mqttConnBeanData;
        } catch (Exception e) {
            Logc.d(TAG, e.toString());
            return null;
        }

    }
}
