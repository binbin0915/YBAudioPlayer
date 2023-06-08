package com.xwrl.mvvm.demo;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.xwrl.mvvm.demo.databinding.ActivityMainBinding;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.service.MusicService;
import com.xwrl.mvvm.demo.util.PermissionUtil;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.view.MusicActivity;
import com.xwrl.mvvm.demo.view.MusicDirActivity;
import com.xwrl.mvvm.demo.view.SongLrcActivity;
import com.xwrl.mvvm.demo.viewmodel.MusicViewModel;

import java.util.List;
import java.util.Timer;

public class MainActivity extends BaseActivity<MusicViewModel> {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mMainBinding;
    private MusicViewModel mMusicViewModel;
    private Timer mTimer;
    private Intent mIntentMusic;

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() { return new MyMediaControllerCallback(); }
    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() { return new MyMediaBrowserSubscriptionCallback(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PermissionUtil.IsPermissionNotObtained(this)) { PermissionUtil.getStorage(this);}
        super.onCreate(savedInstanceState);
        mMainBinding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        mMusicViewModel = new MusicViewModel(getApplication());
        mMainBinding.setUserInfo(mMusicViewModel);
        super.setBackToDesktop();

        initView();
        mIntentMusic = new Intent(this, MusicService.class);
        this.startService(mIntentMusic);
    }

    @Override
    protected void onStart() {
        super.onStart();
        UpdateProgressBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StopProgressBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIntentMusic != null) { mIntentMusic = null; }
        if (mMusicViewModel != null) { mMusicViewModel = null; }
        if (mMainBinding != null) {
            mMainBinding.unbind();
            mMainBinding = null;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PermissionUtil.REQUEST_PERMISSION_CODE) {
            if (PermissionUtil.IsPermissionNotObtained(this)) { PermissionUtil.getStorage(this);}
            else {
                Log.w(TAG, "onRequestPermissionsResult: 已获取读写权限");
                //添加列表
                super.subscribe();
            }
        }
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            activityOnChildrenLoad(mMusicViewModel,
                                    mMainBinding.mainActivityIvPlayLoading,
                                    children);
            mMusicViewModel.setPhoneRefresh(mRefreshRateMax);
            //！！！少更新样式状态
            mMusicViewModel.setCustomStyle(MediaControllerCompat.getMediaController(MainActivity.this)
                    .getMetadata().getLong(BaseMusicService.DYQL_NOTIFICATION_STYLE) == 0
            );
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            mMusicViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
            mMusicViewModel.setPlaybackState(playbackState.getState());
            playbackStateChanged(playbackState,
                    mMainBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }
    }

    private void initView(){
        mMainBinding.activityMainUiRoot.setOnApplyWindowInsetsListener(this);
        //等信息更新后再设置回调
        mMainBinding.activityMainNotificationStyleSwitch.setOnCheckedChangeListener(
                mMusicViewModel.getCheckedListener()
        );


        mMainBinding.activityMainGridLayout.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MusicDirActivity.class))
        );
        mMainBinding.mainActivityBottomLayout.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, SongLrcActivity.class));
                overridePendingTransition(R.anim.push_in,0);
            }
        );

        mMainBinding.mainActivityBottomProgressBar.setOnClickListener(v -> mMusicViewModel.playbackButton());

        super.initAnimation(mMainBinding.mainActivityBottomIvAlbum);
        mMainBinding.activityMainIvUser.setImageDrawable(
                PictureUtil.createUserIconDrawable(getApplication(),
                        BitmapFactory.decodeResource(getResources(),R.drawable.ic_test2),
                        120,dpToPx(64)));
        mMainBinding.activityMainTopLayout.setOnClickListener(v ->
                Toast.makeText(this,"打开APP菜单设置",Toast.LENGTH_SHORT).show());

        mMainBinding.mainActivityTvAuthorExit.setOnLongClickListener(v -> {
                StopProgressBar();
                this.stopService(mIntentMusic);
                finish();
                return true;
            }
        );
        //设置深色模式适配的颜色
        int color = super.getViewColor();;
        mMainBinding.mainActivityBottomIvList.getDrawable().setTint(color);
        mMainBinding.mainActivityBottomProgressBar.setProgressColor(color);
    }

    private void UpdateProgressBar() {
        if (mTimer != null) { return; }

        mTimer = new Timer();
        mTimer.schedule(mMusicViewModel.getCircleBarTask(),300,300);
    }

    private void StopProgressBar(){
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }
}