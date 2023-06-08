package com.xwrl.mvvm.demo.adapter;

import android.app.Application;

import android.view.View;

import androidx.databinding.ObservableArrayList;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.MediaDirItem;
import com.xwrl.mvvm.demo.bean.MusicDirBean;
import com.xwrl.mvvm.demo.databinding.ItemMusicDirListBinding;


import java.util.Objects;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/18
 * 作用:
 */
public class MusicDirAdapter extends BaseBindingAdapter<MediaDirItem, ItemMusicDirListBinding>{

    private static final String TAG = "MusicDirAdapter";
    private ObservableArrayList<MediaDirItem> mSearchMediaItems, mSheetMediaItems;
    private OnItemClickListener mItemClickListener;

    public interface OnItemClickListener{
        void ItemClickListener(MusicDirAdapter adapter, int position);
        void ItemMoreClickListener(View v, int position);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    public MusicDirAdapter(Application context) {
        super(context);
    }

    @Override
    protected int getLayoutResId(int ViewType) {
        return R.layout.item_music_dir_list;
    }

    @Override
    protected void onBindItem(ItemMusicDirListBinding binding, MediaDirItem item, int position) {
        int number = position;
        String artist ="",
                album = "";
        MusicDirBean bean = new MusicDirBean(String.valueOf(++number),
               item.getTitle(),
                artist,album,
                item.getPath(),
                item.getPath(),
                100000);
        binding.setMusicInfoDir(bean);
        if (mItemClickListener == null) return;
        binding.itemMusicListLayout.setOnClickListener(v -> mItemClickListener.ItemClickListener(this,position));
//        binding.itemLocalMusicMore.setOnClickListener(v -> mItemClickListener.ItemMoreClickListener(v,position));
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

    public void setSheetMediaItems(ObservableArrayList<MediaDirItem> sheetMediaItems) {
        if (sheetMediaItems == null || sheetMediaItems.size() == 0) return;

        this.mSheetMediaItems = new ObservableArrayList<>();
        this.mSheetMediaItems.addAll(sheetMediaItems);

    }

    //搜索集合返回
//    public void searchMediaItems(String s){
//
//        if (mSheetMediaItems == null || mSheetMediaItems.size() == 0 ||
//                s == null || TextUtils.isEmpty(s)) return;
//
//        if (mSearchMediaItems == null) mSearchMediaItems = new ObservableArrayList<>();
//        if (mSearchMediaItems.size() > 0) mSearchMediaItems.clear();
//
//        for (MediaItem m: mSheetMediaItems) {
//            //判断每一项歌名、歌手和专辑名是否包含搜索内容或者其字母大小写
//            String description = m.getDescription().toString().toLowerCase();
//            if (description.contains(s)) {
//                mSearchMediaItems.add(m);
//            }
//        }
//        Log.d(TAG, "searchMediaItems: "+mSearchMediaItems.size());
//        super.setItems(mSearchMediaItems);
//    }
}
