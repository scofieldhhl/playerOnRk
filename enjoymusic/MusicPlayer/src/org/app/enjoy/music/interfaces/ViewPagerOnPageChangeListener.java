package org.app.enjoy.music.interfaces;

import android.support.v4.view.ViewPager;

import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;


/**
 * Created by victor on 2016/1/20.
 */
public class ViewPagerOnPageChangeListener implements ViewPager.OnPageChangeListener{

    private String TAG = "ViewPagerOnPageChangeListener";
    private int currentFrag = Contsant.Frag.MUSIC_LIST_FRAG;

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                currentFrag = Contsant.Frag.MUSIC_LIST_FRAG;
                break;
            case 1:
                currentFrag = Contsant.Frag.ARTIST_FRAG;
                break;
            case 2:
                currentFrag =  Contsant.Frag.ALBUM_FRAG;
                break;
            case 3:
                currentFrag =  Contsant.Frag.DIY_FRAG;
                break;
            case 4:
                currentFrag =  Contsant.Frag.SEARCH_MUSIC_FRAG;
                break;
            default:
                currentFrag = Contsant.Frag.MUSIC_LIST_FRAG;
                break;
        }
        DataObservable.getInstance().setData(currentFrag);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
