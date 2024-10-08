package com.files.codes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import com.dreamfilmseu.R;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.files.codes.model.VideoContent;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private static int CARD_WIDTH = 185;
    private static int CARD_HEIGHT = 278;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d("onCreateViewHolder", "creating viewholder");
        mContext = parent.getContext();
        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.requestLayout();
        cardView.setInfoVisibility(View.GONE);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        VideoContent video = (VideoContent) item;
        ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        ((ViewHolder) viewHolder).updateCardViewImage(video.getThumbnailUrl());

    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

    }


    class ViewHolder extends Presenter.ViewHolder {

        private ImageCardView mCardView;
        private Drawable mDefaultCardImage;
        private PicassoImageCardViewTarget mImageCardViewTarget;

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
            mImageCardViewTarget = new PicassoImageCardViewTarget(mCardView);
            mDefaultCardImage = mContext
                    .getResources()
                    .getDrawable(R.drawable.logo);
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        protected void updateCardViewImage(String url) {

            Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.poster_placeholder)
                    .error(mDefaultCardImage)
                    .into(mCardView.getMainImageView());

        }
    }


    static class PicassoImageCardViewTarget implements Target {


        private ImageCardView mImageCardView;

        public PicassoImageCardViewTarget(ImageCardView mImageCardView) {
            this.mImageCardView = mImageCardView;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Drawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            mImageCardView.setMainImage(bitmapDrawable);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            mImageCardView.setMainImage(errorDrawable);
        }


        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }

}