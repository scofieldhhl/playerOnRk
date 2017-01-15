package org.app.enjoy.music.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.db.DBHelper;
import org.app.enjoy.music.frag.MusicPlayFragment;
import org.app.enjoy.music.mode.DataObservable;
import wseemann.media.FFmpegMediaPlayer;
import wseemann.media.TimedText;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.musicplayer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaFormat;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkMediaFormat;
/**
 * 所有播放操作都交给服务,要服务就是为了实现后台播放,如果不用服务，那么一个界面关闭后，音乐也随着消失了。 但是用了服务这种情况不存在了。我们要做的永久
 * 在后台播放。直到用户把服务秒杀了。
 */
public class MusicService extends Service implements Observer {
	public final String PKG_NAME = "org.app.enjoy.musicplayer";
	public final int DELAY_CLOSE_TOAST = 500;
	private static final String TAG = MusicService.class.getName();
	/** 发送给服务一些Action */
	private IMediaPlayer mp = null;
	private Uri uri = null;
	private int id = 10000;
	private long currentTime;// 播放时间
	private long duration;// 总时间
	private DBHelper dbHelper = null;// 数据库对象
	public static int flag;// 标识
	private int position;// 位置
	public static Notification notification;// 通知栏显示当前播放音乐
	public static NotificationManager nm;
	private List<MusicData> musicDatas;
	private String mPath = "";
	private long mSeekPosition = 0L;
	private boolean isSetDataSource = false;//用来区分cue音乐 切换是否需要跳转
	private int prePosition = 0;
	private String mSampleRate;
	private String mBitRate;
	private Context mContext;
	private String mMusicName, mMusicFormat;
	private int mMa_data;//当前播放列表
	private PowerManager.WakeLock mWakeLock;
	private Toast mToast;
	private boolean isHeadsetIn = true;
	//
	private FFmpegMediaPlayer fmp = null;
	private boolean isPlayISO = false;
	private AudioManager mAudioManager;
	private int mMaxVolume;

	public final int TIME_ADJUST_VOL = 10*1000;
	private boolean isAdjustVol = false;
	private int mVol;
	public final int DEFAULT_VOL = 25;
	public final int DEFAULT_VOL_RK = 180;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final Intent intent = new Intent();
			switch (msg.what) {
				case Contsant.PlayStatus.STATE_PREPARED:
					if(isPlayISO){
						currentTime =  mp.getCurrentPosition();
					}else {
						currentTime =  fmp.getCurrentPosition();
					}
					intent.setAction(Contsant.PlayAction.MUSIC_PREPARED);
					intent.putExtra("currentTime", currentTime);
					sendBroadcast(intent);
					handler.sendEmptyMessage(Contsant.PlayStatus.STATE_INFO);
					break;
				case Contsant.PlayStatus.STATE_INFO:
					if(isPlayISO){
						if(mp != null){
							intent.setAction(Contsant.PlayAction.MUSIC_CURRENT);
							currentTime =  mp.getCurrentPosition();
							duration = mp.getDuration();
							intent.putExtra("currentTime", currentTime);
							broadCastMusicInfo(intent);
						}
					}else {
						if(fmp != null){
							intent.setAction(Contsant.PlayAction.MUSIC_CURRENT);
							currentTime =  fmp.getCurrentPosition();
							duration = fmp.getDuration();
							LogTool.d("currentTime:" + currentTime);
							intent.putExtra("currentTime", currentTime);
							broadCastMusicInfo(intent);
						}
					}
					//切换曲目淡入淡出
					if(currentTime > duration - TIME_ADJUST_VOL && !isAdjustVol && duration > 0){
						handler.sendEmptyMessage(Contsant.Msg.LOWER_VOL);
					}

					if(handler != null){
						handler.sendEmptyMessageDelayed(Contsant.PlayStatus.STATE_INFO, 500);
					}
					if(mToast != null){
						mToast.cancel();
						mToast = null;
					}
					break;
				case Contsant.Action.PLAY_PAUSE_MUSIC:
					LogTool.i("PLAY_PAUSE_MUSIC" + msg.arg1);
					intent.setAction(Contsant.PlayAction.PLAY_PAUSE_NEXT);
					intent.putExtra("isPlaying", msg.arg1);
					intent.setPackage(PKG_NAME);
					sendBroadcast(intent);
					break;
				case Contsant.Msg.DELAY_CANCLE_TOAST:
					if(mToast != null){
						mToast.cancel();
						mToast = null;
					}
					break;

				case Contsant.Msg.ADD_VOL:
					if(isPlayISO){
						LogTool.d("AddVol:" + mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC ));
						if(mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC ) > 0 && !isAdjustVol) {
							isAdjustVol = false;
							handler.removeMessages(Contsant.Msg.ADD_VOL);
							handler.removeMessages(Contsant.Msg.LOWER_VOL);
						}else {
							mAudioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0);
							mVol = mVol == 0 ? DEFAULT_VOL : mVol;
							LogTool.d("AddVol  mVol:" + mVol);
							int currentVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
							if (currentVol == mVol || currentVol == mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
								isAdjustVol = false;
								handler.removeMessages(Contsant.Msg.ADD_VOL);
								handler.removeMessages(Contsant.Msg.LOWER_VOL);
							} else {
								isAdjustVol = true;
								handler.sendEmptyMessageDelayed(Contsant.Msg.ADD_VOL, TIME_ADJUST_VOL / mVol);
							}

						}
					}else {
						//TODO 增加ＲＫ播放器音量
					}
					break;
				case Contsant.Msg.LOWER_VOL:
					if(isPlayISO){
						mAudioManager.adjustVolume(AudioManager.ADJUST_LOWER,  0);
						LogTool.d("vol:" + mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC ));
						if(mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC ) == 0){
							isAdjustVol = false;
							handler.removeMessages(Contsant.Msg.ADD_VOL);
							handler.removeMessages(Contsant.Msg.LOWER_VOL);
						}else {
							isAdjustVol = true;
							mVol = mVol == 0 ? DEFAULT_VOL : mVol;
							handler.sendEmptyMessageDelayed(Contsant.Msg.LOWER_VOL, (TIME_ADJUST_VOL - 2*1000)/ (mVol + 20));
						}
					}else {
						//TODO 降低RKplayer音量
					}
					break;
			}
		}
	};

	@Override
	public void onCreate() {
		LogTool.i("onCreate");
		super.onCreate();
		mContext = this;

		mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, currVolume, 0); //tempVolume:音量绝对值
		LogTool.d("Volume Max:" + mMaxVolume);
		LogTool.d("system Volume max:" + mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM));

