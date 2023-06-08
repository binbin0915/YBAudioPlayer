package com.xwrl.mvvm.demo.service.manager;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.media.session.MediaButtonReceiver;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.view.SongLrcActivity;

import java.lang.ref.SoftReference;

import static androidx.media.app.NotificationCompat.MediaStyle;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/3
 * 作用:
 */
public class MediaNotificationManager {

    private static final String TAG = "MediaNotificationManager";

    private static final String CHANNEL_ID = "com.xwrl.mvvm.demo.channel";

    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mNextAction;
    private final NotificationCompat.Action mPrevAction;
    private final NotificationCompat.Action mOrderAction, mRepeatAction, mRandomAction;
    private final NotificationCompat.Action mLrcAction;
    private final NotificationCompat.Action mLoveAction;
    private final NotificationManager mNotificationManager;
    private Application mApplication;
    private final PendingIntent clickPendingIntent, deletePendingIntent;
    private boolean isCustomNotification;
    private final SoftReference<RemoteViews> mRemoteViews,mRemoteViewsBig;

    public MediaNotificationManager(Application application){

        mApplication = application;

        mRemoteViews = new SoftReference<>(new RemoteViews(
                application.getPackageName(),R.layout.layout_notification_normal));
        mRemoteViewsBig = new SoftReference<>(new RemoteViews(
                application.getPackageName(),R.layout.layout_notification_big));
        initRemote(application);

        mNotificationManager =
                (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);

        clickPendingIntent = PendingIntent.getActivity(application, 0,
                new Intent(application, SongLrcActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        deletePendingIntent = getPendingIntent(application,
                                                BaseMusicService.DYQL_CUSTOM_ACTION_STOP);

        mPlayAction =
                new NotificationCompat.Action(
                        R.drawable.iv_lrc_play,
                        application.getString(R.string.label_notification_play),
                        getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_PLAY));
        mPauseAction =
                new NotificationCompat.Action(
                        R.drawable.iv_lrc_pause,
                        application.getString(R.string.label_notification_pause),
                        getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_PAUSE));
        mNextAction =
                new NotificationCompat.Action(
                        R.drawable.iv_next,
                        application.getString(R.string.label_notification_next),
                        getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_NEXT));

