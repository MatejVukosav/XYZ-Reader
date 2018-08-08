package com.example.xyzreader.ui;

import android.view.ViewGroup;

/**
 * Created by mvukosav
 */
public class MyPagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter {

    public interface PagerInterface {
        void moveToPosition( int position );

        int getCursorSize();

        void updateUpButtonPosition( int selectedItemUpButtonFloor );

        long getItemId();
    }

    private PagerInterface pagerInterface;

    public MyPagerAdapter( android.support.v4.app.FragmentManager fm, PagerInterface pagerInterface ) {
        super( fm );
        this.pagerInterface = pagerInterface;
    }

    @Override
    public void setPrimaryItem( ViewGroup container, int position, Object object ) {
        super.setPrimaryItem( container, position, object );
        ArticleDetailFragment fragment = (ArticleDetailFragment) object;
        if ( fragment != null ) {
            pagerInterface.updateUpButtonPosition( fragment.getUpButtonFloor() );
        }
    }

    @Override
    public android.support.v4.app.Fragment getItem( int position ) {
        pagerInterface.moveToPosition( position );
        return ArticleDetailFragment.newInstance( pagerInterface.getItemId() );
    }

    @Override
    public int getCount() {
        return pagerInterface.getCursorSize();
    }
}