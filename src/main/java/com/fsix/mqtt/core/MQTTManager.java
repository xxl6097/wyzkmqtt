package com.fsix.mqtt.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.fsix.mqtt.bean.MQBean;
import com.fsix.mqtt.bean.MqttConnBean;
import com.fsix.mqtt.util.MQConst;
import com.fsix.mqtt.core.callback.INotify;
import com.fsix.mqtt.core.service.MQTTServiceConnection;
import com.fsix.mqtt.util.ATil;
import com.fsix.mqtt.util.Logc;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MQTTManager extends Handler {
    private static MQTTManager instance = null;
    private Messenger client;
    private Context context;
    private boolean isBound;
    private MQTTServiceConnection connection;
    private static Set<INotify> obs = new HashSet<INotify>();

    public static MQTTManager getInstance() {
        if (instance == null) {
            synchronized (MQTTManager.class) {
                if (null == instance) {
                    instance = new MQTTManager();
                }
            }
        }
        return instance;
    }

    public void onCreate(Context c, MqttConnBean mqttConnBean) throws Exception {
        context = c.getApplicationContext();
        Intent intent = new Intent();
        int androidVersion = ATil.getSDKVersionNumber();
        Logc.i("cucent android os version:" + androidVersion + " model=" + android.os.Build.MODEL + " thread:" + Thread.currentThread().getName());
        //MI PAD
        if (androidVersion >= 21 || android.os.Build.MODEL.equalsIgnoreCase("MI PAD")
                || android.os.Build.MODEL.equalsIgnoreCase("LG-D728")) {
            //这一句至关重要，对于android5.0以上，所以minSdkVersion最好小于21；
            intent.setPackage(context.getPackageName());
        }
        intent.setAction(MQConst.MQTTSERVICENAME);
        intent.putExtra(MQConst.MSG_SERVICE_INIT, mqttConnBean);
        client = new Messenger(this);
        connection = new MQTTServiceConnection(client, mqttConnBean);
        isBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!isBound) {
            Logc.e("初始化service失败..mService=" + connection.getServer() + " mConnection=" + connection);
            throw new Exception("create service error!..mService=" + connection.getServer() + " mConnection=" + connection);
        } else {
            Logc.i("成功初始化ServiceManager App.packageName=" + context.getPackageName() + " mService=" + connection.getServer() + " mConnection=" + connection);
        }
    }

    public void send(MQBean mqBean) throws RemoteException {
        if (mqBean == null)
            return;
        if (mqBean != null && connection != null) {
            Message msg = Message.obtain();
            msg.what = MQConst.MSG_SEND_DATA;
            Bundle bundle = new Bundle();
            bundle.putSerializable(MQConst.MSG_DATA, mqBean);
            msg.obj = bundle;
            connection.getServer().send(msg);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MQConst.MSG_SET_VALUE:
                dispathPackets(msg);
                break;
            default:
                super.handleMessage(msg);
        }
    }

    private void dispathPackets(Message msg) {
        if (msg == null || msg.obj == null)
            return;
        Bundle bundle = (Bundle) msg.obj;
        if (bundle == null)
            return;
        Serializable data = bundle.getSerializable(MQConst.MSG_DATA);
        if (data == null)
            return;
        if (data instanceof MQBean) {
            post((MQBean) data);
        }
    }

    public synchronized void registerObserver(INotify o) {
        if (o != null) {
            if (!obs.contains(o)) {
                obs.add(o);
            }
        }
    }

    public synchronized void unregisterObserver(INotify o) {
        if (obs.contains(o)) {
            obs.remove(o);
        }
    }

    private synchronized void post(MQBean obj) {
        if (obj == null)
            return;
        Iterator<INotify> it = obs.iterator();
        while (it.hasNext()) {
            INotify mgr = it.next();
            mgr.onNotify(obj);
        }
    }

    public void onTerminate(Context context) throws RemoteException {
        obs.clear();
        if (isBound) {
            if (connection != null && connection.getServer() != null) {
                Message msg = Message.obtain(null,
                        MQConst.MSG_UNREGISTER_CLIENT);
                msg.replyTo = client;
                connection.getServer().send(msg);
            }
            // Detach our existing connection.
            context.unbindService(connection);
            isBound = false;
            Logc.i("接触Service绑定");
        }
    }
}
