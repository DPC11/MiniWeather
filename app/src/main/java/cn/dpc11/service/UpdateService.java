package cn.dpc11.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by DPC on 2016/11/15.
 */

public class UpdateService extends IntentService {
    static final int UPDATE_INTERVAL = 600 * 1000;
    private Timer timer = new Timer();

    public UpdateService() {
        super("UpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // 后台 Service 自动刷新天气数据
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Log.d("UpdateService", "自动刷新天气数据");

                // 广播通知机制提醒 Activity 刷新天气数据
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("Update");
                getBaseContext().sendBroadcast(broadcastIntent);
            }
        }, 0, UPDATE_INTERVAL);
    }
}
