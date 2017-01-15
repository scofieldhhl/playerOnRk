package org.app.enjoy.music.frag;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.MusicListAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.interfaces.ViewPagerOnPageChangeListener;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.XfDialog;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.musicplayer.MusicActivity;
import org.app.enjoy.musicplayer.PlayMusicActivity;
import org.app.enjoy.musicplayer.R;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Administrator on 2016/6/2.
 */
public class MusicListFragment extends Fragment implements AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener,Observer {

    private String TAG = "MusicListFragment";
    private View view;
    /*** 音乐列表 **/
    private ListView mLvSongs;
    private LayoutInflater inflater;//装载布局
    private ViewGroup.LayoutParams params;
    private int currentPosition = -1;
    private MusicListAdapter musicListAdapter;
    private List<MusicData> musicDatas = new ArrayList<MusicData>();
    private List<MusicData> tmpList = new ArrayList<MusicData>();
    private int currentPlayFrag;//当前播放的Fragment


    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Msg.UPDATE_PLAY_LIST:
                    if (musicDatas != null && musicDatas.size() > 0) {
                        if (musicListAdapter != null) {
                            musicListAdapter.setDatas(musicDatas);
                            musicListAdapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case Contsant.Msg.SEARCH_MUSIC_COMPLETE:
                    if (tmpList != null && tmpList.size() > 0) {
                        musicDatas.addAll(tmpList);
                    }
                    if (musicDatas != null && musicDatas.size() > 0) {
                        if (getActivity() != null) {
                            boolean isMusicLoad = ((MusicActivity)getActivity()).isMusicLoad();
                            if (!isMusicLoad) {
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
                                bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.UPDATE_MUSIC);
                                bundle.putInt(Contsant.POSITION_KEY, currentPosition);
                                DataObservable.getInstance().setData(bundle);
                            }
                        }
                        if (musicListAdapter != null) {
                            //处理当前播放是其他fragment而用户又切换到了播放列表重新找到播放音乐的position
                            if (currentPlayFrag != Contsant.Frag.MUSIC_LIST_FRAG && currentPlayFrag != 0) {
                                int index = MusicUtil.getPositionByMusicName(musicDatas, ((MusicActivity) getActivity()).getCurrentMusicName());
                                if (index != -1) {
                                    currentPosition = index;
                                    musicListAdapter.setDatas(musicDatas);
                                    musicListAdapter.setCurrentPosition(currentPosition);
                                    mLvSongs.setSelection(currentPosition);
                                }
                            } else {
                                if (currentPosition != -1) {
//                                    checkMusicPosition();
                                    musicListAdapter.setDatas(musicDatas);
                                    musicListAdapter.setCurrentPosition(currentPosition);
                                    mLvSongs.setSelection(currentPosition);
                                }
                            }
                        }
                    } else {
                        /*final XfDialog xfdialog = new XfDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.tip)).
                                setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
                                setPositiveButton(getResources().getString(R.string.confrim), null).create();
                        xfdialog.show();*/
                    }
                    break;
                case Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED:
                    if (musicDatas != null && musicDatas.size() > 0) {
                        if (musicListAdapter != null) {
                            if (currentPosition != -1) {
                                musicListAdapter.setDatas(musicDatas);
                                musicListAdapter.setCurrentPosition(currentPosition);
                                mLvSongs.setSelection(currentPosition);
                            }
                        }
                    }
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.music_list,container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (musicDatas != null) {
            musicDatas.clear();
        }
        if (tmpList != null) {
            tmpList.clear();
        }

        initData();
        //异步检索其他音频文件
        new Thread(){
            @Override
            public void run() {
                searchFiles();
            }
        }.start();
    }

    private void initialize (View view) {
        DataObservable.getInstance().addObserver(this);
        mLvSongs = (ListView) view.findViewById(R.id.local_music_list);
        mLvSongs.setOnItemClickListener(this);
        mLvSongs.setOnItemLongClickListener(this);
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        musicListAdapter = new MusicListAdapter(getContext(),mLvSongs);
        musicListAdapter.setDatas(musicDatas);
        mLvSongs.setAdapter(musicListAdapter);


    }

