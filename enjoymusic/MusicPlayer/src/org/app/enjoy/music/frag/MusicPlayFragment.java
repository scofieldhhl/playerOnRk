package org.app.enjoy.music.frag;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.LogService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LRCbean;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.XfDialog;
import org.app.enjoy.music.util.AlbumImgUtil;
import org.app.enjoy.music.util.CubeLeftOutAnimation;
import org.app.enjoy.music.util.CubeLeftOutBackAnimation;
import org.app.enjoy.music.util.CubeRightInAnimation;
import org.app.enjoy.music.util.CubeRightInBackAnimation;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.music.view.CircleImageView;
import org.app.enjoy.music.view.CircularSeekBar;
import org.app.enjoy.music.view.DefaultLrcBuilder;
import org.app.enjoy.music.view.FlingGalleryView;
import org.app.enjoy.music.view.ILrcBuilder;
import org.app.enjoy.music.view.ILrcView;
import org.app.enjoy.music.view.LrcRow;
import org.app.enjoy.music.view.LrcView;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.musicplayer.MusicActivity;
import org.app.enjoy.musicplayer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Created by Administrator on 2016/6/2.
 */
public class MusicPlayFragment extends Fragment implements View.OnClickListener,CircularSeekBar.OnCircularSeekBarChangeListener,Observer,View.OnTouchListener {
    private final int DELAY_CHANGE_PROGRESS = 2 * 1000;
    private MovingTextView mMTvMusicName;// 音乐名
    private String mMusicName = "";
	private TextView mTvFormat, mTvSimapleRate, mTvBitRate;// 艺术家
    private ImageButton mIbFore;
    private ImageButton mIbPlay;// 播放按钮
    private ImageButton mIbPre;// 上一首
    private ImageButton mIbNext,mIbShare;// 下一首
    private ImageButton mIbLoopMode;//播放模式切换按钮
	private TextView mTvCurrentTime,mTvDurationTime;// 歌曲时间
    private CircularSeekBar mCsbProgress;// 进度条
    private int position;// 定义一个位置，用于定位用户点击列表曲首
    private long currentTime;// 当前播放位置
    private long duration;// 总时间
	private ImageView mIbBack,mIbBalance;
	private CircleImageView mIvMusicCd;

    public static int loop_flag = Contsant.LoopMode.LOOP_ALL;
    public static boolean random_flag = false;
    public static int[] randomIDs = null;
    public static int randomNum = 0;
    private int flag = Contsant.PlayStatus.PAUSE;// 标记
    private TreeMap<Integer, LRCbean> lrc_map = new TreeMap<Integer, LRCbean>();// Treemap对象
    private AudioManager audioManager;
    private int maxVolume;// 最大音量
    private int currentVolume;// 当前音量
    private ImageView mIbVoice;//右上角的音量图标
    private Toast toast;//提示消息
    private Context mContext;//上下文。这个重要！
    private Animation rotateAnim;//音乐光盘旋转动画
    private boolean isSilence = false;//是否静音

	private List<MusicData> musicDatas;