        mPrevAction =
                new NotificationCompat.Action(
                        R.drawable.iv_previous,
                        application.getString(R.string.label_notification_previous),
                        getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_PREVIOUS));

        mLoveAction =
                new NotificationCompat.Action(
                        R.drawable.ic_love,
                        application.getString(R.string.label_notification_love),
                        getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_COLLECT_SONGS));

        mOrderAction =
                new NotificationCompat.Action(
                        R.drawable.iv_playback_mode_order,
                        application.getString(R.string.label_notification_mode),
                        getPendingIntent(application, MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE));
        mRepeatAction =
                new NotificationCompat.Action(
                        R.drawable.iv_playback_mode_repeat,
                        application.getString(R.string.label_notification_mode),
                        getPendingIntent(application, MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE));
        mRandomAction =
                new NotificationCompat.Action(
                        R.drawable.iv_playback_mode_random,
                        application.getString(R.string.label_notification_mode),
                        getPendingIntent(application, MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE));
        mLrcAction =
                new NotificationCompat.Action(
                        R.drawable.ic_lrc,
                        application.getString(R.string.label_notification_lyric),
                        getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_SHOW_LYRICS));

        mNotificationManager.cancelAll();
    }

    public void onDestroy(){
        if (mApplication != null) mApplication = null;
        if (mRemoteViews != null) mRemoteViews.clear();
        if (mRemoteViewsBig != null) mRemoteViewsBig.clear();
    }

    public boolean isCustomNotification() { return isCustomNotification; }
    public void setCustomNotification(boolean customNotification) {
        this.isCustomNotification = customNotification;
    }

    private PendingIntent getPendingIntent(Context context, String action) {
        return PendingIntent.getBroadcast(context.getApplicationContext(),
                0,new Intent(action),PendingIntent.FLAG_IMMUTABLE);
    }
    /** 获得MediaButtonReceiver的PendingIntent
     * 可进入其中查看源码收录了哪些Action，{@link PlaybackStateCompat[toKeyCode方法]}
     * 其action未能满足现阶段音频App的全部要求，故未采用，所以走的接收蓝牙广播的回调通道，
     * 且此方法需在AndroidManifest.xml中声明静态广播接收器才能够有播放控制按键回调*/
    @Deprecated(since = ""+Build.VERSION_CODES.LOLLIPOP)
    private PendingIntent getMediaButtonIntent(Application application,
                                               @PlaybackStateCompat.Actions long state) {

        return MediaButtonReceiver.buildMediaButtonPendingIntent(application, state);
    }

    public NotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    public Notification getNotification(MediaMetadataCompat metadata,
                                        @NonNull PlaybackStateCompat state,
                                        MediaSessionCompat.Token token) {
        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
        MediaDescriptionCompat description = metadata.getDescription();
        Log.w(TAG, "getNotification: "+description+", 播放状态 "+isPlaying);
        Bitmap bitmap = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        bitmap = PictureUtil.getResIdBitmap(bitmap,500,mApplication.getResources(),
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q ? 10 : 100);
        NotificationCompat.Builder builder =
                buildNotification(state, token, isPlaying, metadata, bitmap)
                .setLargeIcon(bitmap);
        Log.e(TAG, "getNotification: "+isCustomNotification());
        return builder.build();
    }

    private NotificationCompat.Builder buildNotification(@NonNull PlaybackStateCompat state,
                                                         MediaSessionCompat.Token token,
                                                         boolean isPlaying,
                                                         MediaMetadataCompat metadata,
                                                         Bitmap bitmap) {
        // Android 8.0+ | Api 26+ 必须为 Notification 创建 通知通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { createChannel(true); }

        NotificationCompat.Action modeAction = null;
        //根据播放模式值 确定使用哪个模式Action
        for (PlaybackStateCompat.CustomAction customAction : state.getCustomActions()){
            String action = customAction.getAction();
            int mode = Integer.parseInt(customAction.getName().toString());
            if (MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE.equals(action)) {
                if (1 == mode){ modeAction = mOrderAction; }
                else if (2 == mode){ modeAction = mRandomAction; }
                else { modeAction = mRepeatAction; }
            }
            Log.d(TAG, "buildNotification: "+customAction.getAction()+" "+customAction.getName());
        }
        MediaDescriptionCompat description = metadata.getDescription();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mApplication, CHANNEL_ID)
                        .setChannelId(CHANNEL_ID)
                        .setContentIntent(clickPendingIntent)
                        //适用于Api21-
                        .setDeleteIntent(deletePendingIntent)
                        .setSmallIcon(R.drawable.ic_test)
                        //设置 Ongoing 为true ,则此通知不可滑动关闭。
                        //设置 Ongoing 为false ,则此通知在音乐暂停或停止时可向右滑动关闭。
                        //个人猜测是通过MediaSession播放状态来判定的，
                        //所以false 值 适合系统样式的音乐播放控制通知，适用于Api 21+
                        //true 值 适合App个性样式的音乐播放控制通知
                        .setOngoing(isCustomNotification())
                        .setAutoCancel(false)
                        //设置为任意页面可见
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        //显示此通知的创建时间
                        .setShowWhen(false)//.setWhen(System.currentTimeMillis())
                        //通知类别：用于播放的媒体传输控制。MediaSession框架
                        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                        //添加应用程序图标并设置其强调色，注意颜色(建议支持夜间模式)
                        //.setColor(ContextCompat.getColor(mApplication, R.color.notification_bg))
                        // Title - 通常指歌曲名。
                        .setContentTitle(description.getTitle())
                        // Subtitle - 本APP指 “歌手 - 歌曲专辑名“ 格式文本。
                        .setContentText(description.getSubtitle() + " - " + description.getDescription());

        MediaStyle style = !isCustomNotification() ? new MediaStyle() :
                new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle();
        builder.setStyle(style.setMediaSession(token)
                //折叠通知时选择显示哪三个Action及其图标，开发人员可自定义，亦可交给用户于设置中选择
                .setShowActionsInCompactView(0, 1, 2)
                //与Android L | Api 21及更早版本向后兼容。
                .setShowCancelButton(true)
                .setCancelButtonIntent(deletePendingIntent));
        //为Notification.MediaStyle 设置按钮Action，从左至右 0，1，2，3，4
        builder.addAction(modeAction == null ? mOrderAction : modeAction)
                .addAction(isPlaying ? mPauseAction : mPlayAction)
                .addAction(mNextAction)
                .addAction(mLoveAction)
                .addAction(mLrcAction);
        if (isCustomNotification()) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) builder.setStyle(null);
            //专辑图片
            mRemoteViews.get().setImageViewBitmap(R.id.notification_iv_album,bitmap);
            mRemoteViewsBig.get().setImageViewBitmap(R.id.notification_iv_album,bitmap);
            updateRemoteView(metadata,isPlaying,mApplication);
            builder.setCustomContentView(mRemoteViews.get());
            builder.setCustomBigContentView(mRemoteViewsBig.get());
        }

        return builder;
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(boolean isPlayControl) {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            int importance = isPlayControl ? NotificationManager.IMPORTANCE_LOW :
                    NotificationManager.IMPORTANCE_DEFAULT;
            // 给用户展示的通知通道名称.
            CharSequence name = "音乐播放";
            // 给用户展示通知通道的描述。
            String description = "Bilibili喜闻人籁 - 音乐播放控制通知";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Configure the notification channel.
            channel.setDescription(description);
            //是否显示在锁屏上
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            //关闭此通知发出时铃声
            channel.setSound(null,null);
            /*如果设备支持此功能，则设置发送到此频道通知的通知灯颜色。*/
            if(importance == NotificationManager.IMPORTANCE_LOW){
                channel.enableVibration(false);//禁止震动
                channel.setVibrationPattern(new long[]{0});
            }else {
                channel.enableLights(true);//显示通知呼吸灯
                channel.setLightColor(Color.GREEN);
                channel.enableVibration(true);
                channel.setVibrationPattern(
                        new long[]{100,200,300,400,500,400,300,200,400});
            }
            /*设置发布到此频道的通知是否可以在启动程序中显示为应用程序图标徽章。*/
            channel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(channel);
            Log.d(TAG, "createChannel: 创建新的通知通道 | 信道");
        } else {
            Log.d(TAG, "createChannel: 复用现有通知通道 | 信道");
        }
    }
    private void updateRemoteView(MediaMetadataCompat metadata,
                                  boolean isPlaying,
                                  Application application){
        RemoteViews view = mRemoteViews.get(), viewBig = mRemoteViewsBig.get();
        String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                album = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);


        //歌名
        view.setTextViewText(R.id.notification_top_song,title);
        viewBig.setTextViewText(R.id.notification_top_song,title);
        //歌手
        view.setTextViewText(R.id.notification_top_singer,artist+" - "+album);
        viewBig.setTextViewText(R.id.notification_top_singer,artist+" - "+album);
        //播放
        PendingIntent playIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_PLAY),
                pauseIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_PAUSE);
        /*
        * ！！！通过BitmapFactory获取图片可能无法获取到Bitmap，请使用ResourcesCompat获取Drawable来转Bitmap
        * */
        int playResId = isPlaying ? R.drawable.iv_lrc_pause : R.drawable.iv_lrc_play;
        Drawable drawable = ResourcesCompat.getDrawable(
                                                application.getResources(), playResId, null);
        if (drawable != null){
            Bitmap playBitmap = PictureUtil.drawableToBitmap(drawable);
            //Log.d(TAG, "updateRemoteView: 播放图片对象是否为空 "+(application.getResources() == null));
            view.setImageViewBitmap(R.id.notification_iv_play,playBitmap);
            viewBig.setImageViewBitmap(R.id.notification_iv_play,playBitmap);
        }
        view.setOnClickPendingIntent(R.id.notification_iv_play, isPlaying ? pauseIntent : playIntent);
        viewBig.setOnClickPendingIntent(R.id.notification_iv_play, isPlaying ? pauseIntent : playIntent);
        //收藏
        //歌词
        //版本适配 按压动画
    }
    private void initRemote(Application application){
        RemoteViews view = mRemoteViews.get(), viewBig = mRemoteViewsBig.get();
        //设置点击发送广播 大视图
        PendingIntent
                nextIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_NEXT),
                previousIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_PREVIOUS),
                loveIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_COLLECT_SONGS),
                lrcIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_SHOW_LYRICS),
                stopIntent = getPendingIntent(application,BaseMusicService.DYQL_CUSTOM_ACTION_STOP);


        viewBig.setOnClickPendingIntent(R.id.notification_iv_right, nextIntent);
        viewBig.setOnClickPendingIntent(R.id.notification_iv_left, previousIntent);
        viewBig.setOnClickPendingIntent(R.id.notification_iv_love,loveIntent);
        viewBig.setOnClickPendingIntent(R.id.notification_iv_close,stopIntent);
        viewBig.setOnClickPendingIntent(R.id.notification_iv_lrc,lrcIntent);
        //小视图
        view.setOnClickPendingIntent(R.id.notification_iv_right,nextIntent);
        view.setOnClickPendingIntent(R.id.notification_iv_love,loveIntent);
        view.setOnClickPendingIntent(R.id.notification_iv_close,stopIntent);
    }
}
