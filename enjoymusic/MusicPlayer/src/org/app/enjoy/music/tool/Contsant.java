package org.app.enjoy.music.tool;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import org.app.enjoy.music.tool.XfDialog.Builder;
import org.app.enjoy.musicplayer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 常量类
 * @author
 */
public class Contsant {
	public static final String MUSIC_LIST_KEY = "MUSIC_LIST_KEY";
	public static final String POSITION_KEY = "POSITION_KEY";
	public static final String POSITION_PATH = "POSITION_PATH";
	//	public static final String REMOVE_MUSIC = "REMOVE_MUSIC";
	public static final String SEEK_POSITION = "SEEK_POSITION";
	public static final String ACTION_KEY = "ACTION_KEY";
	public static final String DSD_APE = ".ape";
	public static final String DSD_CUE = ".cue";
	public static final String DSD_ISO = "iso";
	public static final String DSD_ISO_HEADER = "SACD://";
	public static final String MA_DATA = "madata";
	public static final String CURRENT_FRAG = "CURRENT_FRAG";
	public static final String START_SERVICE_FIRST = "start_service_first ";

	public static final String MUSIC_INFO_POSTION = "music_position";
	public static final String MUSIC_INFO_NAME = "music_name";
	public static final String MUSIC_INFO_FORMAT = "music_format";
	public static final String MUSIC_INFO_SAMPLERATE = "music_sample_rate";
	public static final String MUSIC_INFO_BITRATE = "music_bit_rate";
	public static final String MUSIC_INFO_DURATION = "music_duration";
	public static final String CURRENT_MUSIC_NAME = "current_music_name";
	public static final String CURRENT_PLAY_GROUP = "current_play_group";
	public static final String CHAR_ENCODING = "GB2312";
	public static final  int DELAY_SEEK_HEADER = 1500;

	public static class Msg {
		public static final int UPDATE_PLAY_LIST =                 0x101;
		public static final int UPDATE_PLAY_LIST_EXTENSION =       0x102;
		public static final int PLAY_CUE =       0x103;
		public static final int PLAY_MUSIC =       0x104;
		public static final int UPDATE_CATEGORY_LIST =       0x105;
		public static final int CURRENT_PLAY_POSITION_CHANGED =       0x106;
		public static final int SEARCH_ARTIST_COMPLETE =       0x107;
		public static final int SHOW_BOTTOM_PLAY_INFO =       0x108;
		public static final int PLAY_LRC_SWITCH =       0x109;
		public static final int UPDATE_ARTIST_LIST =       0x110;
		public static final int UPDATE_ALBUM_LIST =       0x111;
		public static final int SEARCH_MUSIC_COMPLETE =       0x112;
		public static final int UPDATE_SEEK_BAR =       0x113;
		public static final int DELAY_CANCLE_TOAST =       0x114;

		public static final int ADD_VOL =       0x115;
		public static final int LOWER_VOL =       0x116;
	}

	public static class Frag {
		public static final int MUSIC_LIST_FRAG = 1;
		public static final int ARTIST_FRAG = 2;
		public static final int ALBUM_FRAG = 3;
		public static final int DIY_FRAG = 4;
		public static final int SEARCH_MUSIC_FRAG = 5;
		public static final int MUSIC_PLAY_FRAG = 6;
	}

	public static class Action {
		public static final int REMOVE_MUSIC =                 0x201;
		public static final int CURRENT_TIME_MUSIC =                 0x202;
		public static final int DURATION_MUSIC =                 0x203;
		public static final int NEXTONE_MUSIC =                 0x204;
		public static final int UPDATE_MUSIC =                 0x205;
		public static final int PLAY_PAUSE_MUSIC =                 0x206;
		public static final int MUSIC_STOP =                 0x207;
		public static final int POSITION_CHANGED =                 0x208;
		public static final int PLAY_MUSIC =                 0x209;
		public static final int GOTO_MUSIC_PLAY_FRAG =                 0x210;
		public static final int GOTO_MUSIC_LIST_FRAG =                 0x211;
		public static final int MUSIC_LIST_ITEM_CLICK =                 0x212;
	}

