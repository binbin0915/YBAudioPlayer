package com.xwrl.mvvm.demo.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xwrl.mvvm.demo.BaseDirActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.adapter.MusicDirAdapter;

import com.xwrl.mvvm.demo.bean.MediaDirItem;
import com.xwrl.mvvm.demo.databinding.ActivityDirMusicBinding;
import com.xwrl.mvvm.demo.viewmodel.MusicDirViewModel;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;

public class MusicDirActivity extends BaseDirActivity<MusicDirViewModel> {
    private static final String TAG = "MusicDirActivity";
    private MusicDirViewModel mMusicDirViewModel;
    private MusicDirAdapter mMusicDirAdapter;
    private ActivityDirMusicBinding mMusicDirBinding;

    private MusicDirActivity.MyAdapterItemClickListener mItemClickListener;

    private  Map<String,Integer> mapDirs=new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMusicDirBinding = DataBindingUtil.setContentView(this, R.layout.activity_dir_music);
        mMusicDirViewModel = new MusicDirViewModel(getApplication());
        mMusicDirBinding.setMusicInfo(mMusicDirViewModel);

        initView();

    }
    private void initView() {
        mMusicDirBinding.musicActivityUiRoot.setOnApplyWindowInsetsListener(this);

//        mMusicDirBinding.musicActivityIvReturn.setOnClickListener(v -> returnClick());

//        mMusicDirBinding.mainActivityBottomLayout.setOnClickListener(v -> openActivity(0));

//        mMusicDirBinding.musicActivityIvSearch.setOnClickListener(this::showEditText);

//        mMusicDirBinding.mainActivityBottomProgressBar.setOnClickListener(v -> mMusicViewModel.playbackButton());
        //初始化唱片旋转动画
//        super.initAnimation(mMusicBinding.mainActivityBottomIvAlbum);
        //初始化RecyclerView
        mMusicDirBinding.musicActivityRvMusic.setLayoutManager(new LinearLayoutManager(getApplication()));
        mMusicDirAdapter = new MusicDirAdapter(getApplication());

        mMusicDirBinding.musicActivityRvMusic.setAdapter(mMusicDirAdapter);
        mItemClickListener = new MyAdapterItemClickListener();
        mMusicDirAdapter.setItemClickListener(mItemClickListener);
        //设置深色模式适配的颜色
        int color = super.getViewColor();

    }
    private class MyAdapterItemClickListener implements MusicDirAdapter.OnItemClickListener{

        @Override
        public void ItemClickListener(MusicDirAdapter adapter, int position) {
//            adapter.getItems().get(position)
            MusicActivity.mediaDirItem = adapter.getItems().get(position);
            startActivity(new Intent(MusicDirActivity.this, MusicActivity.class));

        }

        @Override
        public void ItemMoreClickListener(View v, int position) {
            Log.d(TAG, "ItemMoreClickListener: 点击了更多 "+position);
        }
    }
    @Override
    protected MediaControllerCompat.Callback getControllerCallback() {
        return new MusicDirActivity.MyMediaControllerCallback();
    }

    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() {
        return new MusicDirActivity.MyMediaBrowserSubscriptionCallback();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            Log.d(TAG, "onChildrenLoaded: ");

            List<MediaDirItem> childrenDir = new ArrayList<>();
            
            children.forEach(item->{
               String fullpath  = item.getDescription().getMediaUri().getPath();
               String path  = fullpath.substring(0,fullpath.lastIndexOf("/"));
                String title  = path.substring(path.lastIndexOf("/")+1);
                MediaDirItem  md = new MediaDirItem();
                md.setTitle(title);
                md.setPath(path);
                if(!mapDirs.containsKey(path)){
                    childrenDir.add(md);
                    mapDirs.put(path,1);
                }


            });
            mMusicDirAdapter.setItems(childrenDir);
//            activityOnChildrenLoad(mMusicDirViewModel, mMusicDirBinding.mainActivityIvPlayLoading, children);
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.w(TAG, "onMetadataChanged() returned: ");
            mMusicDirViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            int state = playbackState.getState();
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
            mMusicDirViewModel.setPlaybackState(state);
//            playbackStateChanged(playbackState, mMusicDirBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }
    }
}