//		initIJKPlayer();
		//----------------------
//		initFMPlayer();
		//----------------------
		ShowNotifcation();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.ANSWER");
		filter.addAction("android.intent.action.ACTION_SHUTDOWN");
		filter.addAction(Contsant.PlayAction.MUSIC_STOP_SERVICE);
		registerReceiver(PhoneListener, filter);

		IntentFilter filter1 = new IntentFilter();
		filter1.addAction("com.app.playmusic");
		filter1.addAction("com.app.nextone");
		filter1.addAction("com.app.lastone");
		filter1.addAction("com.app.startapp");
		registerReceiver(appWidgetReceiver, filter1);
		registerHeadsetPlugReceiver();
		DataObservable.getInstance().addObserver(this);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,getString(R.string.title_activity_play));
		mWakeLock.acquire();//保持cup不休眠

		//注册声音监听
		StreamMusicObserver mSettingsContentObserver = new StreamMusicObserver(mContext, new Handler());
		this.getApplicationContext().getContentResolver().registerContentObserver(
				android.provider.Settings.System.CONTENT_URI, true,
				mSettingsContentObserver );

		registerVolumeReceiver();
	}

	private void initIJKPlayer(){
		try {
			if (mp != null) {
				mp.reset();
				mp.release();
				mp = null;
			}
			IjkMediaPlayer ijkMediaPlayer = null;
			ijkMediaPlayer = new IjkMediaPlayer();
			mp = ijkMediaPlayer;
			mp.setOnCompletionListener(mCompletionListener);
			mp.setOnInfoListener(mInfoListener);
			mp.setScreenOnWhilePlaying(true);
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setWakeMode(MusicService.this, PowerManager.PARTIAL_WAKE_LOCK);

//			ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
		} catch (Exception ex) {
			Log.e(TAG, "Unable to open content: " + uri, ex);
			return;
		}
	}

	IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
		public void onPrepared(IMediaPlayer mp) {
			handler.sendEmptyMessage(Contsant.Msg.ADD_VOL);
			LogTool.i("onPrepared");
			handler.sendEmptyMessage(Contsant.PlayStatus.STATE_PREPARED);
			if(mSeekPosition != 0){
				LogTool.i("mSeekPosition" + mSeekPosition);
				mp.seekTo(mSeekPosition);
			}else if(musicDatas.get(position).seekPostion != 0 && isSetDataSource){
				LogTool.i("musicDatas mSeekPosition" + musicDatas.get(position).seekPostion);
				mp.seekTo(musicDatas.get(position).seekPostion);
			}
			initAudioInfo();
			saveLastPlayInfo();
		}
	};

	IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {

		@Override
		public boolean onInfo(IMediaPlayer mp, int what, int extra) {
			LogTool.d("getCurrentPosition:" + mp.getCurrentPosition());
			handler.sendEmptyMessage(Contsant.PlayStatus.STATE_INFO);
			return false;
		}
	};

	private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
			new IMediaPlayer.OnBufferingUpdateListener() {
				public void onBufferingUpdate(IMediaPlayer mp, int percent) {
//					mCurrentBufferPercentage = percent;
				}
			};
	/** 初始化1*/
	private void initAudioInfo() {
		mBitRate = ""; mSampleRate = "";
		showMediaInfo();
		if(musicDatas != null && musicDatas.size() > position){
			mMusicName = musicDatas.get(position).title;
			mMusicFormat = musicDatas.get(position).getPath().substring(musicDatas.get(position).getPath().length() - 3).toUpperCase();
			if(musicDatas.get(position).getPath().startsWith(Contsant.DSD_ISO_HEADER)){
				mMusicFormat = Contsant.DSD_ISO.toUpperCase();
			}
		}
		mMa_data = SharePreferencesUtil.getInt(mContext, Contsant.CURRENT_FRAG);
		if(isPlayISO){
			duration = mp.getDuration();
		}else {
			duration = fmp.getDuration();
		}
		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_DURATION);
		broadCastMusicInfo(intent);
	}

	private void broadCastMusicInfo(Intent intent){
		if(intent == null){
			intent = new Intent();
		}
		intent.putExtra(Contsant.CURRENT_FRAG, mMa_data);
		intent.putExtra(Contsant.MUSIC_INFO_POSTION, position);
		intent.putExtra(Contsant.MUSIC_INFO_NAME, mMusicName);
		intent.putExtra(Contsant.MUSIC_INFO_FORMAT, mMusicFormat);
		intent.putExtra(Contsant.MUSIC_INFO_SAMPLERATE, mSampleRate);
		intent.putExtra(Contsant.MUSIC_INFO_BITRATE, mBitRate);
		intent.putExtra(Contsant.MUSIC_INFO_DURATION, duration);
		intent.setPackage(PKG_NAME);
		sendBroadcast(intent, "-1");
	}
	private IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {
		public void onCompletion(IMediaPlayer mp) {
			LogTool.d("onCompletion");
			nextOne();
		}
	};

	/**
	 * 当开始播放时，通知栏显示当前播放信息
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void ShowNotifcation() {
		/*nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);//获取通知栏系统服务对象
		notification = new Notification();// 实例化通知栏
		notification.icon = R.drawable.music;// 为通知栏增加图标
		notification.defaults = Notification.DEFAULT_LIGHTS;// 默认灯
		notification.flags |= Notification.FLAG_AUTO_CANCEL;// 永远驻留
		notification.when = System.currentTimeMillis();// 获得系统时间
		notification.tickerText = musicDatas.get(position).title;//在通知栏显示有关的信息
		notification.tickerText = musicDatas.get(position).artist;
		Intent intent2 = new Intent(getApplicationContext(),PlayMusicActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
		bundle.putInt(Contsant.POSITION_KEY, position);
		intent2.putExtras(bundle);
		intent2.putExtra("position", position);

		String artist = musicDatas.get(position).artist;
		if (artist.equals("<unknown>")) {
			artist = "未知艺术家";
		}
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2,PendingIntent.FLAG_UPDATE_CURRENT);
//		notification.setLatestEventInfo(getApplicationContext(), _title, _artist,contentIntent);
		notification = new Notification.Builder(MusicService.this)
				.setAutoCancel(true)
				.setContentTitle(musicDatas.get(position).title)
				.setContentText(musicDatas.get(position).artist)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.music)
				.setWhen(System.currentTimeMillis())
				.build();
		nm.notify(0, notification);*/

	}

	@Override
	public void onDestroy() {
		mWakeLock.release();
		DataObservable.getInstance().deleteObserver(this);
		super.onDestroy();
		Log.e(TAG, "MusicService is onDestroy().....................");
		if(nm != null){
			nm.cancelAll();// 清除掉通知栏的信息
		}
		if(isPlayISO){
			if (mp != null) {
				mp.stop();// 停止播放
				mp = null;
			}
		}else {
			if (fmp != null) {
				fmp.stop();// 停止播放
				fmp = null;
			}
		}
		if (dbHelper != null) {
			dbHelper.close();// 关闭数据库
			dbHelper = null;
		}
		if (handler != null) {
			handler.removeMessages(1);//移除消息
			handler = null;
		}
		if(PhoneListener != null){
			this.unregisterReceiver(PhoneListener);
		}
		if(appWidgetReceiver != null){
			this.unregisterReceiver(appWidgetReceiver);
		}
		if(headsetPlugReceiver != null){
			unregisterReceiver(headsetPlugReceiver);
		}

		unRegisterVolume();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogTool.i("onStartCommand");
		if (intent == null)
			return 0;
		int startServiceFisrt = intent.getIntExtra(Contsant.START_SERVICE_FIRST, 0);
		//LogTool.d("startServiceFisrt:" + startServiceFisrt + "position:" + position);
		if(startServiceFisrt == 1){//首次启动拿上次播放记录
			mMa_data = SharePreferencesUtil.getInt(mContext, Contsant.CURRENT_FRAG);
			position = SharePreferencesUtil.getInt(mContext, Contsant.MUSIC_INFO_POSTION);
			mMusicName = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_NAME);
			mMusicFormat = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_FORMAT);
			mBitRate = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_BITRATE);
			mSampleRate = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_SAMPLERATE);
			duration = SharePreferencesUtil.getLong(mContext, Contsant.MUSIC_INFO_DURATION);
			Intent infoIntent = new Intent();
			infoIntent.setAction(Contsant.PlayAction.MUSIC_DURATION);
			broadCastMusicInfo(infoIntent);
			return 0;
		}
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			musicDatas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
			position = bundle.getInt(Contsant.POSITION_KEY);
			mSeekPosition = bundle.getLong(Contsant.SEEK_POSITION);
			LogTool.i("position"+position +"mSeekPosition"+ mSeekPosition);
