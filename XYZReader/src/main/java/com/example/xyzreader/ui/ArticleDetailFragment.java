package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.databinding.FragmentArticleDetailBinding;
import com.example.xyzreader.databinding.ItemMetaBarBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends android.support.v4.app.Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    private FragmentArticleDetailBinding binding;
    private ItemMetaBarBinding itemMetaBarBinding;

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private int mMutedColor = 0xFF333333;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault() );
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar( 2, 1, 1 );

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance( long itemId ) {
        Bundle arguments = new Bundle();
        arguments.putLong( ARG_ITEM_ID, itemId );
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments( arguments );
        return fragment;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        if ( getArguments().containsKey( ARG_ITEM_ID ) ) {
            mItemId = getArguments().getLong( ARG_ITEM_ID );
        }

        mIsCard = getResources().getBoolean( R.bool.detail_is_card );
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin );
        setHasOptionsMenu( true );
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader( 0, null, this );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        binding = DataBindingUtil.inflate( inflater, R.layout.fragment_article_detail, container, false );

        itemMetaBarBinding = binding.itemMetaBar;
        binding.drawInsetsFrameLayout.setOnInsetsCallback( new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged( Rect insets ) {
                mTopInset = insets.top;
            }
        } );

        binding.scrollview.setCallbacks( new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = binding.scrollview.getScrollY();
                getActivityCast().onUpButtonFloorChanged( mItemId, ArticleDetailFragment.this );
                binding.photoContainer.setTranslationY( (int) ( mScrollY - mScrollY / PARALLAX_FACTOR ) );
                updateStatusBar();
            }
        } );

        mStatusBarColorDrawable = new ColorDrawable( 0 );

        binding.shareFab.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                startActivity( Intent.createChooser( ShareCompat.IntentBuilder.from( getActivity() )
                        .setType( "text/plain" )
                        .setText( "Some sample text" )
                        .getIntent(), getString( R.string.action_share ) ) );
            }
        } );

        return binding.getRoot();
    }

    private void updateStatusBar() {
        int color = 0;
        if ( mTopInset != 0 && mScrollY > 0 ) {
            float f = progress( mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset );
            color = Color.argb( (int) ( 255 * f ),
                    (int) ( Color.red( mMutedColor ) * 0.9 ),
                    (int) ( Color.green( mMutedColor ) * 0.9 ),
                    (int) ( Color.blue( mMutedColor ) * 0.9 ) );
        }
        mStatusBarColorDrawable.setColor( color );
        binding.drawInsetsFrameLayout.setInsetBackground( mStatusBarColorDrawable );
    }

    static float progress( float v, float min, float max ) {
        return constrain( ( v - min ) / ( max - min ), 0, 1 );
    }

    static float constrain( float val, float min, float max ) {
        if ( val < min ) {
            return min;
        } else if ( val > max ) {
            return max;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString( ArticleLoader.Query.PUBLISHED_DATE );
            return dateFormat.parse( date );
        } catch ( ParseException ex ) {
            Log.e( TAG, ex.getMessage() );
            Log.i( TAG, "passing today's date" );
            return new Date();
        }
    }

    private void bindViews() {
        if ( binding == null || mCursor == null ) {
            return;
        }

        itemMetaBarBinding.articleByline.setMovementMethod( new LinkMovementMethod() );
        binding.articleBody.setTypeface( Typeface.createFromAsset( getResources().getAssets(), "Rosario-Regular.ttf" ) );

        itemMetaBarBinding.articleTitle.setText( mCursor.getString( ArticleLoader.Query.TITLE ) );
        Date publishedDate = parsePublishedDate();

        if ( !publishedDate.before( START_OF_EPOCH.getTime() ) ) {
            itemMetaBarBinding.articleByline.setText( Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL ).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString( ArticleLoader.Query.AUTHOR )
                            + "</font>" ) );

        } else {
            // If date is before 1902, just show the string
            itemMetaBarBinding.articleByline.setText( Html.fromHtml(
                    outputFormat.format( publishedDate ) + " by <font color='#ffffff'>"
                            + mCursor.getString( ArticleLoader.Query.AUTHOR )
                            + "</font>" ) );

        }

        binding.articleBody.setText( Html.fromHtml( mCursor.getString( ArticleLoader.Query.BODY ).replaceAll( "(\r\n|\n)", "<br />" ) ) );

        Glide.with( this )
                .asBitmap()
                .load( Uri.parse( mCursor.getString( ArticleLoader.Query.PHOTO_URL ) ) )
                .listener( new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed( @Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource ) {
                        Log.d( TAG, "Failed to load glide image" );
                        return false;
                    }

                    @Override
                    public boolean onResourceReady( Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource ) {
                        changeScreenColors( resource );
                        return false;
                    }
                } )
                .into( binding.photo );
    }

    @Override
    public Loader<Cursor> onCreateLoader( int i, Bundle bundle ) {
        return ArticleLoader.newInstanceForItemId( getActivity(), mItemId );
    }

    @Override
    public void onLoadFinished( @NonNull Loader<Cursor> loader, Cursor cursor ) {
        if ( !isAdded() ) {
            if ( cursor != null ) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if ( mCursor != null && !mCursor.moveToFirst() ) {
            Log.e( TAG, "Error reading item detail cursor" );
            mCursor.close();
            mCursor = null;
            return;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset( @NonNull android.support.v4.content.Loader<Cursor> loader ) {
    }

    public int getUpButtonFloor() {
        if ( binding == null || binding.photo.getHeight() == 0 ) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) binding.photoContainer.getTranslationY() + binding.photo.getHeight() - mScrollY
                : binding.photo.getHeight() - mScrollY;
    }

    private void changeScreenColors( Bitmap bitmap ) {
        new Palette.Builder( bitmap ).generate( new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated( @NonNull Palette palette ) {
                mMutedColor = palette.getDarkMutedColor( 0xFF333333 );
                itemMetaBarBinding.metaBar.setBackgroundColor( mMutedColor );
                updateStatusBar();
            }
        } );
    }
}
