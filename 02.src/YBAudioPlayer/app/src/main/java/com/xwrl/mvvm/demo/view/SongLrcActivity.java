package com.xwrl.mvvm.demo.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.custom.SlideView;
import com.xwrl.mvvm.demo.custom.lyrics.LrcUtil;
import com.xwrl.mvvm.demo.databinding.ActivitySongLrcBinding;
import com.xwrl.mvvm.demo.service.manager.MediaPlayerManager;
import com.xwrl.mvvm.demo.service.manager.MyAudioManager;
import com.xwrl.mvvm.demo.viewmodel.SongLrcViewModel;

import java.util.List;
import java.util.Timer;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/17
 * 作用:
 */
public class SongLrcActivity extends BaseActivity<SongLrcViewModel> {

    private static final String TAG = "SongLrcActivity";

    private ActivitySongLrcBinding mSongLrcBinding;
    private SongLrcViewModel mSongLrcViewModel;
    private Timer mTimer;
    private SlideView mSlideView;
    private ObjectAnimator mNeedleAnimator, mRecordAnimator;
    private final static int pauseRotation = -32, playRotation = -2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSongLrcBinding = DataBindingUtil.setContentView(this, R.layout.activity_song_lrc);
        mSongLrcViewModel = new SongLrcViewModel(getApplication());
        mSongLrcBinding.setSongLrcInfo(mSongLrcViewModel);

        initView();
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
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        release();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void initView(){
        mSlideView = new SlideView(this,SlideView.SLIDE_DIRECTION_DOWN);
        mSongLrcBinding.songLrcRootLayout.setOnApplyWindowInsetsListener(this);
        mRecordAnimator = super.initAnimation(mSongLrcBinding.songLrcCslCenterIvAlbum);

        mSongLrcBinding.songLrcTopReturn.setOnClickListener(v -> onFinish());

        mSongLrcBinding.songLrcBar.setOnSeekBarChangeListener(mSongLrcViewModel.getSeekBarListener());
        mSongLrcBinding.songLrcTopBarVolume
                .setOnSeekBarChangeListener(mSongLrcViewModel.getVolumeListener());

        mSongLrcBinding.songLrcCenterIvNeedle.post(() -> {
            //布局调整
            sureRecordSize();
            needleViewAdaption();
        });

        mSongLrcBinding.songLrcCenterLrc.setOnClickListener(v -> mSongLrcViewModel.setShowLyric(false));

        initAnimator();
        //！！！让vector资源替换颜色，适配深色模式
        int color = Color.parseColor("#EEEEEE");
        mSongLrcBinding.songLrcBottomLeft.getDrawable().setTint(color);
        mSongLrcBinding.songLrcBottomRight.getDrawable().setTint(color);
        mSongLrcBinding.songLrcCenterLove.getDrawable().setTint(color);
        mSongLrcBinding.songLrcBottomList.getDrawable().setTint(color);
    }

