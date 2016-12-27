package org.app.enjoy.music.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import org.app.enjoy.music.adapter.CategoryAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.db.DbDao;
import org.app.enjoy.musicplayer.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by victor on 2016/1/6.
 */
public class CategoryPopWindow extends PopupWindow implements AdapterView.OnItemClickListener{
    private String TAG = "CategoryPopWindow";
    private Context mContext;
    private ListView mLvCategory;
    private CategoryAdapter categoryAdapter;
    private List<String> categoryList = new ArrayList<>();
    private MusicData musicData;

    public CategoryPopWindow(Context context) {
        mContext = context;
        windowDeploy();
        initialize();
        initData();
    }

    public void setData (MusicData info) {
        musicData = info;
    }

    private void initialize (){
        categoryAdapter = new CategoryAdapter(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.category_popwindow,null);
        mLvCategory = (ListView) view.findViewById(R.id.lv_category);
        categoryAdapter.setDatas(categoryList);
        mLvCategory.setAdapter(categoryAdapter);
        mLvCategory.setOnItemClickListener(this);
        setContentView(view);
    }

    private void initData () {
        List<String> categorys = DbDao.getInstance(mContext).queryCategory();
        if (categoryList != null) {
            categoryList.clear();
        }
        for (int i=0;i<categorys.size();i++) {
            categoryList.add(categorys.get(categorys.size() - i- 1));
        }
        categoryAdapter.setDatas(categoryList);
        categoryAdapter.notifyDataSetChanged();
    }

    private void windowDeploy () {
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        //设置popwindow弹出窗体的宽
        setWidth((int) (width / 2.5));
        //设置popwindow弹出窗体的高
//        setHeight(height / 3);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        //设置popwindow弹出窗体可点击
        setFocusable(true);
        setOutsideTouchable(true);
        //刷新状态
        update();
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable cd = new ColorDrawable(0000000000);
        //点击BACK键和其他地方使其消失，设置了这个才能触发OnDismissListener，设置其他控件变化等操作
        setBackgroundDrawable(cd);
        setAnimationStyle(R.style.CategoryPopWindowStyle);
    }

    public void showPopWindow (View view) {
        Log.e(TAG,"showPopWindow()......");
        initData();
        if (!isShowing()) {
            showAsDropDown(view, view.getLayoutParams().width / 2, 0);
        } else {
            dismiss();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DbDao.getInstance(mContext).addMusic(musicData,categoryList.get(position));
        dismiss();
    }
}
