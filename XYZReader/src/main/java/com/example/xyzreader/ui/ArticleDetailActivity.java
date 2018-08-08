package com.example.xyzreader.ui;

import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, MyPagerAdapter.PagerInterface {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    private Toolbar toolbar;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
//        getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE );
        setContentView( R.layout.activity_article_detail );

        toolbar = findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setDisplayHomeAsUpEnabled( true );
            actionBar.setDisplayShowHomeEnabled( true );
            actionBar.setTitle( " " );
        }

        getSupportLoaderManager().initLoader( 0, null, this );

        mPagerAdapter = new MyPagerAdapter( getSupportFragmentManager(), this );
        mPager = findViewById( R.id.pager );
        mPager.setAdapter( mPagerAdapter );
        mPager.setPageMargin( (int) TypedValue
                .applyDimension( TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics() ) );
        mPager.setPageMarginDrawable( new ColorDrawable( 0x22000000 ) );

        mPager.addOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged( int state ) {
                super.onPageScrollStateChanged( state );
                toolbar.animate()
                        .alpha( ( state == ViewPager.SCROLL_STATE_IDLE ) ? 1f : 0f )
                        .setDuration( 300 );
            }

            @Override
            public void onPageSelected( int position ) {
                if ( mCursor != null ) {
                    mCursor.moveToPosition( position );
                }
                mSelectedItemId = mCursor.getLong( ArticleLoader.Query._ID );
                updateUpButtonPosition();
            }
        } );

        if ( savedInstanceState == null ) {
            if ( getIntent() != null && getIntent().getData() != null ) {
                mStartId = ItemsContract.Items.getItemId( getIntent().getData() );
                mSelectedItemId = mStartId;
            }
        }
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader( int i, Bundle bundle ) {
        return ArticleLoader.newAllArticlesInstance( this );
    }

    @Override
    public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor ) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if ( mStartId > 0 ) {
            mCursor.moveToFirst();
            // TODO: optimize
            while ( !mCursor.isAfterLast() ) {
                if ( mCursor.getLong( ArticleLoader.Query._ID ) == mStartId ) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem( position, false );
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset( Loader<Cursor> cursorLoader ) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged( long itemId, ArticleDetailFragment fragment ) {
        if ( itemId == mSelectedItemId ) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + toolbar.getHeight();
        toolbar.setTranslationY( Math.min( mSelectedItemUpButtonFloor - upButtonNormalBottom, 0 ) );
    }

    @Override
    public void moveToPosition( int position ) {
        mCursor.moveToPosition( position );

    }

    @Override
    public int getCursorSize() {
        return ( mCursor != null ) ? mCursor.getCount() : 0;
    }

    @Override
    public void updateUpButtonPosition( int selectedItemUpButtonFloor ) {
        this.mSelectedItemUpButtonFloor = selectedItemUpButtonFloor;
        updateUpButtonPosition();

    }

    @Override
    public long getItemId() {
        return mCursor.getLong( ArticleLoader.Query._ID );
    }
}