//			mPath = bundle.getString(Contsant.POSITION_PATH);

			// 发送的长度
			int length = intent.getIntExtra("length", -1);
			if (musicDatas == null || position >= musicDatas.size() || position == -1) {
				return 0;
			}
			if (musicDatas.get(position).path != null) {
				LogTool.i("mPath" + mPath);
				LogTool.i(musicDatas.get(position).path);
				if (!mPath.equalsIgnoreCase(musicDatas.get(position).path) || (musicDatas.get(position).seekPostion != 0 && position != prePosition)) {
					mPath = musicDatas.get(position).path;
					LogTool.d("DEBUG:" + mPath);
					playPath(mPath);
				}else if (length == 1) {
					LogTool.d("length == 1 :" + mPath);
					mPath = musicDatas.get(position).path;
					playPath(mPath);
				}
			}

			if (position != -1) {
				Intent intent1 = new Intent();
				intent1.setAction(Contsant.PlayAction.MUSIC_LIST);
				intent1.putExtra("position", position);
				sendBroadcast(intent1, "-1");
				Intent intent2 = new Intent("com.app.musictitle");
				intent2.putExtra("title", musicDatas.get(position).title);
				sendBroadcast(intent2, "-1");
				Intent playIntent = new Intent(Contsant.PlayAction.MUSIC_PLAY);
				sendBroadcast(playIntent, "-1");
			}
			/**
			 * 初始化数据
			 */
			int op = intent.getIntExtra("op", -1);
			LogTool.i("op" + op);
			if (op != -1) {
				switch (op) {
					case Contsant.PlayStatus.PLAY:// 播放
						if(isPlayISO){
							if (!mp.isPlaying()) {
								play();
							}
						}else {
							if(!fmp.isPlaying()){
								play();
							}
						}
						break;
					case Contsant.PlayStatus.PAUSE:// 暂停
						isSetDataSource = false;
						if(isPlayISO){
							if (mp.isPlaying()) {
								pause();
							}
						}else {
							if (fmp.isPlaying()) {
								pause();
							}
						}
						break;
					case Contsant.PlayStatus.STOP:// 停止
						isSetDataSource = false;
						stop();
						break;
					case Contsant.PlayStatus.PROGRESS_CHANGE:// 进度条改变
						isSetDataSource = false;
						currentTime = intent.getExtras().getLong("progress");
						if(isPlayISO){
							mp.seekTo(currentTime);
						}else {
							fmp.seekTo((int)currentTime);
						}
						break;
				}
			}
			ShowNotifcation();
		} else {
			LogTool.e("------------------------bundle == null----------------------------------");
		}

		return super.onStartCommand(intent, flags, startId);
	}

	private void playPath(String path){
		LogTool.d("playPath:" + path);
		if(path == null || TextUtils.isEmpty(path)){
			Toast.makeText(mContext, R.string.file_no_exist, Toast.LENGTH_SHORT).show();
			return;
		}
		if(path.startsWith(Contsant.DSD_ISO_HEADER)){//播放ISO前需要先调用
			String isoPath = path.replace(Contsant.DSD_ISO_HEADER, "");
			String strIndex = path.substring(path.length() - 3,path.length());
			LogTool.d("strIndex:" + strIndex);
			try{
				int songIndex = Integer.parseInt(strIndex.trim()) - 1;
				LogTool.d("strIndex:" + songIndex);
				isoPath = isoPath.substring(0,isoPath.length() - 4);
				LogTool.d("isoPath:" + isoPath);
				int flag = IjkMediaPlayer.native_opensacdisofile(isoPath, 0, songIndex);
				String pathPlay = "SACD://%s:%d";
				path = String.format(pathPlay, isoPath, songIndex);
				LogTool.d("native_opensacdisofile:" + flag + "  ISO Path:" + path);
				try {
					if(fmp != null){
						if (fmp.isPlaying()) {
							fmp.stop();
						}
						fmp.reset();
						fmp.release();
						fmp = null;
					}else {
						LogTool.i("fmp == null!!");
					}
					initIJKPlayer();
					mp.setDataSource(path);
					isSetDataSource = true;
					isPlayISO = true;
					prePosition = position;
					setup();
					LogTool.i("ISO setDataSource:" + path);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}else if(path.endsWith("dsf") || path.endsWith("dff") ||
				path.endsWith("dsd") || path.endsWith("dst")){
			try {
				if(fmp != null){
					if (fmp.isPlaying()) {
						fmp.stop();
					}
					fmp.reset();
					fmp.release();
					fmp = null;
				}else {
					LogTool.i("fmp == null!!");
				}
				initIJKPlayer();
				mp.setDataSource(path);
				isPlayISO = true;
				isSetDataSource = true;
				prePosition = position;
				setup();
				LogTool.i("DSD setDataSource:" + path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else {
			File file = new File(path);
			if(!file.exists()){
				Toast.makeText(mContext, R.string.file_no_exist, Toast.LENGTH_SHORT).show();
				return;
			}
			try {
				if(mp != null){
					if (mp.isPlaying()) {
						mp.stop();
					}
					mp.reset();
					mp.release();
					mp = null;
					Thread.sleep(3*1000);
				}else {
					LogTool.i("ijkmp == null!!");
				}
				initFMPlayer();
				fmp.setDataSource(path);
				isPlayISO = false;
				LogTool.i("========fmp.prepareAsync========");
				isSetDataSource = true;
				prePosition = position;
				setup();
			} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
				LogTool.e("IllegalArgumentException " + e.toString());
			e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
					LogTool.e("SecurityException " + e.toString());
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
					LogTool.e("IllegalStateException " + e.toString());
				fmp.stop();
				fmp.reset();
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
					LogTool.e("IOException " + e.toString());
				fmp.stop();
				fmp.reset();
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			/*try {
				if(!isPlayISO){
					if(fmp != null){
						fmp.reset();
						fmp.release();
					}
					initIJKPlayer();
				}
				mp.reset();
				mp.setDataSource(path);
				isSetDataSource = true;
				isPlayISO = true;
				prePosition = position;
				setup();
				LogTool.i("setDataSource:" + path);
			} catch (Exception e) {
				e.printStackTrace();
			}*/
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/***播放*/
	private void play() {
		if(isPlayISO){
			if (mp != null) {
				mp.start();
				if(handler != null){
					Message msg = handler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC, 1, 0);
					msg.sendToTarget();
				}
			}
		}else {
			if (fmp != null) {
				fmp.start();
				if(handler != null){
					Message msg = handler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC, 1, 0);
					msg.sendToTarget();
				}
			}
		}
		flag = 1;

	}

	/**暂停*/
	private void pause() {
		if(isPlayISO){
			if (mp != null) {
				mp.pause();
				if(handler != null){
					Message msg = handler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC, 0, 0);
					msg.sendToTarget();
				}
			}
		}else {
			if (fmp != null) {
				fmp.pause();
				if(handler != null){
					Message msg = handler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC, 0, 0);
					msg.sendToTarget();
				}
			}
		}
		flag = 0;
	}

	/** 停止*/
	private void stop() {
		if(isPlayISO){
			if (mp != null) {
				LogTool.i("mp.stop();");
				if(mp.isPlaying()){
					mp.stop();
				}
				mp.reset();
				if(handler != null){
					handler.removeMessages(1);
				}
				//add by victor 通知前台关闭播放动画
				Intent intent = new Intent(Contsant.PlayAction.MUSIC_STOP);
				sendBroadcast(intent);
			}
		}else {
			if (fmp != null) {
				LogTool.i("mp.stop();");
				if(fmp.isPlaying()) {
					fmp.stop();
				}
				fmp.reset();
				if(handler != null){
					handler.removeMessages(1);
				}
				//add by victor 通知前台关闭播放动画
				Intent intent = new Intent(Contsant.PlayAction.MUSIC_STOP);
				sendBroadcast(intent);
			}
		}
	}

	/** 初始化1*/
	private void setup() {
		if(isPlayISO) {
			if(mp != null){
				try {
					if (!mp.isPlaying()) {
						mp.prepareAsync();
					}
					mp.setOnPreparedListener(mPreparedListener);
					duration = mp.getDuration();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
		}else {
			if(fmp != null){
				try {
					if (!fmp.isPlaying()) {
						fmp.prepareAsync();
					}
					fmp.setOnPreparedListener(mFmpPreparedListener);
					duration = fmp.getDuration();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
		}
		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_DURATION);
		broadCastMusicInfo(intent);
	}

	/** 获得随机位置*/
	private int getRandomPostion(boolean loopAll) {
		int ret = -1;

		if (MusicPlayFragment.randomNum < musicDatas.size() - 1) {
			MusicPlayFragment.randomIDs[MusicPlayFragment.randomNum] = position;
			ret = MusicPlayFragment.findRandomSound(musicDatas.size());
			MusicPlayFragment.randomNum++;

		} else if (loopAll == true) {
			MusicPlayFragment.randomNum = 0;
			for (int i = 0; i < musicDatas.size(); i++) {
				MusicPlayFragment.randomIDs[i] = -1;
			}
			MusicPlayFragment.randomIDs[MusicPlayFragment.randomNum] = position;
			ret = MusicPlayFragment.findRandomSound(musicDatas.size());
			MusicPlayFragment.randomNum++;
		}

		return ret;
	}

	/**
	 * 下一首
	 */
	private void nextOne() {
		switch (MusicPlayFragment.loop_flag) {
			case Contsant.LoopMode.LOOP_ORDER://顺序播放
				if (position == musicDatas.size() - 1) {
					stop();
					return;
				} else if (position < musicDatas.size() - 1) {
					position++;
				}
				break;
			case Contsant.LoopMode.LOOP_ONE://单曲循环播放
				//不做操作，继续播放当前歌曲
				break;
			case Contsant.LoopMode.LOOP_ALL://全部循环播放
				position++;
				if (position == musicDatas.size()) {
					position = 0;
				}
				break;
			case Contsant.LoopMode.LOOP_RANDOM://随机播放
				int i = getRandomPostion(false);
				if (i == -1) {
					stop();
					return;
				} else {
					position = i;
				}
				break;
		}
		mPath = musicDatas.get(position).path;
		playPath(mPath);
		if(handler != null){
			handler.removeMessages(1);
		}

		setup();
		play();
		SharePreferencesUtil.putInt(mContext, Contsant.CURRENT_FRAG, Contsant.Frag.MUSIC_PLAY_FRAG);
		SharePreferencesUtil.putString(mContext, Contsant.CURRENT_MUSIC_NAME, musicDatas.get(position).title);
		Bundle bundle = new Bundle();
		bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
		bundle.putInt(Contsant.POSITION_KEY, position);
		DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变

		//这里不能再发播放下一曲的广播，因为当前已经播放的是下一曲
		/*Intent intent0 = new Intent();
		intent0.setAction(MUSIC_NEXT);
		intent0.putExtra("position", position);
		sendBroadcast(intent0);*/

		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_LIST);
		intent.putExtra("position", position);
		sendBroadcast(intent);

		Intent intent1 = new Intent();
		intent1.setAction(Contsant.PlayAction.MUSIC_UPDATE);
		intent1.putExtra("position", position);
		sendBroadcast(intent1);

		Intent intent2 = new Intent("com.app.musictitle");
		intent2.putExtra("title", musicDatas.get(position).title);
		sendBroadcast(intent2);
		ShowNotifcation();
	}

	/** 上一首*/
	private void lastOne() {
		if (musicDatas.size() == 1) {
			position = position;

		} else if (position == 0) {
			position = musicDatas.size() - 1;
		} else if (position > 0) {
			position--;
		}
		mPath = musicDatas.get(position).path;
		playPath(mPath);
		handler.removeMessages(1);
		setup();
		play();

		SharePreferencesUtil.putInt(mContext, Contsant.CURRENT_FRAG, Contsant.Frag.MUSIC_PLAY_FRAG);
		SharePreferencesUtil.putString(mContext, Contsant.CURRENT_MUSIC_NAME, musicDatas.get(position).title);
		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_LIST);
		intent.putExtra("position", position);
		sendBroadcast(intent);

		Intent intent1 = new Intent();
		intent1.setAction(Contsant.PlayAction.MUSIC_UPDATE);
		intent1.putExtra("position", position);
		sendBroadcast(intent1);

		Intent intent2 = new Intent("com.app.musictitle");
		intent2.putExtra("title", musicDatas.get(position).title);
		sendBroadcast(intent2);
	}

	/** 操作数据库*/
	private void DBOperate(int pos) {
		dbHelper = new DBHelper(this, "music.db", null, 2);
		Cursor c = dbHelper.query(pos);
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = formatter.format(currentTime);
		try {
			if (c==null||c.getCount()==0){
				ContentValues values = new ContentValues();
				values.put("music_id", pos);
				values.put("clicks", 1);
				values.put("latest", dateString);
				dbHelper.insert(values);
			} else {
				c.moveToNext();
				int clicks = c.getInt(2);
				clicks++;
				ContentValues values = new ContentValues();
				values.put("clicks", clicks);
				values.put("latest", dateString);
				dbHelper.update(values, pos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (c != null) {
			c.close();
			c = null;
		}
		if (dbHelper!=null){
			dbHelper.close();
			dbHelper = null;
		}
	}

	/*** 来电时监听播放状态*/
	protected BroadcastReceiver PhoneListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_ANSWER)) {

			}else if(intent.getAction().equals(Intent.ACTION_SHUTDOWN)){
				saveLastPlayInfo();
			}else if(intent.getAction().equals(Contsant.PlayAction.MUSIC_STOP_SERVICE)){
				saveLastPlayInfo();
				if(isPlayISO){
					if(mp != null){
						mp.stop();
						mp.reset();
					}
				}else {
					if(fmp != null){
						fmp.stop();
						fmp.reset();
					}
				}
				stopSelf();//在service中停止service
			}
		}
	};
	/**
	 * 记录最后一次播放信息
	 * */
	private void saveLastPlayInfo(){
		LogTool.d(position + mMusicName + mSampleRate);
		SharePreferencesUtil.putInt(mContext, Contsant.MUSIC_INFO_POSTION, position);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_NAME, mMusicName);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_FORMAT, mMusicFormat);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_BITRATE, mBitRate);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_SAMPLERATE, mSampleRate);
		SharePreferencesUtil.putLong(mContext, Contsant.MUSIC_INFO_DURATION, duration);
	}
	/** 桌面小插件*/
	protected BroadcastReceiver appWidgetReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/*if (intent.getAction().equals("com.app.playmusic")) {
				if(isPlayISO){
					if (mp.isPlaying()) {
						pause();
						Intent pauseIntent = new Intent("com.app.pause");
						sendBroadcast(pauseIntent);
					} else {
						play();
						Intent playIntent = new Intent("com.app.play");
						sendBroadcast(playIntent);
					}
				}else {
					if (mp.isPlaying()) {
						pause();
						Intent pauseIntent = new Intent("com.app.pause");
						sendBroadcast(pauseIntent);
					} else {
						play();
						Intent playIntent = new Intent("com.app.play");
						sendBroadcast(playIntent);
					}
				}
			} else if (intent.getAction().equals("com.app.nextone")) {
				nextOne();
				Intent playIntent = new Intent("com.app.play");
				sendBroadcast(playIntent);
			} else if (intent.getAction().equals("com.app.lastone")) {
				lastOne();
				Intent playIntent = new Intent("com.app.play");
				sendBroadcast(playIntent);
			} else if (intent.getAction().equals("com.app.startapp")) {
				Intent intent1 = new Intent("com.app.musictitle");
				intent1.putExtra("title", musicDatas.get(position).title);
				sendBroadcast(intent1);
			}*/
		}
	};

	@Override
	public void update(Observable observable, Object data) {
		if (data instanceof Bundle) {
			Bundle bundle = (Bundle) data;
			int action = bundle.getInt(Contsant.ACTION_KEY);
			int position = bundle.getInt(Contsant.POSITION_KEY);
			if (action == Contsant.Action.REMOVE_MUSIC) {
				if (musicDatas != null && musicDatas.size() > 0) {
					if (position < musicDatas.size()) {
						musicDatas.remove(position);
					}
				}
			}
		}
	}

	private void registerHeadsetPlugReceiver(){
		IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		intentFilter.addAction("android.intent.action.HEADSET_PLUG");
		intentFilter.addAction("android.media.AUDIO_BECOMING_NOISY");
		registerReceiver(headsetPlugReceiver, intentFilter);
	}

	/**
	 * 注册耳机插拔广播
	 */
	protected BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			LogTool.i(action);
			String word = null;
			if ("android.media.AUDIO_BECOMING_NOISY".equals(action)) {
				if(intent.hasExtra("state")){
					if(intent.getIntExtra("state", 0)==0){
						isHeadsetIn = false;
						pause();
						word = context.getResources().getString(R.string.headset_out);
					}
				}else{
					pause();
					word = context.getResources().getString(R.string.headset_out);
				}
			}else if ("android.intent.action.HEADSET_PLUG".equals(action) && !isHeadsetIn) {
				if(intent.getIntExtra("state", 0)==1){
					play();
					word = context.getResources().getString(R.string.headset_in);
				}
			}
			if(word != null && !TextUtils.isEmpty(word)){
				mToast = Toast.makeText(context, word, Toast.LENGTH_LONG);
				mToast.show();
				if(handler != null){
					handler.sendEmptyMessageDelayed(Contsant.Msg.DELAY_CANCLE_TOAST, DELAY_CLOSE_TOAST);
				}
			}
		}
	};

	public void showMediaInfo() {
		if(isPlayISO){
			if (mp == null)
				return;
			mp.getDuration();

			ITrackInfo trackInfos[] = mp.getTrackInfo();
			if (trackInfos != null) {
				int index = -1;
				for (ITrackInfo trackInfo : trackInfos) {
					index++;
					int trackType = trackInfo.getTrackType();
					IMediaFormat mediaFormat = trackInfo.getFormat();
					if (mediaFormat == null) {
					} else if (mediaFormat instanceof IjkMediaFormat) {
						switch (trackType) {
							case ITrackInfo.MEDIA_TRACK_TYPE_VIDEO:
								LogTool.i(getString(R.string.mi_codec) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_LONG_NAME_UI));
								LogTool.i(getString(R.string.mi_profile_level) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_PROFILE_LEVEL_UI));
								LogTool.i(getString(R.string.mi_pixel_format) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_PIXEL_FORMAT_UI));
								LogTool.i(getString(R.string.mi_resolution) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_RESOLUTION_UI));
								LogTool.i(getString(R.string.mi_frame_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_FRAME_RATE_UI));
								LogTool.i(getString(R.string.mi_bit_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI));
								break;
							case ITrackInfo.MEDIA_TRACK_TYPE_AUDIO:
								mSampleRate = mediaFormat.getString(IjkMediaFormat.KEY_IJK_SAMPLE_RATE_UI);
								mBitRate = mediaFormat.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI);
								musicDatas.get(position).setSampleRate(mSampleRate);
								musicDatas.get(position).setBitRate(mBitRate);
								break;
							default:
								break;
						}
					}
				}
			}
		}
	}
	public static void showToast(final Activity activity, final String word, final long time){
		activity.runOnUiThread(new Runnable() {
			public void run() {
				final Toast toast = Toast.makeText(activity, word, Toast.LENGTH_LONG);
				toast.show();
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						toast.cancel();
					}
				}, time);
			}
		});
	}

	//FFmpegMediaPlayer------------------------------------------------------------------------------------
	private void initFMPlayer() {
		if (fmp != null) {
			if (fmp.isPlaying()) {
				fmp.stop();
			}
			fmp.reset();
			fmp.release();
			fmp = null;
		}
		fmp = new FFmpegMediaPlayer();
		fmp.setWakeMode(MusicService.this, PowerManager.PARTIAL_WAKE_LOCK);
		setListener();
	}

	private void setListener() {

		fmp.setOnPreparedListener(mFmpPreparedListener);
		fmp.setOnErrorListener(mFmpErrorListener);
		fmp.setOnCompletionListener(mFmpCompletionListener);
		fmp.setOnTimedTextListener(mFmpTimedTextListener);
		fmp.setOnSeekCompleteListener(mFmpSeekBarChangeListener);
		fmp.setOnInfoListener(mFmpInfoListener);
//		mTestPlayBtn.setOnClickListener(this);
//		mTestChooseFileBtn.setOnClickListener(this);
//		mTestPauseBtn.setOnClickListener(this);
//		mPlayBar.setOnSeekBarChangeListener(mPlayBarChangeListener);
	}

	private FFmpegMediaPlayer.OnPreparedListener mFmpPreparedListener = new FFmpegMediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(FFmpegMediaPlayer mp) {
			handler.sendEmptyMessage(Contsant.Msg.ADD_VOL);
			// TODO Auto-generated method stub
			fmp.start();

			LogTool.i("OnPreparedListener onPrepared fmp start");

			LogTool.i("onPrepared");
			handler.sendEmptyMessage(Contsant.PlayStatus.STATE_PREPARED);
			if(mSeekPosition != 0){
				LogTool.i("mSeekPosition" + mSeekPosition);
				fmp.seekTo((int)mSeekPosition);
			}else if(musicDatas.get(position).seekPostion != 0 && isSetDataSource){
				LogTool.i("musicDatas mSeekPosition" + musicDatas.get(position).seekPostion);
				fmp.seekTo((int)musicDatas.get(position).seekPostion);
			}
			initAudioInfo();
			saveLastPlayInfo();
		}
	};

	private FFmpegMediaPlayer.OnErrorListener mFmpErrorListener = new FFmpegMediaPlayer.OnErrorListener() {

		@Override
		public boolean onError(FFmpegMediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub
			if(fmp != null){
				fmp.release();
			}
			LogTool.e("OnPreparedListener onError fmp release");
			return false;
		}
	};

	private FFmpegMediaPlayer.OnCompletionListener mFmpCompletionListener = new FFmpegMediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(FFmpegMediaPlayer fmp) {
			// TODO Auto-generated method stub
			fmp.stop();
			fmp.reset();
			LogTool.d("onCompletion");
			nextOne();
		}

	};
	private FFmpegMediaPlayer.OnTimedTextListener mFmpTimedTextListener = new FFmpegMediaPlayer.OnTimedTextListener() {

		@Override
		public void onTimedText(FFmpegMediaPlayer mp, TimedText text) {
			// TODO Auto-generated method stub
			Log.i("PlayerActivity", "mFmpTimedTextListener onTimedText");
		}
	};

	private FFmpegMediaPlayer.OnSeekCompleteListener mFmpSeekBarChangeListener = new FFmpegMediaPlayer.OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(FFmpegMediaPlayer mp) { // 用來指示seek操作結束
			// TODO Auto-generated method stub
			Log.i("PlayerActivity", "mFmpSeekBarChangeListener onSeekComplete");
		}
	};

	private FFmpegMediaPlayer.OnInfoListener mFmpInfoListener = new FFmpegMediaPlayer.OnInfoListener(){

		@Override
		public boolean onInfo(FFmpegMediaPlayer mp, int what, int extra) {
			LogTool.d("getCurrentPosition:" + mp.getCurrentPosition());
			handler.sendEmptyMessage(Contsant.PlayStatus.STATE_INFO);
			return false;
		}
	};

	MyVolumeReceiver mVolumeReceiver;
	/**
	 * 注册当音量发生变化时接收的广播
	 */
	private void registerVolumeReceiver(){
		mVolumeReceiver = new MyVolumeReceiver() ;
		IntentFilter filter = new IntentFilter() ;
		filter.addAction("android.media.VOLUME_CHANGED_ACTION") ;
		filter.addAction("android.media.VIBRATE_SETTING_CHANGED") ;
		registerReceiver(mVolumeReceiver, filter) ;
	}

	private void unRegisterVolume(){
		if(mVolumeReceiver != null){
			unregisterReceiver(mVolumeReceiver);
			mVolumeReceiver = null;
		}
	}

	/**
	 * 处理音量变化时的界面显示
	 * @author long
	 */
	private class MyVolumeReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
