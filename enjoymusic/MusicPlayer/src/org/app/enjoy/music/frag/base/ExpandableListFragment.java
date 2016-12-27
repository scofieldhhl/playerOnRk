package org.app.enjoy.music.frag.base;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.BaseAddressExpandableListAdapter;
import org.app.enjoy.music.data.AlbumData;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.music.view.LoadingDialog;
import org.app.enjoy.musicplayer.MusicActivity;
import org.app.enjoy.musicplayer.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by victor on 2016/6/12.
 */
public abstract class ExpandableListFragment extends Fragment implements Observer{
    private View view;
    public int mMa_data = Contsant.Frag.ALBUM_FRAG;//当前播放列表

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Msg.UPDATE_ALBUM_LIST:
                    childList = new ArrayList[albumList.size()];
                    if (tmpChildList != null && tmpChildList.length > 0) {
                        childList = tmpChildList;
                        baseELAdapter = new BaseAddressExpandableListAdapter(getActivity(), albumList, childList, mMa_data);
                        expandableListView.setAdapter(baseELAdapter);
                        baseELAdapter.notifyDataSetChanged();
                    }

                    //add by victor
                    if (childList != null && childList.length > 0 && mGroupPosition < childList.length) {
                        mMusicDatas = childList[mGroupPosition];
                        String currentMusicName = SharePreferencesUtil.getString(getContext(),Contsant.CURRENT_MUSIC_NAME);
                        int index = MusicUtil.getPositionByMusicName(mMusicDatas,currentMusicName);
                        if (mCurrentPosition != index) {
                            mCurrentPosition = index;
                            if (baseELAdapter != null) {
                                baseELAdapter.setmChildPositionFocus(mCurrentPosition);
                                baseELAdapter.setmGroupPositionFocus(mGroupPosition);
                                baseELAdapter.notifyDataSetChanged();
                            }
                        }

                    }
                    break;
                case Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED:
                    if (baseELAdapter != null) {
                        baseELAdapter.setmChildPositionFocus(mCurrentPosition);
                        baseELAdapter.setmGroupPositionFocus(mGroupPosition);
                        baseELAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };
    /*
         * To be overrode in child classes to setup fragment data
         */
    public abstract void setupFragmentData();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_album,container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupFragmentData();
        initData();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(getActivity());
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }

    @Override
    public void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
    }

    private void initialize (View view) {
        DataObservable.getInstance().addObserver(this);
        expandableListView = (ExpandableListView)view.findViewById(R.id.ag_addressbook_ELV);
        GroupClickListener();
        expandableListView.setDividerHeight(0);
        expandableListView.setGroupIndicator(null);
        albumList = new ArrayList<>();
    }

    private void initData () {
        if (albumList != null) {
            albumList.clear();
        }
        new Thread(){
            @Override
            public void run() {
                mGroupPosition = SharePreferencesUtil.getInt(getContext(), Contsant.CURRENT_PLAY_GROUP);
                HashMap<String,MusicData> map;
                switch (mMa_data){
                    case Contsant.Frag.ALBUM_FRAG:
                        map = MusicUtil.getAllAlbum(getContext());
                        break;
                    case Contsant.Frag.ARTIST_FRAG:
                        map = MusicUtil.getAllArtist(getContext());
                        break;
                    default:
                        map = MusicUtil.getAllAlbum(getContext());
                        break;
                }
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String key = (String) entry.getKey();
                    MusicData value = (MusicData) entry.getValue();

                    AlbumData info = new AlbumData();
                    info.setAlbumId(key);
                    info.setMusic(value);
                    switch (mMa_data){
                        case Contsant.Frag.ALBUM_FRAG:
                            info.setAlbum(value.album);
                            break;
                        case Contsant.Frag.ARTIST_FRAG:
                            info.setAlbum(value.artist);
                    }
                    albumList.add(info);
                }

                switch (mMa_data) {
                    case Contsant.Frag.ALBUM_FRAG:
                        tmpChildList = new ArrayList[albumList.size()];
                        if(tmpChildList == null && tmpChildList.length == 0){
                           return;
                        }
                        tmpChildList[mGroupPosition] = MusicUtil.getSongByAlbum(getContext(), albumList.get(mGroupPosition).getAlbumId());
                        break;
                    case Contsant.Frag.ARTIST_FRAG:
                        if(albumList == null || albumList.size() == 0){
                            return;
                        }
                        tmpChildList = new ArrayList[albumList.size()];
                        if(tmpChildList == null && tmpChildList.length == 0){
                            return;
                        }
                        tmpChildList[mGroupPosition] = MusicUtil.getSongsByArtist(getContext(), albumList.get(mGroupPosition).getAlbumId());
                        break;
                }
                mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_ALBUM_LIST);
            }
        }.start();
    }

    /**
     * @param musicDatas
     * 从当前专辑第一首歌曲开始播放
     */
    public void play(List<MusicData> musicDatas, int position) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", 1);// 向服务传递数据
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);
    }

    //------------------------------------------------------------------------------------------------------------------
    private int sign= -1;//控制列表的展开
    public void GroupClickListener(){
        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                LogTool.d("setOnGroupClickListener：" + groupPosition);
                mGroupPosition = groupPosition;
                if (childList[groupPosition] == null) {
                    AddressBookChildrenTask task = new AddressBookChildrenTask();
                    task.execute(Integer.toString(groupPosition));
                    if (progressDialog == null){
                        progressDialog = LoadingDialog.createDialog(getActivity());
                    }
                    progressDialog.show();
                }else{
                    setELVGroup(parent,groupPosition);
                }

                switch (mMa_data) {
                    case Contsant.Frag.ALBUM_FRAG:
                        mMusicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                    case Contsant.Frag.ARTIST_FRAG:
                        mMusicDatas = MusicUtil.getSongsByArtist(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                }
                if (mMusicDatas != null && mMusicDatas.size() > 0) {
                    int index = MusicUtil.getPositionByMusicName(mMusicDatas, ((MusicActivity) getActivity()).getCurrentMusicName());
                    mCurrentPosition = index;
                    mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
                }
                return true;
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
                LogTool.d("groupPosition" + groupPosition + "childPosition" + childPosition);
                SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, mMa_data);
                SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_PLAY_GROUP, groupPosition);
                mMusicDatas = childList[groupPosition];
                if (mMusicDatas != null && mMusicDatas.size() > 0 && childPosition < mMusicDatas.size()) {
                    mCurrentPosition = childPosition;
                    mGroupPosition = groupPosition;
                    play(mMusicDatas, childPosition);

                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) mMusicDatas);
                    bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.MUSIC_LIST_ITEM_CLICK);
                    bundle.putInt(Contsant.POSITION_KEY, childPosition);
                    DataObservable.getInstance().setData(bundle);

                    baseELAdapter.setmGroupPositionFocus(groupPosition);
                    baseELAdapter.setmChildPositionFocus(childPosition);
                    baseELAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });

    }

    public void setELVGroup(ExpandableListView parent,int groupPosition){
        if(parent.isGroupExpanded(groupPosition)){
            expandableListView.collapseGroup(groupPosition);
        }else{
            expandableListView.expandGroup(groupPosition);
            expandableListView.setSelectedGroup(groupPosition);
        }
        for (int i = 0; i < baseELAdapter.getGroupCount(); i++) {
            if (parent.isGroupExpanded(i)) {
                count_expand = i+1;
                break;
            }
            count_expand=0;
        }
        count_expand=1;
        indicatorGroupId = groupPosition;
    }


    /**
     * 子节点异步加载
     */
    public class AddressBookChildrenTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            try{
                int groupPosition = Integer.parseInt(params[0]);
                List<MusicData> musicDatas;
                switch (mMa_data){
                    case Contsant.Frag.ALBUM_FRAG:
                        musicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                    case Contsant.Frag.ARTIST_FRAG:
                        musicDatas = MusicUtil.getSongsByArtist(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                    default:
                        musicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                }
                childList[groupPosition] =  musicDatas;
                return params[0];
            }catch(Exception e){
                e.getStackTrace();
                return "-1";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            /*try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            progressDialog.dismiss();
            if(!result.equals("-1")){
                int tmpGroupPosition = Integer.parseInt(result);
                count_expand=1;
                //expandableListView.collapseGroup(sign);
                // 展开被选的group
                expandableListView.expandGroup(tmpGroupPosition);
                // 设置被选中的group置于顶端
                expandableListView.setSelectedGroup(tmpGroupPosition);
                //sign= tmpGroupPosition;
                indicatorGroupId = tmpGroupPosition;
            }
        }

    }
    private List<AlbumData> albumList;
    private List<MusicData>[] childList;
    private List<MusicData>[] tmpChildList;
    private ExpandableListView expandableListView;
    private BaseAddressExpandableListAdapter baseELAdapter = null;
    private LoadingDialog progressDialog=null;
    private int count_expand = 0;
    private int indicatorGroupHeight;
    private int indicatorGroupId = -1;
    private List<MusicData> mMusicDatas;
    private int mCurrentPosition;
    private int mGroupPosition;

    protected void updateFocus(Observable observable, Object data) {
        LogTool.d("updateFocus");
        if (data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            int action = bundle.getInt(Contsant.ACTION_KEY);
            int position = bundle.getInt(Contsant.POSITION_KEY);
            LogTool.d("updateFocus position:"+ position);
            if (action == Contsant.Action.POSITION_CHANGED) {//后台发过来的播放位置改变前台同步改变
                if (mCurrentPosition != position) {
                    mCurrentPosition = position;
                }
                if (mMa_data != Contsant.Frag.MUSIC_LIST_FRAG && mMa_data != 0) {
                    int index = MusicUtil.getPositionByMusicName(mMusicDatas, ((MusicActivity) getActivity()).getCurrentMusicName());
                    if (mCurrentPosition != index) {
                        mCurrentPosition = index;
                    }
                }
                if (mMusicDatas != null && mMusicDatas.size() > 0) {
                    mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
                }
            }
        } else if (data instanceof Integer) {
            int currentFrag = (int) data;
            LogTool.d("updateFocus currentFrag:" + currentFrag);
            if (mMusicDatas != null && mMusicDatas.size() > 0) {
                if (currentFrag == Contsant.Frag.ALBUM_FRAG || currentFrag == Contsant.Frag.ARTIST_FRAG) {
                    int index = MusicUtil.getPositionByMusicName(mMusicDatas, ((MusicActivity) getActivity()).getCurrentMusicName());
                    mCurrentPosition = index;
                    mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
                }
            } else {
                Log.e("ExpandableListFragment","mMusicDatas == null or mMusicDatas.size == 0");
            }
        }
    }
}
