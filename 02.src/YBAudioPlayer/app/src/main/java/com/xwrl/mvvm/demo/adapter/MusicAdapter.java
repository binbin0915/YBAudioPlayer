package com.xwrl.mvvm.demo.adapter;

import android.app.Application;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.databinding.ObservableArrayList;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.databinding.ItemMusicListBinding;

import java.util.Objects;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/18
 * 作用:
 */
public class MusicAdapter extends BaseBindingAdapter<MediaItem, ItemMusicListBinding>{

    private static final String TAG = "MusicAdapter";
    private ObservableArrayList<MediaItem> mSearchMediaItems, mSheetMediaItems;
    private OnItemClickListener mItemClickListener;

    public interface OnItemClickListener{
        void ItemClickListener(MusicAdapter adapter, int position);
        void ItemMoreClickListener(View v, int position);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    public MusicAdapter(Application context) {
        super(context);
    }

    @Override
    protected int getLayoutResId(int ViewType) {
        return R.layout.item_music_list;
    }

    @Override
    protected void onBindItem(ItemMusicListBinding binding, MediaItem item, int position) {
        int number = position;
        String artist = Objects.requireNonNull(item.getDescription().getSubtitle()).toString(),
                album = Objects.requireNonNull(item.getDescription().getDescription()).toString();
        MusicBean bean = new MusicBean(String.valueOf(++number),
                Objects.requireNonNull(item.getDescription().getTitle()).toString(),
                artist,album,
                Objects.requireNonNull(item.getDescription().getMediaUri()).toString(),
                Objects.requireNonNull(item.getDescription().getMediaUri()).toString(),
                100000);
        binding.setMusicInfo(bean);
        if (mItemClickListener == null) return;
        binding.itemMusicListLayout.setOnClickListener(v -> mItemClickListener.ItemClickListener(this,position));
        binding.itemLocalMusicMore.setOnClickListener(v -> mItemClickListener.ItemMoreClickListener(v,position));
    }

    public void release(){
        super.release();
        if (mItemClickListener != null) { mItemClickListener = null; }
        releaseThisMediaItems();
    }
    public void releaseThisMediaItems(){
        if (mSearchMediaItems != null) {
            if (mSearchMediaItems.size() > 0) { mSearchMediaItems.clear(); }
            mSearchMediaItems = null;
        }
        if (mSheetMediaItems != null) {
            if (mSheetMediaItems.size() > 0) { mSheetMediaItems.clear(); }
            mSheetMediaItems = null;
        }
    }

    public void setSheetMediaItems(ObservableArrayList<MediaItem> sheetMediaItems) {
        if (sheetMediaItems == null || sheetMediaItems.size() == 0) return;

        this.mSheetMediaItems = new ObservableArrayList<>();
        this.mSheetMediaItems.addAll(sheetMediaItems);

    }

    //搜索集合返回
    public void searchMediaItems(String s){

        if (mSheetMediaItems == null || mSheetMediaItems.size() == 0 ||
                s == null || TextUtils.isEmpty(s)) return;

        if (mSearchMediaItems == null) mSearchMediaItems = new ObservableArrayList<>();
        if (mSearchMediaItems.size() > 0) mSearchMediaItems.clear();

        for (MediaItem m: mSheetMediaItems) {
            //判断每一项歌名、歌手和专辑名是否包含搜索内容或者其字母大小写
            String description = m.getDescription().toString().toLowerCase();
            if (description.contains(s)) {
                mSearchMediaItems.add(m);
            }
        }
        Log.d(TAG, "searchMediaItems: "+mSearchMediaItems.size());
        super.setItems(mSearchMediaItems);
    }
}