//			LogTool.d("Volume Action:"+ intent.getAction());
			//如果音量发生变化则更改seekbar的位置
			if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
				int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ;// 当前的媒体音量
//				LogTool.d("currVolume:" + currVolume);
				/*if(currVolume > 0){
					execCommand(String.format(strCommand, currVolume + 126));
				}else {
					execCommand(String.format(strCommand, 0));
				}*/
			}
		}
	}


	private String strCommand = "tinymix 0 %d";
	public void execCommand(String command){
		LogTool.d("Volume command:" + command);
		Process proc = null;        //这句话就是shell与高级语言间的调用
		try {
			// start the ls command running
			//String[] args =  new String[]{"sh", "-c", command};
			Runtime runtime = Runtime.getRuntime();
			proc = runtime.exec(command);
			//如果有参数的话可以用另外一个被重载的exec方法
			//实际上这样执行时启动了一个子进程,它没有父进程的控制台
			//也就看不到输出,所以我们需要用输出流来得到shell执行后的输出
			InputStream inputstream = proc.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			// read the ls output
			String line = "";
			StringBuilder sb = new StringBuilder(line);
			while ((line = bufferedreader.readLine()) != null) {
				//System.out.println(line);
				sb.append(line);
				sb.append('\n');
			}
			LogTool.d("result:" + sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		//tv.setText(sb.toString());
		//使用exec执行不会等执行成功以后才返回,它会立即返回
		//所以在某些情况下是很要命的(比如复制文件的时候)
		//使用wairFor()可以等待命令执行完成以后才返回
		try {
			if (proc.waitFor() != 0) {
				System.err.println("exit value = " + proc.exitValue());
			}
		}
		catch (InterruptedException e) {
			System.err.println(e);
		}
	}
}
