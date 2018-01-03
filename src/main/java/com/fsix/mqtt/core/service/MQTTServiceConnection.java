package com.fsix.mqtt.core.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.fsix.mqtt.bean.MqttConnBean;
import com.fsix.mqtt.util.MQConst;
import com.fsix.mqtt.util.Logc;

public class MQTTServiceConnection implements ServiceConnection {
    private Messenger client;
    private Messenger server;
    private MqttConnBean mqttConnBean;

    public MQTTServiceConnection(Messenger client, MqttConnBean mqttConnBean) {
        this.client = client;
        this.mqttConnBean = mqttConnBean;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        server = new Messenger(service);
        Logc.i("onServiceConnected.Connected to remote service");
        try {
            Message msg = Message.obtain(null,
                    MQConst.MSG_REGISTER_CLIENT);
            msg.replyTo = client;
            Bundle bundle = new Bundle();
            bundle.putSerializable(MQConst.MSG_DATA, mqttConnBean);
            msg.obj = bundle;
            server.send(msg);
        } catch (RemoteException e) {
            Logc.e("onServiceConnected1.." + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Logc.e("onServiceConnected2.." + e.getMessage());
        }
        Logc.i("MQTTService服务启动成功");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    public Messenger getServer() {
        return server;
    }
}
