package com.xwrl.mvvm.demo.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.adapter.MusicAdapter;
import com.xwrl.mvvm.demo.bean.MediaDirItem;
import com.xwrl.mvvm.demo.databinding.ActivityMusicBinding;
import com.xwrl.mvvm.demo.util.ImmersiveStatusBarUtil;
import com.xwrl.mvvm.demo.viewmodel.MusicViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/17
 * 作用:
 */
public class MusicActivity extends BaseActivity<MusicViewModel>{

    private static final String TAG = "MusicActivity";

    private ActivityMusicBinding mMusicBinding;
    private MusicViewModel mMusicViewModel;
    private MusicAdapter mMusicAdapter;
    private MyAdapterItemClickListener mItemClickListener;
    public  static MediaDirItem mediaDirItem;

    private Timer mTimer;

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() { return new MyMediaControllerCallback(); }
    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() { return new MyMediaBrowserSubscriptionCallback(); }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMusicBinding = DataBindingUtil.setContentView(this, R.layout.activity_music);
        mMusicViewModel = new MusicViewModel(getApplication());
        mMusicBinding.setMusicInfo(mMusicViewModel);

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
        StopProgressBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mItemClickListener != null) { mItemClickListener = null; }
        if (mMusicAdapter != null) {
            mMusicAdapter.release();
            mMusicAdapter = null;
        }
        if (mMusicViewModel != null) { mMusicViewModel = null; }
        if (mMusicBinding != null) {
            mMusicBinding.unbind();
            mMusicBinding = null;
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //当打开软键盘时，再按返回按键，则由软键盘本身消耗本次按键事件，此处不会收到该按键回调
        if (keyCode == KeyEvent.KEYCODE_BACK) return returnClick();
        return super.onKeyDown(keyCode, event);
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            Log.d(TAG, "onChildrenLoaded: ");

            List<MediaItem> childrenFilter = new ArrayList<>();
            children.forEach(item->{
                if(item.getDescription().getMediaUri().getPath().contains(mediaDirItem.getPath())){
                    childrenFilter.add(item);
                }


            });

            mMusicAdapter.setItems(childrenFilter);
            activityOnChildrenLoad(mMusicViewModel, mMusicBinding.mainActivityIvPlayLoading, childrenFilter);
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.w(TAG, "onMetadataChanged() returned: ");
            mMusicViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            int state = playbackState.getState();
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
            mMusicViewModel.setPlaybackState(state);
            playbackStateChanged(playbackState, mMusicBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }
    }

    private void initView() {
        mMusicBinding.musicActivityUiRoot.setOnApplyWindowInsetsListener(this);

        mMusicBinding.musicActivityIvReturn.setOnClickListener(v -> returnClick());

        mMusicBinding.mainActivityBottomLayout.setOnClickListener(v -> openActivity(0));

        mMusicBinding.musicActivityIvSearch.setOnClickListener(this::showEditText);

        mMusicBinding.mainActivityBottomProgressBar.setOnClickListener(v -> mMusicViewModel.playbackButton());
        //初始化唱片旋转动画
        super.initAnimation(mMusicBinding.mainActivityBottomIvAlbum);
        //初始化RecyclerView
        mMusicBinding.musicActivityRvMusic.setLayoutManager(new LinearLayoutManager(getApplication()));
        mMusicAdapter = new MusicAdapter(getApplication());

        mMusicBinding.musicActivityRvMusic.setAdapter(mMusicAdapter);
        mItemClickListener = new MyAdapterItemClickListener();
        mMusicAdapter.setItemClickListener(mItemClickListener);
        //设置深色模式适配的颜色
        int color = super.getViewColor();
        mMusicBinding.mainActivityBottomIvList.getDrawable().setTint(color);
        mMusicBinding.mainActivityBottomProgressBar.setProgressColor(color);
    }
    /*
    * 音乐列表点击事件回调
    * */
    private class MyAdapterItemClickListener implements MusicAdapter.OnItemClickListener{
        @Override
        public void ItemClickListener(MusicAdapter adapter, int position) {
            MediaControllerCompat mediaController =
                    MediaControllerCompat.getMediaController(MusicActivity.this);
            String mediaId = mediaController.getMetadata()
                    .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                    currentMediaId = adapter.getItems().get(position).getMediaId();
            if (mediaId.equals(currentMediaId)) {
                MusicActivity.this.mMusicViewModel.playbackButton();
            } else mediaController.getTransportControls().playFromMediaId(currentMediaId, null);

            Log.d(TAG, "ItemClickListener: 点击了 "+mediaId+", "+currentMediaId);
        }

        @Override
        public void ItemMoreClickListener(View v, int position) {
            Log.d(TAG, "ItemMoreClickListener: 点击了更多 "+position);
        }
    }

    private void UpdateProgressBar() {
        if (mTimer != null) { return; }

        mTimer = new Timer();
        mTimer.schedule(mMusicViewModel.getCircleBarTask(),300,300);
    }

    private void StopProgressBar() {
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }

    private boolean returnClick(){
        if (mMusicViewModel != null && mMusicViewModel.isShowSearchView()){
            mMusicBinding.musicActivityTopEdit
                    .removeTextChangedListener(mMusicViewModel.getTextListener());
            //失去焦点前调用才有效
            mMusicBinding.musicActivityRvSearchMusic.setAdapter(null);
            ImmersiveStatusBarUtil.HideSoftInput(this);
            mMusicBinding.musicActivityTopEdit.clearFocus();
            mMusicViewModel.setSearchGroupVisible(false);
        }else {
            this.finish();
            overridePendingTransition(0,R.anim.push_out);
        }
        Log.d(TAG, "returnClick: ");
        return true;
    }

    private void showEditText(View v){
        mMusicBinding.musicActivityTopEdit.addTextChangedListener(mMusicViewModel.getTextListener());
        //Log.d(TAG, "showEditText: 音乐列表是否显示 "+mMusicBinding.musicActivityRvMusic.getVisibility());
        mMusicViewModel.setSearchGroupVisible(true);
        mMusicBinding.musicActivityTopEdit.setVisibility(View.VISIBLE);
        mMusicBinding.musicActivityTopEdit.requestFocus();
        //EditText处于显示状态且获取焦点后调用软键盘才有效
        ImmersiveStatusBarUtil.ShowSoftInput(getApplication(),mMusicBinding.musicActivityTopEdit);
        //初始化RecyclerView
        mMusicBinding.musicActivityRvSearchMusic.setLayoutManager(new LinearLayoutManager(getApplication()));
        MusicAdapter adapter = new MusicAdapter(getApplication());
        adapter.setSheetMediaItems(mMusicAdapter.getItems());
        mMusicBinding.musicActivityRvSearchMusic.setAdapter(adapter);
        mMusicViewModel.setAdapter(adapter);
        adapter.setItemClickListener(mItemClickListener);
        //if (mMusicAdapter != null) mMusicAdapter.setItems()
    }

    private void openActivity(int mode){
        if (mode == 0) {
            startActivity(new Intent(MusicActivity.this, SongLrcActivity.class));
            overridePendingTransition(R.anim.push_in,0);
        }

    }
}