    private CubeLeftOutAnimation cubeLeftOutAnimation;
    private CubeRightInAnimation cubeRightInAnimation;
    private CubeLeftOutBackAnimation cubeLeftOutBackAnimation;
    private CubeRightInBackAnimation cubeRightInBackAnimation;
    private long mSeekPosition = 0L;
	private FlingGalleryView mFlingView;
	private TextView mTvlrcText;
	private LrcView mLrcView;
	private int mPalyTimerDuration = 1000;
	private Timer mTimer;
	private TimerTask mTask;
	private ImageView mIvBgLrc;
	private ArrayList<String> mLrcPathlist = new ArrayList<>();
	private String mSimpleRate = "", mBitRate = "", mMusicFormat;
    private XfDialog popupWindow;
    private int mMa_data;//当前播放列表
    private List<MusicData> mMusicDatasNull = new ArrayList<>();
    private boolean isCatchLog = true;
    private boolean isSeeking = false;
    private ImageView mIvNoTouch1, mIvNoTouch2, mIvNoTouch3;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Contsant.Action.CURRENT_TIME_MUSIC:
                    showPlayingTime(currentTime, duration);
                    showAudioInfo();
					updateLRC(currentTime);
                    break;
                case Contsant.Action.DURATION_MUSIC:
					showPlayingTime(currentTime, duration);
                    showAudioInfo();
                    break;
                case Contsant.Action.NEXTONE_MUSIC:
                    nextOne();
                    break;
                case Contsant.Action.UPDATE_MUSIC:
                    DataObservable.getInstance().setData(position);//通知播放列表播放位置变化
                    setup();
                    break;
                case Contsant.Action.PLAY_PAUSE_MUSIC:
                    LogTool.d("msg.arg1:" + msg.arg1);
                    if (msg.arg1 == 1) {
                        openAnim();
					} else if (msg.arg1 == 0) {
						closeAnim();
					}
					break;
				case Contsant.Action.MUSIC_STOP:
					flag = Contsant.PlayStatus.PAUSE;
					closeAnim();
					break;
				case Contsant.Msg.PLAY_LRC_SWITCH:
					int currentScreen = mFlingView.getCurrentScreen();
					LogTool.d("currentScreen:"+currentScreen);
					if(currentScreen == 0){
						mFlingView.setToScreen(1, true);
					}else if(currentScreen == 1){
						mFlingView.setToScreen(0, true);
					}
					ReadSDLrc();
					break;
                case Contsant.Msg.UPDATE_SEEK_BAR:
                    isSeeking = false;
                    break;
			}
		}
	};

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_play,container, false);
        initialize(view);
        return view;
    }

    /**
     * 判断某个服务是否正在运行的方法
     * @param mContext
     * @param serviceName 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        LogTool.d("isServiceWork:" + isWork);
        return isWork;
    }
    private void initialize (View view) {
		DataObservable.getInstance().addObserver(this);
        mIbBack = (ImageView)view.findViewById(R.id.ib_back);
        mMTvMusicName = (MovingTextView) view.findViewById(R.id.mtv_music_name);
		mTvFormat = (TextView) view.findViewById(R.id.tv_format);
		mTvSimapleRate = (TextView) view.findViewById(R.id.tv_simaple_rate);
		mTvBitRate = (TextView) view.findViewById(R.id.tv_bit_rate);
        mIbFore = (ImageButton) view.findViewById(R.id.ib_fore);
        mIbPlay = (ImageButton) view.findViewById(R.id.ib_play);
        mIbPre = (ImageButton) view.findViewById(R.id.ib_pre);
        mIbNext = (ImageButton) view.findViewById(R.id.ib_next);
        mIbLoopMode=(ImageButton)view.findViewById(R.id.ib_loop_mode);
		mIbBalance = (ImageButton)view.findViewById(R.id.ib_balance);
        mIbShare = (ImageButton) view.findViewById(R.id.ib_share);
        mIvMusicCd = (CircleImageView)view.findViewById(R.id.iv_music_cd);
        mIbVoice = (ImageView)view.findViewById(R.id.ib_voice);
		mTvCurrentTime = (TextView) view.findViewById(R.id.tv_current_time);
		mTvDurationTime = (TextView) view.findViewById(R.id.tv_duration_time);
        mCsbProgress = (CircularSeekBar) view.findViewById(R.id.csb_progress);
        mIvNoTouch1 = (ImageView)view.findViewById(R.id.iv_no_touch1);
        mIvNoTouch2 = (ImageView)view.findViewById(R.id.iv_no_touch2);
        mIvNoTouch3 = (ImageView)view.findViewById(R.id.iv_no_touch3);
        mIvNoTouch1.setOnTouchListener(this);
        mIvNoTouch2.setOnTouchListener(this);
        mIvNoTouch3.setOnTouchListener(this);
        mIbBack.setOnClickListener(this);
        mIbVoice.setOnClickListener(this);
        mIbPre.setOnClickListener(this);
        mIbFore.setOnClickListener(this);
        mIbPlay.setOnClickListener(this);
        mIbNext.setOnClickListener(this);
        mIbLoopMode.setOnClickListener(this);
		mIbBalance.setOnClickListener(this);
        mIbShare.setOnClickListener(this);
        mCsbProgress.setOnSeekBarChangeListener(this);
		mIvMusicCd.setOnTouchListener(this);
        mContext = getContext();
        // 获取系统音乐音量
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取系统音乐当前音量
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //初始化光盘旋转动画
        rotateAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        rotateAnim.setInterpolator(new LinearInterpolator());//重复播放不停顿
        rotateAnim.setFillAfter(true);//停在最后

		cubeLeftOutAnimation = new CubeLeftOutAnimation();
		cubeLeftOutAnimation.setDuration(500);
		cubeLeftOutAnimation.setFillAfter(true);

		cubeRightInAnimation = new CubeRightInAnimation();
		cubeRightInAnimation.setDuration(500);
		cubeRightInAnimation.setFillAfter(true);

		//////////////////////////////////////////////////////////
		cubeLeftOutBackAnimation = new CubeLeftOutBackAnimation();
		cubeLeftOutBackAnimation.setDuration(500);
		cubeLeftOutBackAnimation.setFillAfter(true);

        cubeRightInBackAnimation = new CubeRightInBackAnimation();
        cubeRightInBackAnimation.setDuration(500);
        cubeRightInBackAnimation.setFillAfter(true);
        /*if(flag == Contsant.PlayStatus.PLAY){
            openAnim();
        }else{
            closeAnim();
        }*/
        mFlingView = (FlingGalleryView)view.findViewById(R.id.fgv_player_main);
		mFlingView.setDefaultScreen(0);
		mTvlrcText = (TextView) view.findViewById(R.id.tv_player_lyric_info);
		mLrcView = (LrcView) view.findViewById(R.id.view_lrc);
		mLrcView.setListener(new ILrcView.LrcViewListener() {

			public void onLrcSeeked(int newPosition, LrcRow row) {
				LogTool.d("onLrcSeeked:" + row.time);
				seekbar_change(row.time);
			}
		});
		mIvBgLrc = (ImageView) view.findViewById(R.id.iv_bg_lrc);

        //异步检索歌词
        new Thread(){
            @Override
            public void run() {
                GetFiles(MusicUtil.getSDPath(),arrExtension, true);
            }
        }.start();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerMusicReceiver();
        if(!isServiceWork(getActivity(), "org.app.enjoy.music.service.MusicService")){
            Intent intent = new Intent();
            intent.setAction(Contsant.PlayAction.MUSIC_PLAY_SERVICE);
            intent.putExtra(Contsant.START_SERVICE_FIRST, 1);// 向服务传递数据
            intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
        }else{
            flag = Contsant.PlayStatus.PLAY;//认为是后台播放
            LogTool.d("onActivityCreated" + flag);
            if(flag == Contsant.PlayStatus.PLAY){
                openAnim();
            }else{
                closeAnim();
            }
        }

        /*if(!isServiceWork(getActivity(), "org.app.enjoy.music.service.LogService") && isCatchLog){
            Intent intent = new Intent(getActivity(), LogService.class);
            intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
        }*/
        initMusicData();
    }

    private static MusicPlayFragment mInstance;
    public static MusicPlayFragment getmInstance(){
        if(mInstance == null){
            mInstance = new MusicPlayFragment();
        }
        return mInstance;
    }

    private void initData (Bundle bundle) {
        mCsbProgress.setProgress(0);
        List<MusicData> datas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
        if(datas != null && datas.size() > 0){
            musicDatas = datas;
        }
        position = bundle.getInt(Contsant.POSITION_KEY);
        if (musicDatas != null) {
            randomIDs = new int[musicDatas.size()];
        }
	}

    private void openAnim () {
		mIbFore.setVisibility(View.VISIBLE);
		mIbPlay.setVisibility(View.GONE);
    }

    private void closeAnim () {
		mIbFore.setVisibility(View.GONE);
		mIbPlay.setVisibility(View.VISIBLE);
	}

	/**
	 * 在播放布局里先把播放按钮先删除并用background设置为透明。然后在代码添加按钮
	 */
	public void play() {
		if(musicDatas != null && musicDatas.size() > 0) {
			LogTool.i("play---startService");
			flag = Contsant.PlayStatus.PLAY;
			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
			bundle.putInt(Contsant.POSITION_KEY, position);
			intent.putExtras(bundle);
			intent.setAction(Contsant.PlayAction.MUSIC_PLAY_SERVICE);
			intent.putExtra("op", Contsant.PlayStatus.PLAY);// 向服务传递数据
			intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
		}
	}
	/**
	 * 暂停
	 */
	public void pause() {
		if(musicDatas != null && musicDatas.size() > 0){
			flag = Contsant.PlayStatus.PAUSE;
			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
			bundle.putInt(Contsant.POSITION_KEY, position);
			intent.putExtras(bundle);
			intent.setAction(Contsant.PlayAction.MUSIC_PLAY_SERVICE);
			intent.putExtra("op", Contsant.PlayStatus.PAUSE);
			intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
		}

    }
    /**
     * 上一首
     */
    private void lastOne() {
		LogTool.i("postion" + position);
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        stop();
//		if (musicDatas.size() == 1 || loop_flag == LOOP_ONE) {
        if (musicDatas.size() == 1) {//单曲循环允许手动切换到上一曲和下一曲
            position = position;
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            intent.putExtras(bundle);
			intent.setAction(Contsant.PlayAction.MUSIC_PLAY_SERVICE);
            intent.putExtra("length", 1);
            intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
            play();
            return;
        }
        if (random_flag == true) {
            if (randomNum < musicDatas.size() - 1) {
                randomIDs[randomNum] = position;
                position = findRandomSound(musicDatas.size());
                randomNum++;

            } else {
                randomNum = 0;
                for (int i = 0; i < musicDatas.size(); i++) {
                    randomIDs[i] = -1;
                }
                randomIDs[randomNum] = position;
                position = findRandomSound(musicDatas.size());
                randomNum++;
            }
        } else {
            if (position == 0) {
                position = musicDatas.size() - 1;
            } else if (position > 0) {
                position--;
            }
        }
        LogTool.i("postion" + position);
        Bundle bundle = new Bundle();
        bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
        bundle.putInt(Contsant.POSITION_KEY, position);
        DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变

        //add by victor
        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.MUSIC_PLAY_FRAG);
        SharePreferencesUtil.putString(mContext, Contsant.CURRENT_MUSIC_NAME, musicDatas.get(position).title);

        setup();
        play();

    }

    /**
     * 进度条改变事件
     */
    private void seekbar_change(long progress) {
        Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_PLAY_SERVICE);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        bundle.putLong(Contsant.SEEK_POSITION, mSeekPosition);
        intent.putExtras(bundle);
        intent.putExtra("op", Contsant.PlayStatus.PROGRESS_CHANGE);
        intent.putExtra("progress", progress);
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);

    }

    /**
     * 下一首
     */
    public  void nextOne() {
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        switch (loop_flag) {
            case Contsant.LoopMode.LOOP_ORDER://顺序播放
                //顺序播放运行手动切换下一曲，如果后台播放的最后一曲则停止播放
                position++;
                if (position == musicDatas.size()) {
                    position = 0;
                }
                break;
            case Contsant.LoopMode.LOOP_ONE://单曲循环
                //单曲循环播放模式如果手动点击下一曲则同全部循环处理，后台播放完毕当前歌曲自动播放当前歌曲
                position++;
                if (position == musicDatas.size()) {
                    position = 0;
                }
                break;
            case Contsant.LoopMode.LOOP_ALL://全部循环
                position++;
                if (position == musicDatas.size()) {
                    position = 0;
                }
                break;
            case Contsant.LoopMode.LOOP_RANDOM://随机播放
                if (randomNum < musicDatas.size() - 1) {
                    randomIDs[randomNum] = position;
                    position = findRandomSound(musicDatas.size());
                    randomNum++;

                } else {
                    randomNum = 0;
                    for (int i = 0; i < musicDatas.size(); i++) {
                        randomIDs[i] = -1;
                    }
                    randomIDs[randomNum] = position;
                    position = findRandomSound(musicDatas.size());
                    randomNum++;
                }
                break;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
        bundle.putInt(Contsant.POSITION_KEY, position);
        DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变

        //add by victor
        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.MUSIC_PLAY_FRAG);
        SharePreferencesUtil.putString(mContext, Contsant.CURRENT_MUSIC_NAME, musicDatas.get(position).title);

        setup();
        play();

	}
	/**找到随机位置**/
	public static int findRandomSound(int end) {
		int ret = -1;
		ret = (int) (Math.random() * end);
		while (havePlayed(ret, end)) {
			ret = (int) (Math.random() * end);
		}
		return ret;
	}
	/**是否在播放**/
	public static boolean havePlayed(int position, int num) {
		boolean ret = false;

		for (int i = 0; i < num; i++) {
			if (position == randomIDs[i]) {
				ret = true;
				break;
			}
		}

		return ret;
	}

    /**
     * 停止播放音乐
     */
    private void stop() {
        Intent isplay = new Intent("notifi.update");
        getActivity().sendBroadcast(isplay);// 发起后台支持
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.putExtras(bundle);
		intent.setAction(Contsant.PlayAction.MUSIC_PLAY_SERVICE);
        intent.putExtra("op", Contsant.PlayStatus.STOP);
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(getActivity());
        ReadSDLrc();
        isSeeking = false;
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }


    @Override
    public void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
        getActivity().unregisterReceiver(musicReceiver);
        if(mTask != null){
            mTask.cancel();
            mTask = null;
        }
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
    }

    /**
     * 初始化
     */
    private void setup() {
		registerMusicReceiver();
		ReadSDLrc();
    }

    public void showAudioInfo (){
        mMTvMusicName.setText(mMusicName);
        mTvFormat.setText(mMusicFormat);
        mTvSimapleRate.setText(mSimpleRate);
        mTvBitRate.setText(mBitRate);
	}
    /**
     * 初始化注册广播
     */
    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contsant.PlayAction.MUSIC_CURRENT);
        filter.addAction(Contsant.PlayAction.MUSIC_DURATION);
        filter.addAction(Contsant.PlayAction.MUSIC_NEXT);
        filter.addAction(Contsant.PlayAction.MUSIC_UPDATE);
        filter.addAction(Contsant.PlayAction.MUSIC_STOP);//add by victor
		filter.addAction(Contsant.PlayAction.PLAY_PAUSE_NEXT);
        filter.addAction(Contsant.PlayAction.MUSIC_LIST);
        filter.addAction("notifi.update");
        if(getActivity() != null && musicReceiver != null){
            getActivity().registerReceiver(musicReceiver, filter);
        }
    }
    /**在后台MusicService里使用handler消息机制，不停的向前台发送广播，广播里面的数据是当前mp播放的时间点，
     * 前台接收到广播后获得播放时间点来更新进度条,暂且先这样。但是一些人说虽然这样能实现。但是还是觉得开个子线程不错**/
    protected BroadcastReceiver musicReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
			if (action.equals(Contsant.PlayAction.MUSIC_PREPARED)) {
				LogTool.d("MusicService.MUSIC_PREPARED");
			}else if (action.equals(Contsant.PlayAction.MUSIC_CURRENT)) {
                getInfoFromBroadcast(intent);
				currentTime = intent.getExtras().getLong("currentTime");// 获得当前播放位置
                LogTool.d("currentTime:" + currentTime);
                mHandler.sendEmptyMessage(Contsant.Action.CURRENT_TIME_MUSIC);
            } else if (action.equals(Contsant.PlayAction.MUSIC_DURATION)) {
                getInfoFromBroadcast(intent);
                mHandler.sendEmptyMessage(Contsant.Action.DURATION_MUSIC);
                if(mTimer == null){
                    LogTool.d("LrcTask == null");
                    mTimer = new Timer();
                    mTask = new LrcTask();
                    mTimer.schedule(mTask, 0, mPalyTimerDuration);
                }
                if(musicDatas == null || musicDatas.size() == 0){

                    if(mMusicName == null || TextUtils.isEmpty(mMusicName)){//没有获取上次播放记录时，自动播放第一首
                        position = 0;
                        musicDatas = mMusicDatasNull;
                        play();
                    }
                }
            } else if (action.equals(Contsant.PlayAction.MUSIC_NEXT)) {
                mHandler.sendEmptyMessage(Contsant.Action.NEXTONE_MUSIC);
            } else if (action.equals(Contsant.PlayAction.MUSIC_UPDATE)) {
                position = intent.getExtras().getInt("position");
                LogTool.d("PlayAction.MUSIC_UPDATE  position:"+position);
                mHandler.sendEmptyMessage(Contsant.Action.UPDATE_MUSIC);
			}else if(action.equals(Contsant.PlayAction.PLAY_PAUSE_NEXT)){
                int isPlaying = intent.getExtras().getInt("isPlaying");
                LogTool.d("isplaying:" + isPlaying);
                Message msg = mHandler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC,isPlaying, 0);
                msg.sendToTarget();
            } else if (action.equals(Contsant.PlayAction.MUSIC_STOP)) {//add by victor
                mHandler.sendEmptyMessage(Contsant.Action.MUSIC_STOP);
            }else if(action.equals(Contsant.PlayAction.MUSIC_LIST)){
                position = intent.getExtras().getInt("position");
                Bundle bundle = intent.getExtras();
                LogTool.d("bundle != null"+(bundle != null) + position);
                if(bundle != null){
                    List<MusicData> datas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
                    if(datas != null && datas.size() > 0){
                        musicDatas = datas;
                        position = bundle.getInt(Contsant.POSITION_KEY);
                    }
                    LogTool.d("bundle != null position:"+ position);
                    if (musicDatas != null) {
                        randomIDs = new int[musicDatas.size()];
                    }
                }
            }
        }
    };

    private void initMusicData () {
        List<MusicData> musicList = MusicUtil.getAllSongs(getActivity());
        mMusicDatasNull.addAll(musicList);
        //异步检索其他音频文件
        new Thread(){
            @Override
            public void run() {
                MusicUtil.GetMusicFiles(MusicUtil.getSDPath(), true , mMusicDatasNull);
            }
        }.start();
        LogTool.d("initMusicData" + mMusicDatasNull.size());
    }

    private void getInfoFromBroadcast(Intent intent){
        if(intent == null){
            return;
        }
        mMa_data = intent.getExtras().getInt(Contsant.CURRENT_FRAG);
        position = intent.getExtras().getInt(Contsant.MUSIC_INFO_POSTION);
        duration = intent.getExtras().getLong(Contsant.MUSIC_INFO_DURATION);
        mMusicName = intent.getStringExtra(Contsant.MUSIC_INFO_NAME);
        mMusicFormat = intent.getStringExtra(Contsant.MUSIC_INFO_FORMAT);
        mSimpleRate = intent.getStringExtra(Contsant.MUSIC_INFO_SAMPLERATE);
        mBitRate = intent.getStringExtra(Contsant.MUSIC_INFO_BITRATE);
        Bundle bundle = intent.getExtras();
        if(bundle != null){
            if(musicDatas == null){
                musicDatas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
            }
        }
//        LogTool.d("info:" + position +"duration:" +duration);
    }

    /*
     * 毫秒转化时分秒毫秒
     */
    public static String formatTime(Long ms) {
        Integer ss = 1000;
        Integer mi = ss * 60;
        Integer hh = mi * 60;
        Integer dd = hh * 24;

        Long day = ms / dd;
        Long hour = (ms - day * dd) / hh;
        Long minute = (ms - day * dd - hour * hh) / mi;
        Long second = (ms - day * dd - hour * hh - minute * mi) / ss;
        if(hour > 0) {
            return String.format("%02d:%02d:%02d",hour, minute, second);
        }else{
            return String.format("%02d:%02d", minute, second);
        }
    }

    @Override
    public void onClick(View view) {
        if(musicDatas == null || musicDatas.size() == 0){
            musicDatas = mMusicDatasNull;
            for(int j = 0; j < mMusicDatasNull.size(); j++){
                MusicData music = mMusicDatasNull.get(j);
                if(music != null && mMusicName.equalsIgnoreCase(music.getTitle())) {
                    position = j;
                    musicDatas = mMusicDatasNull;
                }
            }
        }
        int i = view.getId();
        if (i == R.id.ib_back) {
            startActivity(new Intent(mContext, MusicActivity.class));
		} else if (i == R.id.ib_voice) {
                if(popupWindow != null && popupWindow.isShowing()){
                    popupWindow.dismiss();
                }else{
                    popupWindow = new XfDialog.Builder(getActivity()).setTitle(R.string.info).setMessage(R.string.dialog_messenge).setPositiveButton(R.string.confrim, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setAction(Contsant.PlayAction.MUSIC_STOP_SERVICE);
                            getActivity().sendBroadcast(intent);
                            popupWindow.dismiss();
                        }
                    }).setNeutralButton(R.string.cancel, null).show();
                }
		} else if (i == R.id.ib_pre) {
			if (musicDatas != null && musicDatas.size() > 0) {
				lastOne();
			}

		} else if (i == R.id.ib_play) {
			if (musicDatas != null && musicDatas.size() > 0) {
				if (flag == Contsant.PlayStatus.PLAY) {
					pause();
				} else if (flag == Contsant.PlayStatus.PAUSE) {
					play();
				}
			}else if(mMusicDatasNull != null && mMusicDatasNull.size() > 0){
                LogTool.d("mMusicDatasNull != null && mMusicDatasNull.size() > 0");
                /*for(int j = 0; j < mMusicDatasNull.size(); j++){
                    MusicData music = mMusicDatasNull.get(j);
                    if(music != null && mMusicName.equalsIgnoreCase(music.getTitle())) {
                        position = j;
                        musicDatas = mMusicDatasNull;
                        play();
                        return;
                    }
                }*/
                Toast.makeText(getActivity(), R.string.play_init_data,Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getActivity(), R.string.play_init_data,Toast.LENGTH_SHORT).show();
            }

		} else if (i == R.id.ib_fore) {
			if (musicDatas != null && musicDatas.size() > 0) {
				if (flag == Contsant.PlayStatus.PLAY) {
					pause();
				} else if (flag == Contsant.PlayStatus.PAUSE) {
					play();
				}
			}

		} else if (i == R.id.ib_next) {
			if (musicDatas != null && musicDatas.size() > 0) {
				nextOne();
			}

		} else if (i == R.id.ib_loop_mode) {
			loop_flag++;
			if (loop_flag > 4) {
				loop_flag = 2;//去掉顺序播放功能
			}
			setPlayModeBg();

		} else if (i == R.id.ib_share) {
			Intent intentshare = new Intent(Intent.ACTION_SEND);
			intentshare
					.setType("text/plain")
					.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.play_share))
					.putExtra(Intent.EXTRA_TEXT,
							getResources().getString(R.string.play_share_content) + mMusicName);
			Intent.createChooser(intentshare, getResources().getString(R.string.play_share));
			startActivity(intentshare);

		} else if (i == R.id.ib_balance) {
			mHandler.sendEmptyMessage(Contsant.Msg.PLAY_LRC_SWITCH);
		}
	}

	private long mPositionSeek = 0;
	@Override
	public void onProgressChanged(CircularSeekBar circularSeekBar, long progress, boolean fromUser) {
		if (fromUser) {
			mPositionSeek = progress;
            isSeeking = true;
		}/*else if (circularSeekBar.getId() == R.id.sb_player_voice) {
			// 设置音量
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)progress, 0);
		}*/
	}

	@Override
	public void onStopTrackingTouch(CircularSeekBar seekBar) {
		if(musicDatas != null && musicDatas.size() > 0){
			seekbar_change(mPositionSeek);
            mHandler.sendEmptyMessageDelayed(Contsant.Msg.UPDATE_SEEK_BAR, DELAY_CHANGE_PROGRESS);
			play();
		}
	}

	@Override
	public void onStartTrackingTouch(CircularSeekBar seekBar) {
		if(musicDatas != null && musicDatas.size() > 0){
			mPositionSeek = 0;
			pause();
		}
	}

	// Press the back button in mobile phone
	/*@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(0, R.anim.base_slide_right_out);
	}*/
    /**
     * 设置播放模式按钮背景
     * add by victor
     */
    private void setPlayModeBg() {
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        switch (loop_flag) {
            case Contsant.LoopMode.LOOP_ORDER:
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_sequence);
                break;
            case Contsant.LoopMode.LOOP_ONE:
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_circleone);
                break;
            case Contsant.LoopMode.LOOP_ALL:
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_circlelist);
                break;
            case Contsant.LoopMode.LOOP_RANDOM:
				if(musicDatas != null && musicDatas.size() >0){
					for (int i = 0; i < musicDatas.size(); i++) {
						randomIDs[i] = -1;
					}
				}
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_random);
                break;
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            int action = bundle.getInt(Contsant.ACTION_KEY);
            if (action == Contsant.Action.PLAY_MUSIC) {
                initData(bundle);
                mHandler.sendEmptyMessage(Contsant.Msg.PLAY_MUSIC);
            }
        }
    }