	public static class PlayStatus {
		public static final int PLAY =                 1;
		public static final int PAUSE =                 2;
		public static final int STOP =                 3;
		public static final int PROGRESS_CHANGE =                 4;
		public static final int STATE_PREPARED =                 0x301;
		public static final int STATE_INFO =       0x302;
	}
	public static class LoopMode {
		public static final int LOOP_ORDER = 1;//顺序播放
		public static final int LOOP_ONE = 2;//单曲循环
		public static final int LOOP_ALL = 3;//全部循环
		public static final int LOOP_RANDOM = 4;//随机播放
	}

	public static class PlayAction {
		public static final String MUSIC_CURRENT = "com.app.currentTime";// Action的状态
		public static final String MUSIC_DURATION = "com.app.duration";
		public static final String MUSIC_LAST = "com.app.last";
		public static final String MUSIC_NEXT = "com.app.next";
		public static final String MUSIC_UPDATE = "com.app.update";
		public static final String MUSIC_PLAY = "com.app.play";
		public static final String MUSIC_PAUSE = "com.app.pause";
		public static final String MUSIC_STOP = "com.app.stop";
		public static final String MUSIC_LIST = "com.app.list";

		public static final String PLAY_PAUSE_NEXT = "com.app.play.pause";
		public static final String MUSIC_PREPARED = "com.app.prepared";

		public static final String MUSIC_PLAY_SERVICE = "com.app.media.MUSIC_SERVICE";
		public static final String MUSIC_STOP_SERVICE = "com.app.media.stopService";
	}

	/**
	 * 数据库配置信息
	 * @author zhanghaifeng
	 * @date 2016-2-24
	 */
	public static class DbConfig {
		public static final String DB_NAME 						= "music_db";
		public static final String SCHEME 						= "content://";
		public static final String AUTHORITY 					= "content.music.mycontent";
		public static final String URI_PATH 						= SCHEME + AUTHORITY + "/";
		public static final int DB_VERSION 						= 1;
	}

	/**
	 * 表名
	 * @author victor
	 * @date 2016-2-24
	 */
	public static class TB {
		public static final String CATEGORY						  		= "category_tb";
		public static final String MUSIC						  		= "music_tb";
	}

