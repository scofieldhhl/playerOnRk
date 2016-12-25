package org.app.enjoy.musicplayer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.app.enjoy.music.adapter.MenuAdapter;
import org.app.enjoy.music.adapter.ViewPagerAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.frag.MusicListFragment;
import org.app.enjoy.music.frag.MusicPlayFragment;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.Menu;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.music.tool.XfDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class MusicPlayActivity extends BaseActivity implements View.OnClickListener,Observer{

    public  int  REQUEST_CODE_SOME_FEATURES_PERMISSIONS = 1;
    private MusicListFragment musicListFragment;
    private MusicPlayFragment musicPlayFragment;
    private List<Fragment> frags = new ArrayList<>();
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;

    private Menu xmenu;//自定义菜单
    private Toast toast;//提示
    private Timers timer;//倒计时内部对象
    private TextView timers;//显示倒计时的文字
    private int c;//同上
    private PopupWindow popupWindow;
    private AudioManager audioManager;
    private int maxVolume;// 最大音量
    private int currentVolume;// 当前音量

    private List<MusicData> musicDatas = new ArrayList<>();
    private int currentPosition;
    private Context mContext;//上下文。这个重要！

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Action.GOTO_MUSIC_LIST_FRAG:
                    if (viewPager != null) {
                        viewPager.setCurrentItem(0);
                    }
                    break;
                case Contsant.Action.GOTO_MUSIC_PLAY_FRAG:
                    if (viewPager != null) {
                        viewPager.setCurrentItem(1);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);
        checkSDK();
        mContext = this;
        initialize();
        LoadMenu();

    }

    private void checkSDK(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            List<String> permissions = new ArrayList<String>();
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
//                preferencesUtility.setString("storage", "true");
            }

            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);

            } else {
//                preferencesUtility.setString("storage", "true");
            }

            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_CODE_SOME_FEATURES_PERMISSIONS);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("Permissions --> " + "Permission Granted: " + permissions[i]);
                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        System.out.println("Permissions --> " + "Permission Denied: " + permissions[i]);
                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    private void initialize () {
        DataObservable.getInstance().addObserver(this);
        viewPager = (ViewPager) findViewById(R.id.viewpager);

//        musicListFragment = new MusicListFragment();
        musicPlayFragment = new MusicPlayFragment();
//        frags.add(musicListFragment);
        frags.add(musicPlayFragment);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.setFrags(frags);
        String[] arrTitles = new String[1];
        arrTitles[0] = "play";
        viewPagerAdapter.setTitles(arrTitles);
        viewPager.setOnPageChangeListener(new ViewPagerOnPageChangeListener());
        viewPager.setAdapter(viewPagerAdapter);

        timers=(TextView) findViewById(R.id.timer_clock);

        // 获取系统音乐音量
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取系统音乐当前音量
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

    }

    /**
     * 初始化菜单
     */
    private void LoadMenu() {
        xmenu = new Menu(this);
        List<int[]> data1 = new ArrayList<int[]>();
        data1.add(new int[]{R.drawable.btn_menu_skin, R.string.skin_settings});
        data1.add(new int[]{R.drawable.btn_menu_exit, R.string.menu_exit_txt});

        xmenu.addItem(getResources().getString(R.string.common), data1, new MenuAdapter.ItemListener() {

            @Override
            public void onClickListener(int position, View view) {
                xmenu.cancel();
                /*if (position == 0) {
                    Intent it = new Intent(mContext, SkinSettingActivity.class);
                    startActivityForResult(it, 2);

                } else if (position == 1) {
                    exit();

                }*/
            }
        });
        List<int[]> data2 = new ArrayList<int[]>();
        data2.add(new int[]{R.drawable.btn_menu_setting, R.string.menu_settings});
        data2.add(new int[]{R.drawable.btn_menu_sleep, R.string.menu_time_txt});
        Setting setting = new Setting(this, false);
        String brightness=setting.getValue(Setting.KEY_BRIGHTNESS);
        if(brightness != null && brightness.equals("0")) {//夜间模式
            data2.add(new int[]{R.drawable.btn_menu_brightness, R.string.brightness_title});
        } else {
            data2.add(new int[]{R.drawable.btn_menu_darkness, R.string.darkness_title});
        }
        xmenu.addItem(getResources().getString(R.string.tool), data2, new MenuAdapter.ItemListener() {

            @Override
            public void onClickListener(int position, View view) {
                xmenu.cancel();
                if (position == 0) {

                } else if (position == 1) {
                    Sleep();
                } else if (position == 2) {
                    setBrightness(view);
                }
            }
        });
        List<int[]> data3 = new ArrayList<int[]>();
        data3.add(new int[]{R.drawable.btn_menu_about, R.string.about_title});
        xmenu.addItem(getResources().getString(R.string.help), data3, new MenuAdapter.ItemListener() {
            @Override
            public void onClickListener(int position, View view) {
                xmenu.cancel();
                /*Intent intent = new Intent(mContext, AboutActivity.class);
                startActivity(intent);*/

            }
        });
        xmenu.create();
    }

    /**
     * 休眠方法
     */
    private void Sleep(){
        final EditText edtext = new EditText(this);
        edtext.setText("5");//设置初始值
        edtext.setKeyListener(new DigitsKeyListener(false, true));
        edtext.setGravity(Gravity.CENTER_HORIZONTAL);//设置摆设位置
        edtext.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));//字体类型
        edtext.setTextColor(Color.BLUE);//字体颜色
        edtext.setSelection(edtext.length());//设置选择位置
        edtext.selectAll();//全部选择
        new XfDialog.Builder(mContext).setTitle(getResources().getString(R.string.please_enter_time)).
                setView(edtext).setPositiveButton(getResources().getString(R.string.confrim), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialog.cancel();
                /**如果输入小于2或者等于0会告知用户**/
                if (edtext.length() <= 2 && edtext.length() != 0) {
                    if (".".equals(edtext.getText().toString())) {
                        toast = Contsant.showMessage(toast, mContext, getResources().getString(R.string.enter_error));
                    } else {
                        final String time = edtext.getText().toString();
                        long Money = Integer.parseInt(time);
                        long cX = Money * 60000;
                        timer= new Timers(cX, 1000);
                        timer.start();//倒计时开始
                        toast = Contsant.showMessage(toast,mContext, getResources().getString(R.string.sleep_mode_start)
                                + String.valueOf(time)+ getResources().getString(R.string.close_app));
                        timers.setVisibility(View.INVISIBLE);
                        timers.setVisibility(View.VISIBLE);
                        timers.setText(String.valueOf(time));
                    }

                } else {
                    Toast.makeText(mContext, getResources().getString(R.string.please_enter_time_delay), Toast.LENGTH_SHORT).show();
                }

            }
        }).setNegativeButton(R.string.cancel, null).show();

    }

    /**
     * 产生一个倒计时
     */
    private class Timers extends CountDownTimer{

        public Timers(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            if (c==0) {
                exit();
//                finish();
//                onDestroy();
            }else {
                finish();
//                onDestroy();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
            timers.setText("" + millisUntilFinished / 1000 / 60 + ":"+ millisUntilFinished / 1000 % 60);
            // 假如这个数大于9 说明就是2位数了,可以直接输入。假如小于等于9 那就是1位数。所以前面加一个0
            String abc = (millisUntilFinished / 1000 / 60) > 9 ? (millisUntilFinished / 1000 / 60)+ "": "0" + (millisUntilFinished / 1000 / 60);
            String b = (millisUntilFinished / 1000 % 60) > 9 ? (millisUntilFinished / 1000 % 60)+ "": "0" + (millisUntilFinished / 1000 % 60);
            timers.setText(abc + ":" + b);
            timers.setVisibility(View.GONE);
        }

    }

    /**
     * 复写菜单方法
     */
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add("menu");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, android.view.Menu menu) {
        /** 菜单在哪里显示。参数1是该布局总的ID，第二个位置，第三，四个是XY坐标 **/
//        xmenu.showAtLocation(findViewById(R.id.rl_parent_cotent), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        /** 如果返回true的话就会显示系统自带的菜单，反之返回false的话就显示自己写的。 **/
        return false;
    }

    /**
     * 退出程序方法
     */
    private void exit(){
        Intent mediaServer = new Intent(mContext, MusicService.class);
        stopService(mediaServer);
        finish();
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Integer) {
            int action = (int) data;
            if (action == Contsant.Action.GOTO_MUSIC_PLAY_FRAG) {
                mHandler.sendEmptyMessage(Contsant.Action.GOTO_MUSIC_PLAY_FRAG);
            } else if (action == Contsant.Action.GOTO_MUSIC_LIST_FRAG) {
                mHandler.sendEmptyMessage(Contsant.Action.GOTO_MUSIC_LIST_FRAG);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if(popupWindow != null && popupWindow.isShowing()){
                popupWindow.dismiss();
            }else{
                new XfDialog.Builder(mContext).setTitle(R.string.info).setMessage(R.string.dialog_messenge).setPositiveButton(R.string.confrim, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exit();

                    }
                }).setNeutralButton(R.string.cancel, null).show();
            }

            return false;
        }else if(keyCode == KeyEvent.KEYCODE_BACK && popupWindow.isShowing()){
            popupWindow.dismiss();
        }*/
        finish();
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            popupWindow = null;
        }
        return super.onTouchEvent(event);
    }

    class ViewPagerOnPageChangeListener implements ViewPager.OnPageChangeListener{

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    break;
                case 1:
                    //暂时这么处理，滑动到播放界面时如果当前没有播放其他歌曲则播放第一个歌曲，
                    // 后续增加播放记录后改进从上次播放位置开始播放
                    if (currentPosition == 0) {
                        /*playMusic(currentPosition,0);*/

                        Bundle bundle = new Bundle();
                        bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
                        bundle.putInt(Contsant.POSITION_KEY, currentPosition);
                        DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    /**
     * 回调音量大小函数
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_UP) {
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_UP) {
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                return false;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
    }

    public void setCurrentPosition (int position) {
        currentPosition = position;
    }

    public void setMusicDatas (List<MusicData> musicList) {
        musicDatas = musicList;
    }

    public List<MusicData> getMusicDatas () {
        return musicDatas;
    }

    /**
     * 根据Position播放音乐
     */
    /*public void playMusic(int position, long seekPosition) {
        if (musicDatas != null && musicDatas.size() > 0) {
            play(position);

            Intent intent = new Intent(mContext,PlayMusicActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            bundle.putLong(Contsant.SEEK_POSITION, seekPosition);
            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.PLAY_MUSIC);
            intent.putExtras(bundle);
            DataObservable.getInstance().setData(bundle);
            DataObservable.getInstance().setData(Contsant.Action.GOTO_MUSIC_PLAY_FRAG);//进入播放fragmengt
        } else {
            final XfDialog xfdialog = new XfDialog.Builder(mContext).setTitle(getResources().getString(R.string.tip)).
                    setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
                    setPositiveButton(getResources().getString(R.string.confrim), null).create();
            xfdialog.show();
        }
    }
    public void play(int position) {
        LogTool.i("play---startService");
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", 1);// 向服务传递数据
        intent.setPackage(getPackageName());
        startService(intent);

    }*/

}
