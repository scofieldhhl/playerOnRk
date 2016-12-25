package org.app.enjoy.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.app.enjoy.music.adapter.ViewPagerAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.frag.AlbumFragment;
import org.app.enjoy.music.frag.ArtistFragment;
import org.app.enjoy.music.frag.DiyFragment;
import org.app.enjoy.music.frag.MusicListFragment;
import org.app.enjoy.music.frag.SearchMusicFragment;
import org.app.enjoy.music.interfaces.ViewPagerOnPageChangeListener;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.XfDialog;
import org.app.enjoy.music.util.AlbumImgUtil;
import org.app.enjoy.music.util.ImageDownLoader;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.music.view.AttrViewPager;
import org.app.enjoy.music.view.CategoryPopWindow;
import org.app.enjoy.music.view.CircleImageView;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.music.view.PagerSlidingTabStrip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class MusicActivity extends BaseActivity implements View.OnClickListener,Observer,View.OnTouchListener{
    private String TAG = "MusicActivity";
    private MusicListFragment musicListFragment;
    private ArtistFragment artistFragment;
    private AlbumFragment albumFragment;
    private DiyFragment diyFragment;
    private SearchMusicFragment searchMusicFragment;
    private List<Fragment> frags = new ArrayList<>();
    private ViewPagerAdapter viewPagerAdapter;
    private ImageView mIvSearch,mIvPlay,mIvAdd;
    private LinearLayout mLayoutPlayBottom,mLayoutPlayBottomRight;
    private PagerSlidingTabStrip tabs;
    private AttrViewPager viewPager;
    private CircleImageView mCivAlbum;
    private MovingTextView mMtvTitle;
    private int playStatus;
    private String[] titles = { "All Songs", "Artist","Album", "PlayLists"};
    private List<MusicData> musicDatas = new ArrayList<>();
    private int currentPosition = -1;
//    private int currentMusicId  = -1;//当前播放音乐id
    private int isPlaying;//后台发过来的播放状态
    private CategoryPopWindow categoryPopWindow;

    private float xLast,yLast;
    private boolean isNext = false;//是否是下一曲
    private boolean isClick = true;//判断是否是点击事件
    private boolean isMusicLoad = false;//判断是否播放列表加载完毕
    private int currentPlayFrag;//当前播放的Fragment
    private ImageDownLoader imageDownLoader;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED:
                    showPlayInfo();
                    break;
                case Contsant.Msg.SHOW_BOTTOM_PLAY_INFO:
                    showPlayInfo();
                    break;
                case Contsant.Action.MUSIC_STOP:
                    mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
                    break;
                case Contsant.PlayStatus.PAUSE:
                    mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
                    break;
                case Contsant.Action.PLAY_PAUSE_MUSIC:
                    if (isPlaying == 1) {
                        playStatus = Contsant.PlayStatus.PLAY;
                        mIvPlay.setImageResource(R.drawable.icon_play_bottom_pause);
                    } else if (isPlaying == 0) {
                        playStatus = Contsant.PlayStatus.PAUSE;
                        mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initialize () {
        registerMusicReceiver();
        DataObservable.getInstance().addObserver(this);
        imageDownLoader = new ImageDownLoader(this);
        currentPlayFrag = SharePreferencesUtil.getInt(this,Contsant.CURRENT_FRAG);
        currentPosition = SharePreferencesUtil.getInt(this,Contsant.MUSIC_INFO_POSTION);
        categoryPopWindow = new CategoryPopWindow(this);

        mIvSearch = (ImageView) findViewById(R.id.iv_search);
        mIvAdd = (ImageView) findViewById(R.id.iv_add);
        mIvPlay = (ImageView) findViewById(R.id.iv_play);
        mLayoutPlayBottom = (LinearLayout) findViewById(R.id.l_play_bottom);
        mLayoutPlayBottomRight = (LinearLayout) findViewById(R.id.l_play_bottom_right);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        viewPager = (AttrViewPager) findViewById(R.id.viewpager);
        viewPager.setCanScroll(false);//屏蔽左右滑动

        musicListFragment = new MusicListFragment();
        artistFragment = new ArtistFragment();
        albumFragment = new AlbumFragment();
        diyFragment = new DiyFragment();
        searchMusicFragment = new SearchMusicFragment();

        frags.add(musicListFragment);
        frags.add(artistFragment);
        frags.add(albumFragment);
        frags.add(diyFragment);
        frags.add(searchMusicFragment);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.setTitles(titles);
        viewPagerAdapter.setFrags(frags);
        viewPager.setAdapter(viewPagerAdapter);

        tabs.setViewPager(viewPager);
        mCivAlbum = (CircleImageView) findViewById(R.id.civ_album);
        mMtvTitle = (MovingTextView) findViewById(R.id.mtv_title);

        mIvSearch.setOnClickListener(this);
        mIvPlay.setOnClickListener(this);
        mIvAdd.setOnClickListener(this);
        mLayoutPlayBottom.setOnTouchListener(this);
        mLayoutPlayBottom.setOnClickListener(this);

    }

    private void showPlayInfo () {
        if (musicDatas != null && musicDatas.size() > 0) {
            if (currentPosition < musicDatas.size() && currentPosition != -1) {
                String currentMusicName = musicDatas.get(currentPosition).title;
                SharePreferencesUtil.putString(this, Contsant.CURRENT_MUSIC_NAME, currentMusicName);
                mMtvTitle.setText(musicDatas.get(currentPosition).title);
                if (MusicService.flag == 1){
                    playStatus = Contsant.PlayStatus.PLAY;
                    mIvPlay.setImageResource(R.drawable.icon_play_bottom_pause);
                } else {
                    playStatus = Contsant.PlayStatus.PAUSE;
                    mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
                }
                String albumId = musicDatas.get(currentPosition).getAlbumId();
                Bitmap bitmap;
                if (!TextUtils.isEmpty(albumId)) {
                    bitmap = imageDownLoader.showCacheBitmap(albumId);
                    if (bitmap != null) {
                        mCivAlbum.setImageBitmap(bitmap);
                    } else {
                        imageDownLoader.getAlbumImage(this, musicDatas.get(currentPosition).getId(), albumId, new ImageDownLoader.onImageLoaderListener() {
                            @Override
                            public void onImageLoader(Bitmap bitmap) {
                                if (bitmap != null) {
                                    mCivAlbum.setImageBitmap(bitmap);
                                } else {
                                    mCivAlbum.setImageResource(R.drawable.default_album);
                                }
                            }
                        });
                    }
                } else {
                    mCivAlbum.setImageResource(R.drawable.default_album);
                }
            } else {
                Log.e(TAG, "showPlayInfo() error............");
            }
        } else {
            Log.e(TAG,"musicDatas == null or musicDatas.size() == 0");
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float xDistance,yDistance;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = 0f;
                yDistance = 0f;
                xLast = event.getX();
                yLast = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (musicDatas != null && musicDatas.size() > 0 && !isClick) {
                    if (isNext) {
                        //下一曲
                        currentPosition ++;
                        if (currentPosition == musicDatas.size()) {
                            currentPosition = 0;
                        }
                    } else {
                        //上一曲
                        currentPosition --;
                        if (currentPosition < 0) {
                            currentPosition = musicDatas.size() - 1;
                        }
                    }
                    //如果当前是播放列表MusicListFragment或者搜索SearchMusicFragment则通知播放位置发生改变同步更新UI
                    if (viewPagerAdapter != null) {
//                        if (viewPager.getCurrentItem() == 0 || viewPager.getCurrentItem() == 3 || viewPager.getCurrentItem() == 4) {
                            SharePreferencesUtil.putString(this,Contsant.CURRENT_MUSIC_NAME,musicDatas.get(currentPosition).title);
                            Bundle bundle = new Bundle();
                            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
                            bundle.putInt(Contsant.POSITION_KEY, currentPosition);
                            DataObservable.getInstance().setData(bundle);
//                        }
                    }
                    play();
                    isNext = false;
                    isClick = true;
                }else if(isClick){
                    if(currentPosition < musicDatas.size() && currentPosition != -1){
                        playMusic(currentPosition,musicDatas.get(currentPosition).seekPostion);
                        isClick = true;
                    }
                }else {
                    if(currentPosition < musicDatas.size() && currentPosition != -1){
                        playMusic(currentPosition,musicDatas.get(currentPosition).seekPostion);
                        isClick = true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float curX = event.getX();
                float curY = event.getY();
                xDistance = curX - xLast;
                yDistance = curY - yLast;
                float distance = Math.abs(xDistance) - Math.abs(yDistance);
                LogTool.d("distance:" + distance);
                if(distance == 0 || distance < 10){
                    isClick = true;
                } else if(distance > 50){
                    if(xDistance > 50){//从左向右滑动并且x方向比y方向滑动距离>50
                        Log.e(TAG,">>>>>>>>>>>>>>>>>>>>>>>>>>>>>从左向右滑动xDistance = " + xDistance);
                        isNext = false;
                    } else if (xDistance < -50){//从右向左滑动
                        Log.e(TAG,"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<从右向左滑动xDistance = " + xDistance);
                        isNext = true;
                    }
                    isClick = false;
                } else {
                    isClick = true;
                }
                break;
        }
        return true;
    }

    /**
     * 退出程序方法
     */
    private void exit(){
        Intent mediaServer = new Intent(MusicActivity.this, MusicService.class);
        stopService(mediaServer);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_search:
                if (viewPager != null) {
                    viewPager.setCurrentItem(4);
                }
                break;
            case R.id.iv_play:
                if (musicDatas != null && musicDatas.size() > 0) {
                    if (playStatus == Contsant.PlayStatus.PLAY) {
                        pause();
                        playStatus = Contsant.PlayStatus.PAUSE;

                    } else {
                        play();
                        playStatus = Contsant.PlayStatus.PLAY;
                    }
                }
                break;
            case R.id.iv_add:
                if (musicDatas != null && musicDatas.size() > 0 && currentPosition != -1) {
                    if (currentPosition < musicDatas.size()) {
                        categoryPopWindow.setData(musicDatas.get(currentPosition));
                        categoryPopWindow.showPopWindow(mLayoutPlayBottomRight);
                    }
                }
                break;
            case R.id.l_play_bottom:
                LogTool.d("l_play_bottom:");
                playMusic(currentPosition, musicDatas.get(currentPosition).seekPostion);
                break;
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            int action = bundle.getInt(Contsant.ACTION_KEY);
            int position = bundle.getInt(Contsant.POSITION_KEY);

            if (action == Contsant.Action.UPDATE_MUSIC) {
                if (position != -1) {
                    List<MusicData> musicList = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
                    if (musicList != null && musicList.size() > 0) {
                        if (musicDatas != null) {
                            musicDatas.clear();
                            musicDatas.addAll(musicList);
                            isMusicLoad = true;
                            currentPosition = position;

                            int index = getPositionByMusicName();
                            if (index != -1) {
                                currentPosition = index;
                            }
//                            checkMusicPosition();
                            mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
                        }
                    }
                }
            } else  if (action == Contsant.Action.MUSIC_LIST_ITEM_CLICK) {
                if (bundle.containsKey(Contsant.MUSIC_LIST_KEY)) {
                    List<MusicData> musicList = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
                    if (musicList != null && musicList.size() > 0) {
                        if (musicDatas != null) {
                            musicDatas.clear();
                            musicDatas.addAll(musicList);
                        }
                    }
                    currentPosition = position;
                    mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
                }
            } else if (action == Contsant.Action.MUSIC_STOP) {//后台发过来的播放位置改变前台同步改变
                pause();
            } else if (action == Contsant.Action.REMOVE_MUSIC) {
                if (musicDatas != null && musicDatas.size() > 0) {
                    if (position < musicDatas.size()) {
                        if (position != -1) {
                            musicDatas.remove(position);
                        }
                    }
                }
            } else if (action == Contsant.Action.POSITION_CHANGED) {//后台发过来的播放位置改变前台同步改变
                if (position < musicDatas.size()) {
                    if (currentPosition != position) {
                        currentPosition = position;
                        mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
        unregisterReceiver(musicReceiver);
    }

    public void play() {
        Log.e(TAG,"play-currentPosition=" + currentPosition);
        mIvPlay.setImageResource(R.drawable.icon_play_bottom_pause);
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, currentPosition);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", Contsant.PlayStatus.PLAY);// 向服务传递数据
        intent.setPackage(getPackageName());
        startService(intent);
        showPlayInfo();
    }

    /**
     * 暂停
     */
    public void pause() {
        mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, currentPosition);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", Contsant.PlayStatus.PAUSE);
        intent.setPackage(getPackageName());
        startService(intent);
    }

    /**
     * 初始化注册广播
     */
    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contsant.PlayAction.MUSIC_STOP);
        filter.addAction(Contsant.PlayAction.PLAY_PAUSE_NEXT);
        registerReceiver(musicReceiver, filter);

    }
    /**在后台MusicService里使用handler消息机制，不停的向前台发送广播，广播里面的数据是当前mp播放的时间点，
     * 前台接收到广播后获得播放时间点来更新进度条,暂且先这样。但是一些人说虽然这样能实现。但是还是觉得开个子线程不错**/
    protected BroadcastReceiver musicReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Contsant.PlayAction.MUSIC_STOP)) {
                mHandler.sendEmptyMessage(Contsant.Action.MUSIC_STOP);
            } else if(action.equals(Contsant.PlayAction.PLAY_PAUSE_NEXT)){
                isPlaying = intent.getExtras().getInt("isPlaying");
                mHandler.sendEmptyMessage(Contsant.Action.PLAY_PAUSE_MUSIC);
            }
        }
    };

    /**
     * 根据Position播放音乐
     */
    public void playMusic(int position, long seekPosition) {
        finish();
        if (musicDatas.size() > 0) {
            startActivity(new Intent(MusicActivity.this,MusicPlayActivity.class));
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            Intent intent = new Intent();
            intent.setAction(Contsant.PlayAction.MUSIC_LIST);
            intent.putExtras(bundle);
            sendBroadcast(intent);
        } else {
            final XfDialog xfdialog = new XfDialog.Builder(MusicActivity.this).setTitle(getResources().getString(R.string.tip)).
                    setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
                    setPositiveButton(getResources().getString(R.string.confrim), null).create();
            xfdialog.show();
        }
    }

    public int getCurrentPage () {
        if (viewPager != null) {
            return viewPager.getCurrentItem();
        }
        return 0;
    }

    public String getCurrentMusicName () {
        return SharePreferencesUtil.getString(this, Contsant.CURRENT_MUSIC_NAME);
    }

    public boolean isMusicLoad () {
        return isMusicLoad;
    }

    private int getPositionByMusicName () {
        int position = -1;
        if (musicDatas == null || musicDatas.size() == 0) {
            return -1;
        }
        String currentMusicNme = getCurrentMusicName();
        if (!TextUtils.isEmpty(currentMusicNme)) {
            for (int i=0;i<musicDatas.size();i++) {
                if (currentMusicNme.equals(musicDatas.get(i).title)) {
                    position = i;
                    break;
                }
            }
        }
        Log.e(TAG, "getPositionByMusicName-position = " + position);
        return position;
    }

//    private void checkMusicPosition () {
//        int playId = getCurrentMusicId();
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
