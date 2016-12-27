package org.app.enjoy.music.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.view.CircleImageView;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.musicplayer.FileExplorerActivity;
import org.app.enjoy.musicplayer.R;

import java.util.List;

public class MusicListAdapter extends BaseAdapter implements AbsListView.OnScrollListener {
	private String TAG = "MusicListAdapter";
	private Context mcontext;// 上下文
	private List<MusicData> musicDatas;
	private int currentPosition = -1;
	private int currentLongPosition = -1;
	private int currentMusicId  = -1;//当前播放音乐id
	private boolean cancelLong;
	private boolean isFirstEnter = true;//记录是否刚打开程序，用于解决进入程序不滚动屏幕，不会下载图片的问题。
	private int mFirstVisibleItem;//一屏中第一个item的位置
	private int mVisibleItemCount;//一屏中所有item的个数
	private ListView mListView;
//	private Typeface typeFace;
	public MusicListAdapter(Context context,ListView listView) {
		mcontext = context;
		mListView = listView;
		mListView.setOnScrollListener(this);
		//经典细圆字体
//		typeFace = Typeface.createFromAsset(mcontext.getAssets(), "fonts/DroidSansFallback.ttf");
	}

	public void setDatas (List<MusicData> musicDatas) {
		this.musicDatas = musicDatas;
	}

	public void setCurrentPosition (int position) {
		if(musicDatas != null && musicDatas.size() > 0 && musicDatas.size() > position){
			currentPosition = position;
			currentMusicId = musicDatas.get(currentPosition).id;
			notifyDataSetChanged();
		}
	}
	public void setCurrentLongPosition (int position) {
		currentLongPosition = position;
		notifyDataSetChanged();
	}
	public void cancelLongClick (boolean cancel) {
		cancelLong = cancel;
	}

	@Override
	public int getCount() {
		return musicDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return musicDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewholder;
		if (convertView == null) {
			viewholder = new ViewHolder();
			convertView = LayoutInflater.from(mcontext).inflate(R.layout.lv_music_item, null);
			viewholder.mCivAlbum = (CircleImageView) convertView.findViewById(R.id.civ_album);
			viewholder.mMtvTitle = (MovingTextView) convertView.findViewById(R.id.mtv_title);
			viewholder.singers = (TextView) convertView.findViewById(R.id.singer);
			viewholder.times = (TextView) convertView.findViewById(R.id.time);
			viewholder.mIconRemove = (ImageView) convertView.findViewById(R.id.iv_remove);
			viewholder.mIvLocation = (ImageView) convertView.findViewById(R.id.iv_location);
//			viewholder.song_list_item_menu = (ImageButton) convertView.findViewById(R.id.ibtn_song_list_item_menu);

			convertView.setTag(viewholder);
		} else {
			viewholder = (ViewHolder) convertView.getTag();
		}

		if (cancelLong) {
			viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.times.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.mIconRemove.setVisibility(View.GONE);
			viewholder.mIvLocation.setVisibility(View.GONE);
		} else {
			if (currentLongPosition == position) {
				viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.red));
				viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.red));
				viewholder.times.setTextColor(mcontext.getResources().getColor(R.color.red));
				viewholder.mIconRemove.setVisibility(View.VISIBLE);
				viewholder.mIvLocation.setVisibility(View.VISIBLE);
			} else {
				viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.white));
				viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.white));
				viewholder.times.setTextColor(mcontext.getResources().getColor(R.color.white));
				viewholder.times.setVisibility(View.VISIBLE);
				viewholder.mIconRemove.setVisibility(View.GONE);
				viewholder.mIvLocation.setVisibility(View.GONE);
			}
		}
		if (currentPosition == position) {
			viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.times.setVisibility(View.VISIBLE);
			viewholder.mIconRemove.setVisibility(View.GONE);
			viewholder.mIvLocation.setVisibility(View.GONE);
			convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_yellow));
		} else {
			if (position % 2 == 0) {
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_blue));
			} else {
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.dark_blue));
			}
		}

		MusicData data = musicDatas.get(position);
		viewholder.mMtvTitle.setText(data.title);
		viewholder.mCivAlbum.setImageResource(R.drawable.default_album);

		/*String albumId = data.getAlbumId();
		Bitmap bitmap;
		if (!TextUtils.isEmpty(albumId)) {
			viewholder.mCivAlbum.setImageResource(R.drawable.default_album);
			bitmap = imageDownLoader.showCacheBitmap(albumId);
			if (bitmap != null) {
				viewholder.mCivAlbum.setImageBitmap(bitmap);
			} else {
				final ViewHolder finalViewholder = viewholder;
				imageDownLoader.getAlbumImage(mcontext, data.getId(), albumId, new ImageDownLoader.onImageLoaderListener() {
					@Override
					public void onImageLoader(Bitmap bitmap) {
						if (bitmap != null) {
							finalViewholder.mCivAlbum.setImageBitmap(bitmap);
						}
					}
				});
			}
		}*/

		viewholder.singers.setText(data.artist);
		if(data.duration > 0){
			viewholder.times.setText(toTime(data.duration));
		}else{
			viewholder.times.setText("");
		}

		viewholder.mIconRemove.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Bundle bundle = new Bundle();
				bundle.putInt(Contsant.ACTION_KEY,Contsant.Action.REMOVE_MUSIC);
				bundle.putInt(Contsant.POSITION_KEY, currentLongPosition);
				DataObservable.getInstance().setData(bundle);
				MusicUtil.deleteFile(mcontext,musicDatas.get(currentLongPosition).data,musicDatas.get(currentLongPosition).id);
				musicDatas.remove(currentLongPosition);

				currentLongPosition = -1;
				//删除之后重新找到当前播放音乐的position
				for (int i=0;i<musicDatas.size();i++) {
					if (currentMusicId == musicDatas.get(i).id) {
						currentPosition = i;
						break;
					}
				}
				notifyDataSetChanged();
			}
		});

		viewholder.mIvLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mcontext.startActivity(new Intent(mcontext, FileExplorerActivity.class));
			}
		});

		return convertView;

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		//仅当ListView静止时才去下载图片，ListView滑动时取消所有正在下载的任务
		/*if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE){
			showImage(mFirstVisibleItem, mVisibleItemCount);
		}else{
			cancelTask();
		}*/
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		/*mFirstVisibleItem = firstVisibleItem;
		mVisibleItemCount = visibleItemCount;
		// 因此在这里为首次进入程序开启下载任务。
		if(isFirstEnter && visibleItemCount > 0){
			showImage(mFirstVisibleItem, mVisibleItemCount);
			isFirstEnter = false;
		}*/
	}



	public class ViewHolder {
		public CircleImageView mCivAlbum;
		public MovingTextView mMtvTitle;
		public TextView singers;
		public TextView times;
		public ImageButton song_list_item_menu;
		public ImageView mIconRemove, mIvLocation;
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
