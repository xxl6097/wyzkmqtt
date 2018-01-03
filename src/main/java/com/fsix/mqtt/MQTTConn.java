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

public class MQTTConn {
    public static final String TAG = MQTTConn.class.getSimpleName();
    private MqttAndroidClient client = null;
    private MqttConnectOptions conOpt;
    private static MqttConnBean connBean;
    public volatile boolean isConnectFlag = false; //是否连接
    private boolean cancleConnectFlag = false; //断开连接标志
    private Context mContext;

    public void start(Context mContext, MqttConnBean bean) {
        this.connBean = processMqtt(bean);
        this.mContext = mContext;
        init();
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
                doClientConnection();
            }

        }


    }


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
        if (isConnectFlag()) {
            MqttMessage mqttMessage = new MqttMessage();
            try {
                mqttMessage.setPayload(data);
                client.publish(connBean.getTopic(), mqttMessage);
            } catch (MqttException e) {
                e.printStackTrace();
            }
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
     * 订阅主题
     */
    public void subscribe() {
        try {
            // 订阅myTopic话题
            if (connBean != null) {
                String topic = connBean.getTopic();
                Logc.w("NETWORK_TAG:.topic:"+topic);
                int qas = connBean.getQos();
                if (!TextUtils.isEmpty(topic)) {
                    client.subscribe(topic, qas);
                }
            }


        } catch (MqttException e) {
            Logc.e(TAG, e.toString());
        }
    }

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
            return null;
        }

    }
}
