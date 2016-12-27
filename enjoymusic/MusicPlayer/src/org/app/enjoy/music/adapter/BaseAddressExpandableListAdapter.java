package org.app.enjoy.music.adapter;

//**

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.app.enjoy.music.data.AlbumData;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.util.AlbumImgUtil;
import org.app.enjoy.music.view.CircleImageView;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

public class BaseAddressExpandableListAdapter extends BaseExpandableListAdapter {
    private List<AlbumData> mGroupList;
    private List[] mChildList;
    private Context mContext;
    private int mGroupPositionFocus = -1;
    private int mChildPositionFocus = -1;
    public int mMa_data = Contsant.Frag.ALBUM_FRAG;//当前播放列表

    public BaseAddressExpandableListAdapter(Context context,List<AlbumData> groupList, List[] childList, int flag){
        this.mContext = context;
        this.mGroupList = groupList;
        this.mChildList = childList;
        this.mMa_data = flag;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        if(mChildList[groupPosition]!=null && mChildList[groupPosition].size() > 0){
            return (mChildList[groupPosition]).get(childPosition);
        }
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewGroupHolder viewholder;
        if (convertView == null) {
            viewholder = new ViewGroupHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.lv_music_item, null);
            viewholder.mCivAlbum = (CircleImageView) convertView.findViewById(R.id.civ_album);
            viewholder.mMtvTitle = (MovingTextView) convertView.findViewById(R.id.mtv_title);
            viewholder.singers = (TextView) convertView.findViewById(R.id.singer);
            viewholder.times = (TextView) convertView.findViewById(R.id.time);
            viewholder.mIconRemove = (ImageView) convertView.findViewById(R.id.iv_remove);
            viewholder.mIconRemove.setVisibility(View.GONE);
//            viewholder.song_list_item_menu = (ImageButton) convertView.findViewById(R.id.ibtn_song_list_item_menu);
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewGroupHolder) convertView.getTag();
        }

        if (childPosition % 2 == 0) {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.light_blue));
        } else {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.dark_blue));
        }
        if(groupPosition == mGroupPositionFocus && childPosition == mChildPositionFocus){
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.light_yellow));
        }

        MusicData data = (MusicData)getChild(groupPosition,childPosition);
        viewholder.mMtvTitle.setText(data.title);
        viewholder.mCivAlbum.setImageResource(R.drawable.default_album);
        viewholder.singers.setText(data.artist);
        if(data.duration > 0){
            viewholder.times.setText(toTime(data.duration));
        }else{
            viewholder.times.setText("");
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // TODO Auto-generated method stub
        if(mChildList[groupPosition]!=null){
            return mChildList[groupPosition].size();
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        // TODO Auto-generated method stub
        return mGroupList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        // TODO Auto-generated method stub
        return mGroupList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final ViewHolder viewholder;
        if (convertView == null) {
            viewholder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.lv_album_item, null);
            viewholder.mTvTitle = (TextView) convertView.findViewById(R.id.tv_title);
            viewholder.mCivAlbum = (CircleImageView) convertView.findViewById(R.id.civ_album);
            viewholder.mIvExpand = (ImageView) convertView.findViewById(R.id.iv_expand);
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }
        AlbumData data = (AlbumData)getGroup(groupPosition);
        viewholder.mTvTitle.setText(data.getAlbum());
        long albumid = -1;
        try {
            albumid = Long.parseLong(data.getAlbumId());
        }catch (Exception e){
            e.printStackTrace();
        }
        Glide.with(mContext)//加载显示圆形图片
                .load(AlbumImgUtil.getAlbumartPath(data.getMusic().getId(), albumid, mContext))
                .asBitmap()
                .placeholder(R.drawable.default_album)
                .centerCrop()
                .into(new BitmapImageViewTarget(viewholder.mCivAlbum) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        viewholder.mCivAlbum.setImageDrawable(circularBitmapDrawable);
                    }
                });
        if (isExpanded) {
            viewholder.mIvExpand.setImageResource(R.drawable.item_expand);
        } else {
            viewholder.mIvExpand.setImageResource(R.drawable.item_unexpand);
        }
        switch (mMa_data){
            case Contsant.Frag.ALBUM_FRAG:
                convertView.setBackgroundResource(R.drawable.bg_item_group);
                break;
            case Contsant.Frag.ARTIST_FRAG:
                convertView.setBackgroundResource(R.drawable.bg_item_group_artist);
                break;
            default:
                convertView.setBackgroundResource(R.drawable.bg_item_group);
                break;
        }
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }
    /**ExpandableListView 如果子条目需要响应click事件,必需返回true*/
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return true;
    }

    public List[] getmChildList() {
        return mChildList;
    }

    public void setmChildList(List[] mChildList) {
        this.mChildList = mChildList;
    }

    public List<AlbumData> getmGroupList() {
        return mGroupList;
    }

    public void setmGroupList(List<AlbumData> mGroupList) {
        this.mGroupList = mGroupList;
    }

    public int getmGroupPositionFocus() {
        return mGroupPositionFocus;
    }

    public void setmGroupPositionFocus(int mGroupPositionFocus) {
        this.mGroupPositionFocus = mGroupPositionFocus;
    }

    public int getmChildPositionFocus() {
        return mChildPositionFocus;
    }

    public void setmChildPositionFocus(int mChildPositionFocus) {
        this.mChildPositionFocus = mChildPositionFocus;
    }

    public class ViewHolder {
        public CircleImageView mCivAlbum;
        private TextView mTvTitle;
        private ImageView mIvExpand;
    }

    public class ViewGroupHolder {
        public CircleImageView mCivAlbum;
        public MovingTextView mMtvTitle;
        public TextView singers;
        public TextView times;
        public ImageButton song_list_item_menu;
        public ImageView mIconRemove;
    }

    /**
     * 时间的转换
     */
    public String toTime(long time) {

        time /= 1000;
        long minute = time / 60;
        long second = time % 60;
        minute %= 60;
        /** 返回结果用string的format方法把时间转换成字符类型 **/
        return String.format("%02d:%02d", minute, second);
    }
}