package com.work.light;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.work.light.alarm.service.AlarmService;
import com.work.light.alarm.service.JobSchedulerService;
import com.work.light.alarm.service.RemoteService;
import com.work.light.alarm.sharedpreferences.SharedpreferencesUtil;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendAlarmEveryday1();
    }



    public void sendAlarmEveryday1() {
        Map<String, Object> map = new HashMap<>();
        map.put("isOpenAlarmRemind", true);
        SharedpreferencesUtil.getIntance().saveSharedPreferences(MainActivity.this, "StudyRemindSetting", MODE_APPEND, map);
// 处理Android 6.0 Doze模式
        isIgnoreBatteryOption(MainActivity.this);
        startJobSchedulerService();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("hour", 19);
        SharedpreferencesUtil.getIntance().saveSharedPreferences(MainActivity.this, "StudyRemindSetting", MODE_APPEND, map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("minute", 54);
        SharedpreferencesUtil.getIntance().saveSharedPreferences(MainActivity.this, "StudyRemindSetting", MODE_APPEND, map2);

        // 开启闹钟服务
        startAlarmService();
        // 开启远程服务
        startRemoteService();
    }


    /**
     * 开启闹钟服务
     */
    private void startAlarmService() {
        Intent intentAlarm = new Intent(this, AlarmService.class);
        intentAlarm.setAction("com.work.light.alarm_service");
        intentAlarm.putExtra("isOpenStartForeground", true);
        intentAlarm.putExtra("isUpdateAlarmCalendar", true);
        intentAlarm.putExtra("alarmType", AlarmService.TYPE_ONE_DAY);
        startService(intentAlarm);
    }

    /**
     * 开启远程服务
     */
    private void startRemoteService() {
        Intent intentRemote = new Intent(this, RemoteService.class);
        intentRemote.setAction("com.work.light.remote_service");
        startService(intentRemote);
    }

    /**
     * 开启JobSchedulerService，监听关机重启
     */
    private void startJobSchedulerService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, new ComponentName(getPackageName(), JobSchedulerService.class.getName()));

            builder.setPeriodic(60 * 1000); // 每隔60秒运行一次
            builder.setRequiresCharging(true);

            builder.setPersisted(true);  // 设置设备重启后，是否重新执行任务
            builder.setRequiresDeviceIdle(true);

//            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);

            if (jobScheduler != null) {
                int result = jobScheduler.schedule(builder.build());
                if (result <= 0) {
                    // If something goes wrong
                    jobScheduler.cancel(JOB_ID);
                }
            }
        }
    }

    /**
     * 取消JobScheduler
     */
    private void cancelJobScheduler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(JOB_ID);
            }
        }
    }

    /**
     * 针对N以上的Doze模式
     *
     * @param activity 当前Activity
     */
    @SuppressLint("BatteryLife")
    public void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!(pm != null && pm.isIgnoringBatteryOptimizations(packageName))) {
//                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final int JOB_ID = 122;
    private final int REQUEST_IGNORE_BATTERY_CODE = 123;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
                // 开启JobSchedulerService
                startJobSchedulerService();
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
                Toast.makeText(MainActivity.this, "请开启忽略电池优化~", Toast.LENGTH_LONG).show();
            }
        }
    }
}