@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		LogTool.i("MotionEvent" + motionEvent.getAction());
		int i = view.getId();
		if (i == R.id.iv_music_cd) {
			if (MotionEvent.ACTION_DOWN == motionEvent.getAction()) {
				pause();
			} else if (MotionEvent.ACTION_UP == motionEvent.getAction()) {
				play();
			}
		}else if( i == R.id.iv_no_touch1 ||  i == R.id.iv_no_touch2 ||  i == R.id.iv_no_touch3){
            return true;
        }
		return true;
	}

	/**
	 * 为音乐读取歌词，歌词一行一行方式输出，但是卡拉OK完全做不出来着。
	 */
	private void ReadSDLrc() {
		if(musicDatas == null || musicDatas.size() == 0){
			return;
		}
        long albumid = -1;
        String cursorName = null;
        Cursor cursor = null;
		try{
            /**我们现在的歌词就是要String数组的第4个参数-显示文件名字**/
            cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] { MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.ALBUM_ID }, "_id=?",new String[] { musicDatas.get(position).getId() + "" }, null);
            cursor.moveToFirst();// 将游标移至第一位
            if(cursor.getCount() > 0){
                /**切换播放时候专辑图片出现透明效果**/
                Animation albumanim = AnimationUtils.loadAnimation(getActivity(), R.anim.album_replace);
                /**开始播放动画效果**/
//			mIvMusicCd.startAnimation(albumanim);
                albumid = cursor.getInt(5);
                /**游标定位到DISPLAY_NAME**/
                cursorName = cursor.getString(4);

            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }

        if(musicDatas != null && musicDatas.size() > position){
            String albumartPath = AlbumImgUtil.getAlbumartPath(musicDatas.get(position).getId(), albumid, getActivity());
            Glide.with(mContext)//加载显示圆形图片
                    .load(albumartPath)
                    .asBitmap()
                    .placeholder(R.drawable.icon_music_cd)
                    .centerCrop()
                    .into(new BitmapImageViewTarget(mIvMusicCd) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            mIvMusicCd.setImageDrawable(circularBitmapDrawable);
                        }
                    });
            Glide.with(mContext)//加载显示圆形图片
                    .load(albumartPath)
                    .asBitmap()
                    .placeholder(R.drawable.icon_music_cd)
                    .centerCrop()
                    .into(mIvBgLrc);
        }

        if(cursorName != null && !TextUtils.isEmpty(cursorName)){
            String lrcName = cursorName.substring(0, cursorName.indexOf(".")) + ".lrc";

            String lrc = getLrcFromPath(lrcName);
            LogTool.d("lrc:" + lrc);

            ILrcBuilder builder = new DefaultLrcBuilder();
            List<LrcRow> rows = builder.getLrcRows(lrc);
            mLrcView.setLrc(rows);
        }
    }

	private void updateLRC(long position){
		Iterator<Integer> iterator = lrc_map.keySet().iterator();
		while (iterator.hasNext()) {
			Object o = iterator.next();
			LRCbean val = lrc_map.get(o);
			if (val != null) {
				if (position > val.getBeginTime()&& position < val.getBeginTime()+ val.getLineTime()) {
					mTvlrcText.setText(val.getLrcBody());
					break;
				}
			}else{
				LogTool.e("val == null");
			}
		}
	}

	public String getLrcFromPath(String pathName){
		LogTool.i(pathName);
		String path = getLrcPath(mLrcPathlist,pathName);
		LogTool.i(path);
		File file = new File(path);
		/**如果没有歌词，则用没有歌词显示**/
		if (!file.exists()) {
			LogTool.e("!file.exists()");
			Animation lrcanim=AnimationUtils.loadAnimation(mContext, R.anim.album_replace);
			mTvlrcText.setText(R.string.no_lrc_messenge);
			mTvlrcText.startAnimation(lrcanim);
			return "";
		}
		FileInputStream stream = null;
		try {
			stream = new FileInputStream( file );
			BufferedReader bufReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			String line="";
			String Result="";
			while((line = bufReader.readLine()) != null){
				if(line.trim().equals(""))
					continue;
				Result += line + "\r\n";
			}
			return Result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public void stopLrcPlay(){
		if(mTimer != null){
			mTimer.cancel();
			mTimer = null;
		}
	}

    class LrcTask extends TimerTask {

		long beginTime = -1;

		@Override
		public void run() {
			if(beginTime == -1) {
				beginTime = System.currentTimeMillis();
			}
			getActivity().runOnUiThread(new Runnable() {

                public void run() {
                    mLrcView.seekLrcToTime(currentTime);
                }
            });
		}
	};

	private void showPlayingTime(long currentTime, long duration){
        if(!isSeeking) {
            mCsbProgress.setProgress((int) currentTime);// 设置进度条
        }
        mCsbProgress.setMax((int) duration);
        mTvCurrentTime.setText(formatTime(currentTime));
        mTvDurationTime.setText(formatTime( duration));
	}

	/**
	 * 遍历文件夹，搜索指定扩展名的文件
	 * */
	private String[] arrExtension = new String[]{"lrc"};
	public void GetFiles(String Path, String[] arrExtension, boolean IsIterative)  //搜索目录，扩展名，是否进入子文件夹
	{
		if(Path == null || TextUtils.isEmpty(Path)){
			return;
		}
		File[] files = new File(Path).listFiles();

		if(files != null && files.length > 0){
            for (int i = 0; i < files.length; i++)
            {
                File f = files[i];
                if (f.isFile())
                {
                    String[] arrFile = f.getPath().split("\\.");
                    if(arrFile != null && arrFile.length >0){
                        int length = arrFile.length;
                        if(arrFile[length -1] != null){
                            for(String str : arrExtension){
                                if(arrFile[length -1].equalsIgnoreCase(str)){
                                    LogTool.d(f.getPath());
                                    mLrcPathlist.add(f.getPath());
                                    break;
                                }
                            }
                        }
                    }
                    if (!IsIterative)
                        break;
                }
                else if (f.isDirectory() && f.getPath().indexOf("/.") == -1)  //忽略点文件（隐藏文件/文件夹）
                    GetFiles(f.getPath(), arrExtension, IsIterative);
            }
        }
	}



	private String getLrcPath(List<String> list, String pathName){
		String path = "";
		for(String strPath : list){
			if(strPath != null && strPath.endsWith(pathName)){
				path = strPath;
			}
		}
		return path;
	}
}