    private void initAnimator(){
        mNeedleAnimator = ObjectAnimator.ofFloat(
                mSongLrcBinding.songLrcCenterIvNeedle, "rotation",
                pauseRotation, playRotation);
        mSongLrcBinding.songLrcCenterIvNeedle.setPivotX(0);
        mSongLrcBinding.songLrcCenterIvNeedle.setPivotY(0);
        mNeedleAnimator.setDuration(200);
        mNeedleAnimator.setInterpolator(new LinearInterpolator());
        mNeedleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                updateRecordState(
                        MediaControllerCompat.getMediaController(SongLrcActivity.this).getPlaybackState());
            }
        });
    }

    private void release(){
        if (mNeedleAnimator != null) {
            mNeedleAnimator.removeAllListeners();
            mNeedleAnimator.end();
            mNeedleAnimator.cancel();
            mNeedleAnimator = null;
        }
        if (mSlideView != null) { mSlideView.onDestroy(); mSlideView = null; }
        if (mSongLrcViewModel != null) { mSongLrcViewModel = null; }
        if (mSongLrcBinding != null) {
            mSongLrcBinding.unbind();
            mSongLrcBinding = null;
        }
    }

    private void UpdateProgressBar() {
        if (mTimer != null) { return; }

        mTimer = new Timer();
        mTimer.schedule(mSongLrcViewModel.getTimerTask(),300,300);
    }

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() { return new MyMediaControllerCallback(); }
    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() { return new MyMediaBrowserSubscriptionCallback(); }


    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            activityOnChildrenLoad(mSongLrcViewModel,
                                    mSongLrcBinding.songLrcIvLoading,
                                    children);
            MediaControllerCompat mediaController =
                    MediaControllerCompat.getMediaController(SongLrcActivity.this);
            //同步歌词
            mSongLrcBinding.songLrcCenterLrc.setMediaController(mediaController);
            mSongLrcBinding.songLrcCenterLrc.setLrc(LrcUtil.getLocalLrc(
                    mSongLrcViewModel.getMusicName()+".lrc"));
            //同步播放动画
            if (mediaController.getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING) {
                mSongLrcBinding.songLrcCenterIvNeedle.setRotation(pauseRotation);
            }

        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            mSongLrcViewModel.SyncMusicInformation();
            mSongLrcBinding.songLrcCenterLrc.setLrc(LrcUtil.getLocalLrc(
                    mSongLrcViewModel.getMusicName()+".lrc"));
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            mSongLrcViewModel.setPlaybackState(playbackState.getState());
            updatePlaybackStateAnimator(playbackState);

        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            if (MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME.equals(event)){
                int currentVolume = extras.getInt(event);
                Log.d(TAG, "onSessionEvent: 更新音量 "+currentVolume);
                mSongLrcViewModel.setProgressV(currentVolume);
            }else if (MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE.equals(event)){
                mSongLrcViewModel.setPlaybackModeRes(
                        extras.getInt(MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE));
            }
        }
    }

    private void updatePlaybackStateAnimator(PlaybackStateCompat playbackState) {
        int state = playbackState.getState();
        //加载 与 唱片转动、机械臂 动画
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mSongLrcBinding.songLrcIvLoading.clearAnimation();
            mSongLrcBinding.songLrcIvLoading.setVisibility(View.GONE);
            mNeedleAnimator.start();

        }else if (state == PlaybackStateCompat.STATE_BUFFERING){
            mSongLrcBinding.songLrcIvLoading.setVisibility(View.VISIBLE);
            mSongLrcBinding.songLrcIvLoading.startAnimation(getLoadingAnimation());
        }else if (state == PlaybackStateCompat.STATE_PAUSED ||
                state == PlaybackStateCompat.STATE_STOPPED){
            mNeedleAnimator.reverse();
        }
    }

    private void updateRecordState(PlaybackStateCompat playbackState){
        if (mRecordAnimator == null) return;
        int state = playbackState.getState();
        if (state == PlaybackStateCompat.STATE_PLAYING) {

            if (playbackState.getExtras() == null) mRecordAnimator.start();//动画开始
            else if (playbackState.getExtras().getBoolean("Continue_Playing_Tips")) {
                Log.d(TAG, "onChildrenLoaded: "+mRecordAnimator.isStarted());
                if (mRecordAnimator.isStarted()) {
                    mRecordAnimator.resume();
                }else mRecordAnimator.start();
            }

        }else if (state == PlaybackStateCompat.STATE_PAUSED ||
                state == PlaybackStateCompat.STATE_STOPPED){
            mRecordAnimator.pause();//动画暂停
        }
    }

    private void sureRecordSize(){
        ViewGroup.LayoutParams params1 =
                mSongLrcBinding.songLrcCslCenterIvAlbumBottom.getLayoutParams();
        int width = Math.min(mPhoneWidth,mPhoneHeight);
        width = Math.abs(mPhoneWidth - mPhoneHeight) < 200 ? width - 150 : width;
        int size = (int) (width * 0.76);
        params1.width = size;
        params1.height = size;

        params1 = mSongLrcBinding.songLrcCslCenterIvAlbum.getLayoutParams();
        size -= dpToPx(2);
        params1.width = size;
        params1.height = size;
        mSongLrcViewModel.setAlbumViewSize(size);

    }

    private void needleViewAdaption(){
        //更改布局之前，相关变量值一定要提前获取，低配机型在更改布局后可能不能及时获取到某些控件的宽、高度
        //首先获取到布局绘制完成后唱片的高height
        int top = mSongLrcBinding.songLrcCslCenterIvAlbumBottom.getTop(),
                height = mSongLrcBinding.songLrcCenterIvNeedle.getHeight(),
                bottom = mSongLrcBinding.songLrcTopSinger.getBottom();
        //Log.d(TAG, "needleLayoutAdaption: "+top+", bottom= "+bottom+", height= "+height);
        ConstraintLayout.LayoutParams layoutParams =
                (ConstraintLayout.LayoutParams) mSongLrcBinding.songLrcCenterIvNeedle.getLayoutParams();
        if(top - bottom - 20 <= (height >> 1)){
            //歌手控件与专辑图片高度 大于 机械臂高度，缩小机械臂图标
            //Log.d(TAG, "needleLayoutAdaption: "+(top - bottom - (height >> 1)));
            int def = top - bottom - (height >> 1) <= -30 ? 120 : 50;
            def = isInMultiWindowMode() ? 100 : def;
            if (isPad) def *= 1.5;
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) mSongLrcBinding.songLrcCslCenterIvAlbumBottom.getLayoutParams();
            params.setMargins(0,(top - bottom - (height >> 1) + def),0 ,0);
            int marginLeft = isPad ? (mSongLrcBinding.songLrcCslCenterIvAlbumBottom.getWidth() >> 1) - 18 : (mPhoneWidth >> 1) - 30;
            layoutParams.setMargins(marginLeft, 0,0,0);
        }else {
            //歌手控件与专辑图片高度 小于 机械臂高度，放大机械臂图标
            //Log.d(TAG, "needleLayoutAdaption: ");
            ViewGroup.LayoutParams paramsSize = mSongLrcBinding.songLrcCenterIvNeedle.getLayoutParams();
            int def = top - bottom - (height >> 1);
            paramsSize.height = height + def;
            paramsSize.width = mSongLrcBinding.songLrcCenterIvNeedle.getWidth() + def / 3 * 2;
            layoutParams.setMargins((mPhoneWidth >> 1) - (isPad ? 38 : 50), 0,0,0);
        }
    }
}
