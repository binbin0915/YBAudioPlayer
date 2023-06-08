package com.xwrl.mvvm.demo.service;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.TransportControls;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import com.xwrl.mvvm.demo.service.manager.MediaNotificationManager;
import com.xwrl.mvvm.demo.service.manager.MediaPlayerManager;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/20
 * 作用:
 */
public abstract class BaseMusicService extends MediaBrowserServiceCompat {

    private static final String TAG = "BaseMusicService";

    private MediaControllerCompat mMediaController;
    private MediaNotificationManager mMediaNotificationManager;
    private MyBlueToothBroadcastReceiver mBlueToothReceiver;
    private boolean isStartForeground;
    private static final int MEDIA_CHANNEL_ID = 130;
    //通知Action，componentName
    public static final String DYQL_CUSTOM_ACTION_COLLECT_SONGS = "collect_songs_dyql";
    public static final String DYQL_CUSTOM_ACTION_SHOW_LYRICS = "show_lyrics_dyql";
    public static final String DYQL_CUSTOM_ACTION_PLAY = "play_dyql";
    public static final String DYQL_CUSTOM_ACTION_PAUSE = "pause_dyql";
    public static final String DYQL_CUSTOM_ACTION_PREVIOUS = "previous_dyql";
    public static final String DYQL_CUSTOM_ACTION_NEXT = "next_dyql";
    public static final String DYQL_CUSTOM_ACTION_STOP = "stop_dyql";
    public static final String DYQL_NOTIFICATION_STYLE = "notification_style_dyql";

    @Override
    public void onCreate() {
        super.onCreate();
        initManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaController != null) { mMediaController = null; }
        if (mBlueToothReceiver != null) {
            unregisterReceiver(mBlueToothReceiver);
            mBlueToothReceiver = null;
        }
        if (mMediaNotificationManager != null) {
            mMediaNotificationManager.onDestroy();
            mMediaNotificationManager = null;
        }
    }

    protected void setMediaController(MediaControllerCompat mediaController) {
        this.mMediaController = mediaController;
    }

    protected boolean isStartForeground() { return isStartForeground; }

    private void initManager(){
        //初始化通知管理者和媒体按钮接收器
        mMediaNotificationManager = new MediaNotificationManager(getApplication());
        SharedPreferences settings = getSharedPreferences("UserLastMusicPlay", 0);
        boolean notificationStyle = settings.getBoolean("NotificationStyle", false);
        mMediaNotificationManager.setCustomNotification(notificationStyle);
        //Log.e(TAG, "initManager: "+notificationStyle);
        initReceiver();
    }

    private void initReceiver() {
        mBlueToothReceiver = new MyBlueToothBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(DYQL_CUSTOM_ACTION_SHOW_LYRICS);
        filter.addAction(DYQL_CUSTOM_ACTION_COLLECT_SONGS);
        filter.addAction(MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE);
        filter.addAction(DYQL_CUSTOM_ACTION_PLAY);
        filter.addAction(DYQL_CUSTOM_ACTION_PAUSE);
        filter.addAction(DYQL_CUSTOM_ACTION_PREVIOUS);
        filter.addAction(DYQL_CUSTOM_ACTION_NEXT);
        filter.addAction(DYQL_CUSTOM_ACTION_STOP);
        registerReceiver(mBlueToothReceiver, filter);
    }

    protected void StartForeground(MediaSessionCompat.Token token,
                                   @NonNull PlaybackStateCompat state){

        if (mMediaController == null) {
            Log.e(TAG, "onReceive: mMediaController == null");return;}

        MediaMetadataCompat metadata = mMediaController.getMetadata();

        Notification notification =
                mMediaNotificationManager.getNotification(metadata, state, token);

        if (isStartForeground) {
            Log.e(TAG, "startForeground: 已创建通知！更新通知");
            mMediaNotificationManager.getNotificationManager()
                    .notify(MEDIA_CHANNEL_ID, notification);
        }else {
            notification.flags = Notification.FLAG_ONGOING_EVENT;//设置常驻通知
            this.startForeground(MEDIA_CHANNEL_ID,notification);
            isStartForeground = true;
        }
    }
    protected void StopForeground(){
        boolean isStopped =
                mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED;
        this.stopForeground(mMediaNotificationManager.isCustomNotification() && isStopped);
        isStartForeground = false;
    }
    protected void setNotificationStyle(MediaSessionCompat.Token token,
                                        @NonNull PlaybackStateCompat playbackState,
                                           boolean nextNotificationStyle){
        if (mMediaNotificationManager == null || mMediaController == null) { return ; }
        mMediaNotificationManager.setCustomNotification(nextNotificationStyle);

        //Log.d(TAG, "setNotificationStyle: "+nextNotificationStyle);
        int state = playbackState.getState();
        boolean isUpdate = state != PlaybackStateCompat.STATE_STOPPED &&
                state != PlaybackStateCompat.STATE_NONE;
        if (isUpdate) { StartForeground(token,playbackState); }
    }

    protected class MyBlueToothBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMediaController == null) {
                Log.e(TAG, "onReceive: mMediaController == null");return;}
            TransportControls transportControls = mMediaController.getTransportControls();
            String action = intent.getAction();
            Log.d(TAG, "onReceive: "+action);
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
            Log.d(TAG, "onReceive: "+bluetoothState);
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_DISCONNECTED:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    //Log.e(TAG, "onReceive: 蓝牙已打开");
                    break;
                case BluetoothAdapter.STATE_CONNECTED:
                    //Log.e(TAG, "onReceive: 蓝牙已连接");
                    break;
            }
            if (MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE.equals(action)) {
                transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            }else if (DYQL_CUSTOM_ACTION_PLAY.equals(action)){
                transportControls.play();
            }else if (DYQL_CUSTOM_ACTION_PAUSE.equals(action)){
                transportControls.pause();
            }else if (DYQL_CUSTOM_ACTION_PREVIOUS.equals(action)){
                transportControls.skipToPrevious();
            }else if (DYQL_CUSTOM_ACTION_NEXT.equals(action)){
                transportControls.skipToNext();
            }else if (DYQL_CUSTOM_ACTION_STOP.equals(action)){
                transportControls.stop();
            }
        }
    }
}