    /**
     * 显示MP3信息,其中_ids保存了所有音乐文件的_ID，用来确定到底要播放哪一首歌曲，_titles存放音乐名，用来显示在播放界面，
     * 而_path存放音乐文件的路径（删除文件时会用到）。
     */
    private void initData () {
        currentPlayFrag = SharePreferencesUtil.getInt(getContext(),Contsant.CURRENT_FRAG);
        currentPosition = SharePreferencesUtil.getInt(getContext(),Contsant.MUSIC_INFO_POSTION);
        List<MusicData> musicList = MusicUtil.getAllSongs(getContext());
        musicDatas.addAll(musicList);
        mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume().......................");
        MobclickAgent.onResume(getActivity());
    }
    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }

    /**
     * 根据Position播放音乐
     */
    public void playMusic(int position, long seekPosition) {
        if (musicDatas.size() > 0) {
            LogTool.i("play---startService");
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            intent.putExtras(bundle);
            intent.setAction("com.app.media.MUSIC_SERVICE");
            intent.putExtra("op", Contsant.PlayStatus.PLAY);// 向服务传递数据
            intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
        } else {
            final XfDialog xfdialog = new XfDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.tip)).
                    setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
                    setPositiveButton(getResources().getString(R.string.confrim), null).create();
            xfdialog.show();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.MUSIC_LIST_FRAG);
        playMusic(position, 0);
        currentPosition = position;
        if (musicListAdapter != null) {
            musicListAdapter.cancelLongClick(true);
        }
        if (musicListAdapter != null && currentPosition != -1) {
            musicListAdapter.setCurrentPosition(currentPosition);
//            sendBroadcast(currentPosition);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.MUSIC_LIST_ITEM_CLICK);
            bundle.putInt(Contsant.POSITION_KEY, currentPosition);
            DataObservable.getInstance().setData(bundle);

        }


    }


    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (musicListAdapter != null) {
            musicListAdapter.cancelLongClick(false);
            musicListAdapter.setCurrentLongPosition(position);
        }

        return true;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            int action = bundle.getInt(Contsant.ACTION_KEY);
            int position = bundle.getInt(Contsant.POSITION_KEY);

            if (action == Contsant.Action.POSITION_CHANGED) {//后台发过来的播放位置改变前台同步改变
                if(position < musicDatas.size()) {
                    if (((MusicActivity) getActivity()).getCurrentPage() == 0) {

                        if (currentPosition != position) {
                            currentPosition = position;
                        }
                        if (currentPlayFrag != Contsant.Frag.MUSIC_LIST_FRAG && currentPlayFrag != 0) {
                            int index = MusicUtil.getPositionByMusicName(musicDatas, ((MusicActivity) getActivity()).getCurrentMusicName());
                            if (index != -1) {
                                currentPosition = index;
                            }
                        }
                        mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
                    }
                }
            }
        } else if (data instanceof Integer) {
            int currentFrag = (int) data;
            if (currentFrag == Contsant.Frag.MUSIC_LIST_FRAG) {
                currentPlayFrag = SharePreferencesUtil.getInt(getContext(),Contsant.CURRENT_FRAG);
                if (currentPlayFrag != Contsant.Frag.MUSIC_LIST_FRAG && currentPlayFrag != 0) {
                    int index = MusicUtil.getPositionByMusicName(musicDatas, ((MusicActivity) getActivity()).getCurrentMusicName());
                    if (index != -1 && currentPosition != index) {
                        currentPosition = index;
                    }
                }
                mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
            }
        }
    }

    @Override
    public void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
    }

    private void searchFiles () {
        long currentTime = System.currentTimeMillis();
        Log.e(TAG, "searchFiles()......start");
//        GetFiles(path);
        MusicUtil.GetMusicFiles(MusicUtil.getSDPath(),true, tmpList);
        Log.e(TAG,"searchFiles()......end");
        Log.e(TAG, "searchFiles()......耗时=" + (System.currentTimeMillis() - currentTime));
        mHandler.sendEmptyMessage(Contsant.Msg.SEARCH_MUSIC_COMPLETE);
    }

    /**
     * 遍历文件夹，搜索指定扩展名的文件
     * isLastOne 是否是最后一条，true 则搜索最后一条记录完毕后通知list刷新数据
     * */
    /*public void GetFiles(String Path) {  //搜索目录，扩展名，是否进入子文件夹
        if(Path == null || TextUtils.isEmpty(Path)){
            return;
        }
        File[] files = new File(Path).listFiles();

        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isFile()) {
                    String[] arrFile = f.getPath().split("\\.");
                    if (arrFile != null && arrFile.length >0) {
                        int length = arrFile.length;
                        if(arrFile[length -1] != null){
                            for(String str : arrExtension){
                                if(arrFile[length -1].equalsIgnoreCase(str)){
                                    MusicData md = new MusicData();
                                    String[] arrFileName = f.getPath().split("/");
                                    md.title = arrFile[length -1];
                                    if(arrFileName != null && arrFileName.length > 0){
                                        md.title = arrFileName[arrFileName.length - 1].substring(0,arrFileName[arrFileName.length - 1].indexOf("."));
                                    }
                                    md.duration = 0;
                                    md.artist = "";
                                    md.displayName = md.title;
                                    md.data = f.getPath();
                                    md.path = f.getPath();
                                    LogTool.i( f.getPath());
                                    md.size = String.valueOf(f.length());
                                    tmpList.add(md);
                                    break;
                                }
                            }
                        }
                    }
                } else if (f.isDirectory() && f.getPath().indexOf("/.") == -1) {  //忽略点文件（隐藏文件/文件夹）
                    GetFiles(f.getPath());
                }
            }
        }
    }

    public String getSDPath(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);   //判断sd卡是否存在
        if(sdCardExist){
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        if(sdDir != null){
            return sdDir.toString();
        }else{
            return null;
        }
    }*/

//    private void checkMusicPosition () {
//        int playId = ((MusicActivity)getActivity()).getCurrentMusicId();
//        if(currentPosition < musicDatas.size()){
//            int id = musicDatas.get(currentPosition).id;
//            if (id != playId &&  playId != -1) {
//                int position = getPositionByMusicId();
//                if (position != -1) {
//                    currentPosition = position;
//                }
//            }
//        }
//    }


}
