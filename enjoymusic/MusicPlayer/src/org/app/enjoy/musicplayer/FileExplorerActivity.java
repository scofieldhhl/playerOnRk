/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.app.enjoy.musicplayer;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.app.enjoy.eventbus.FileExplorerEvents;
import org.app.enjoy.music.adapter.MusicListAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.frag.FileListFragment;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.util.MusicUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class FileExplorerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        String lastDirectory = "";
        if (!TextUtils.isEmpty(lastDirectory) && new File(lastDirectory).isDirectory())
            doOpenDirectory(lastDirectory, false);
        else
            doOpenDirectory("/", false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        FileExplorerEvents.getBus().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        FileExplorerEvents.getBus().unregister(this);
    }

    private void doOpenDirectory(String path, boolean addToBackStack) {
        Fragment newFragment = FileListFragment.newInstance(path);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.body, newFragment);

        if (addToBackStack)
            transaction.addToBackStack(null);
        transaction.commit();
    }

    @Subscribe
    public void onClickFile(FileExplorerEvents.OnClickFile event) {
        File f = event.mFile;
        try {
            f = f.getAbsoluteFile();
            f = f.getCanonicalFile();
            if (TextUtils.isEmpty(f.toString()))
                f = new File("/");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (f.isDirectory()) {
            String path = f.toString();
//            mSettings.setLastDirectory(path);
            doOpenDirectory(path, true);
        } else if (f.exists()) {
            if(f.getPath().toLowerCase().endsWith(".iso")){
                List<MusicData> list = MusicUtil.parseISO(f.getPath());
                if(list != null && list.size() > 0){
                    getPopupWindow(list);
                    popupWindow.showAtLocation(this.getCurrentFocus(), Gravity.CENTER,0,0);
                }else {
                    Toast.makeText(FileExplorerActivity.this, "ISO 文件解析失败！", Toast.LENGTH_SHORT);
                }
            }
        }
    }

    /**
     * 创建PopupWindow
     *
     */
    private PopupWindow popupWindow;
    protected  void  initPopupWindow(final List<MusicData> list){
        // TODO: 15/10/9
        //获取自定义布局文件activity_pop_left.xml 布局文件
        final View popipWindow_view = getLayoutInflater().inflate(R.layout.music_list,null,false);
        //创建Popupwindow 实例，200，LayoutParams.MATCH_PARENT 分别是宽高
        popupWindow = new PopupWindow(popipWindow_view, ViewGroup.LayoutParams.MATCH_PARENT - 150, ViewGroup.LayoutParams.MATCH_PARENT - 150,true);
        //设置动画效果
//        popupWindow.setAnimationStyle(R.style.AnimationFade);
        //点击其他地方消失
        popipWindow_view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (popipWindow_view != null && popipWindow_view.isShown()) {

                    popupWindow.dismiss();
                    popupWindow = null;
                }
                return false;
            }
        });
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        ListView listView = (ListView) popipWindow_view.findViewById(R.id.local_music_list);
        MusicListAdapter musicListAdapter = new MusicListAdapter(FileExplorerActivity.this, listView);
        musicListAdapter.setDatas(list);
        listView.setAdapter(musicListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) list);
                bundle.putInt(Contsant.POSITION_KEY, position);
                intent.putExtras(bundle);
                intent.setAction("com.app.media.MUSIC_SERVICE");
                intent.putExtra("op", Contsant.PlayStatus.PLAY);// 向服务传递数据
                intent.setPackage(getPackageName());
                startService(intent);
            }
        });
    }
    /**
     * 获取PopipWinsow实例
     */
    private  void  getPopupWindow(List<MusicData> list){
        if (null!=popupWindow){
            popupWindow.dismiss();
            return;
        }else {
            initPopupWindow(list);
        }}
}