	/***判断网络是否正常**/
	public static boolean getNetIsAvailable(Context context){
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo=connManager.getActiveNetworkInfo();
		if(networkInfo==null){
			return false;
		}
		return networkInfo.isAvailable();
	}
	/**
	 * 提示消息
	 * */
	public static Toast showMessage(Toast toastMsg, Context context, String msg) {
		if (toastMsg == null) {
			toastMsg = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		} else {
			toastMsg.setText(msg);
		}
		toastMsg.show();
		return toastMsg;
	}
	/**
	 * 删除文件并删除媒体库中数据
	 * */
	public static boolean deleteFile(Context context,String filePath){
		new File(filePath).delete();
		ContentResolver cr=context.getContentResolver();
		int id=-1;
		Cursor cursor=cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media._ID}
				, MediaStore.Audio.Media.DATA+"=?", new String[]{filePath}, null);
		if(cursor.moveToNext()){
			id=cursor.getInt(0);
		}
		cursor.close();
		if(id!=-1){
			return cr.delete(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id), null, null)>0;
		}
		return false;
	}


	/**
	 * 单个按钮对话框
	 * */
	public static XfDialog createConfirmDialog(Context context, String text,
											   String title, String msg, DialogInterface.OnClickListener listener) {
		Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(msg);
		builder.setPositiveButton(text, listener);
		return builder.create();
	}
	/**
	 * 获取屏幕的大小0：宽度 1：高度
	 * */
	public static int[] getScreen(Context context) {
		WindowManager windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		return new int[] { (int) (outMetrics.density * outMetrics.widthPixels),
				(int) (outMetrics.density * outMetrics.heightPixels) };
	}
	/**
	 * 获取文件的后缀名，返回大写
	 * */
	public static String getSuffix(String str) {
		int i = str.lastIndexOf('.');
		if (i != -1) {
			return str.substring(i + 1).toUpperCase();
		}
		return str;
	}
	/**
	 * 格式化毫秒->00:00
	 * */
	public static String formatSecondTime(int millisecond) {
		if (millisecond == 0) {
			return "00:00";
		}
		millisecond = millisecond / 1000;
		int m = millisecond / 60 % 60;
		int s = millisecond % 60;
		return (m > 9 ? m : "0" + m) + ":" + (s > 9 ? s : "0" + s);
	}

	/**
	 * 格式化文件大小 Byte->MB
	 * */
	public static String formatByteToMB(int size){
		float mb=size/1024f/1024f;
		return String.format("%.2f",mb);
	}
	/**
	 * 根据文件路径获取文件目录
	 * */
	public static String clearFileName(String str) {
		int i = str.lastIndexOf(File.separator);
		if (i != -1) {
			return str.substring(0, i + 1);
		}
		return str;
	}
	/**
	 * 根据文件名获取不带后缀名的文件名
	 * */
	public static String clearSuffix(String str) {
		int i = str.lastIndexOf(".");
		if (i != -1) {
			return str.substring(0, i);
		}
		return str;
	}
	/**
	 * 根据文件路径获取不带后缀名的文件名
	 * */
	public static String clearDirectory(String str) {
		int i = str.lastIndexOf(File.separator);
		if (i != -1) {
			return clearSuffix(str.substring(i + 1, str.length()));
		}
		return str;
	}
	/**
	 * 检查SD卡是否已装载
	 * */
	public static boolean isExistSdCard(){
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	/**
	 * 获得SD目录路径
	 * */
	public static String getSdCardPath(){
		return Environment.getExternalStorageDirectory().getPath();
	}

	/**
	 * 判断文件是否存在
	 * */
	public static boolean isExistFile(String file){
		return new File(file).exists();
	}
	/**
	 * 判断目录是否存在，不在则创建
	 * */
	public static void isExistDirectory(String directoryName) {
		File file = new File(directoryName);
		if (!file.exists()) {
			file.mkdirs();
		}
	}
	/**
	 * 修改文件名
	 * */
	public static String renameFileName(String str){
		int i=str.lastIndexOf('.');
		if(i!=-1){
			File file=new File(str);
			file.renameTo(new File(str.substring(0,i)));
			return str.substring(0,i);
		}
		return str;
	}
	/**
	 * 获取音乐列表类别
	 * */
	public static List<HashMap<String, Object>> getListMusicData() {
		List<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map = new HashMap<String, Object>();


		HashMap<String, Object> map2 = new HashMap<String, Object>();
		map2.put("icon", String.valueOf(R.drawable.local_singer));
		map2.put("title", "歌手");
		map2.put("icon2", String.valueOf(R.drawable.playlist_sign));
		data.add(map2);

		HashMap<String, Object> map3 = new HashMap<String, Object>();
		map3.put("icon", String.valueOf(R.drawable.local_album));
		map3.put("title", "专辑");
		map3.put("icon2", String.valueOf(R.drawable.playlist_sign));
		data.add(map3);

		HashMap<String, Object> map4 = new HashMap<String, Object>();
		map4.put("icon", String.valueOf(R.drawable.local_file));
		map4.put("title", "下载的音乐");
		map4.put("icon2", String.valueOf(R.drawable.playlist_sign));
		data.add(map4);


		HashMap<String, Object> map7 = new HashMap<String, Object>();
		map7.put("icon", String.valueOf(R.drawable.lately_player));
		map7.put("title", "最近播放");
		map7.put("icon2", String.valueOf(R.drawable.playlist_sign));
		data.add(map7);
		return data;
	}
	/***
	 * 获取列表下载数据
	 * @return
	 */
	public static List<HashMap<String, Object>> getListDownLoadData() {
		List<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("icon", String.valueOf(R.drawable.download_icon_down));
		map.put("title", "正在下载");
		map.put("icon2", String.valueOf(R.drawable.playlist_sign));
		data.add(map);

		HashMap<String, Object> map2 = new HashMap<String, Object>();
		map2.put("icon", String.valueOf(R.drawable.download_icon_finish));
		map2.put("title", "下载完成");
		map2.put("icon2", String.valueOf(R.drawable.playlist_sign));
		data.add(map2);
		return data;
	}

}
