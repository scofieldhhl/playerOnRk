package org.app.enjoy.music.frag;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.BaseAddressExpandableListAdapter;
import org.app.enjoy.music.data.AlbumData;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.frag.base.ExpandableListFragment;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.music.view.LoadingDialog;
import org.app.enjoy.musicplayer.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * Created by victor on 2016/6/12.
 */
public class AlbumFragment extends ExpandableListFragment {

    @Override
    public void setupFragmentData() {
        mMa_data = Contsant.Frag.ALBUM_FRAG;
    }

    @Override
    public void update(Observable observable, Object data) {
        updateFocus(observable, data);
    }
    /*private View view;
    private ListView mLvAlbum;

    private AlbumAdapter albumAdapter;
    private List<AlbumData> albumList = new ArrayList<>();

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Msg.UPDATE_ALBUM_LIST:
                    if (albumAdapter != null) {
                        albumAdapter.setDatas(albumList);
                        albumAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_album,container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initialize(view);
        initData();
        MobclickAgent.onResume(getActivity());
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }

    private void initialize (View view) {
        mLvAlbum = (ListView) view.findViewById(R.id.lv_album);
        mLvAlbum.setOnItemClickListener(this);
        albumAdapter = new AlbumAdapter(getActivity());
        albumAdapter.setDatas(albumList);
        mLvAlbum.setAdapter(albumAdapter);
    }

    private void initData () {
        if (albumList != null) {
            albumList.clear();
        }
        new Thread(){
            @Override
            public void run() {
                HashMap<String,MusicData> map = MusicUtil.getAllAlbum(getContext());
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String key = (String) entry.getKey();
                    MusicData value = (MusicData) entry.getValue();

                    AlbumData info = new AlbumData();
                    info.setAlbumId(key);
                    info.setMusic(value);
                    info.setAlbum(value.album);
                    albumList.add(info);
                }
                mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_ALBUM_LIST);
            }
        }.start();
    }

    *//**
     * @param musicDatas
     * 从当前专辑第一首歌曲开始播放
     *//*
    public void play(List<MusicData> musicDatas) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, 0);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", 1);// 向服务传递数据
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.ALBUM_FRAG);
        List<MusicData> musicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(position).getAlbumId());
        if (musicDatas != null && musicDatas.size() > 0) {
            if (albumAdapter != null) {
                albumAdapter.setCurrentPosition(position);
            }
            play(musicDatas);
//            Bundle bundle = new Bundle();
//            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
//            bundle.putInt(Contsant.POSITION_KEY, 0);
//
//            Intent intent = new Intent();
//            intent.setAction(Contsant.PlayAction.MUSIC_LIST);
//            intent.putExtras(bundle);
//            getActivity().sendBroadcast(intent);

            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.MUSIC_LIST_ITEM_CLICK);
            bundle.putInt(Contsant.POSITION_KEY, 0);
            DataObservable.getInstance().setData(bundle);


        }
    }*/
}
