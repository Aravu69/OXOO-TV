package com.files.codes.view.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.dreamfilmseu.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.FavoriteModel;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.movieDetails.RelatedMovie;
import com.files.codes.model.movieDetails.Season;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.LoginAlertDialog;
import com.files.codes.utils.PaidDialog;
import com.files.codes.utils.PaletteColors;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.Utils;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.files.codes.view.adapter.ServerAdapter;
import com.files.codes.view.presenter.ActionButtonPresenter;
import com.files.codes.view.presenter.CustomMovieDetailsPresenter;
import com.files.codes.view.presenter.EpisodPresenter;
import com.files.codes.view.presenter.MovieDetailsDescriptionPresenter;
import com.files.codes.view.presenter.RelatedPresenter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class VideoDetailsFragment extends DetailsSupportFragment implements Palette.PaletteAsyncListener{
    private final String TAG = "VideoDetailsFragment";
    public static String TRANSITION_NAME = "poster_transition";
    private FullWidthDetailsOverviewRowPresenter mFullWidthMovieDetailsPresenter;
    private ArrayObjectAdapter mAdapter;

    private DetailsOverviewRow mDetailsOverviewRow;
    public static MovieSingleDetails movieDetails = null;
    private Context mContext;
    private String type;
    private String id;
    private String thumbUrl;
    private BackgroundHelper bgHelper;

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_WATCH_LATER = 2;
    private boolean favStatus;
    private String userId = "";
    private String isPaid = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        type = getActivity().getIntent().getStringExtra("type");
        id = getActivity().getIntent().getStringExtra("id");
        thumbUrl = getActivity().getIntent().getStringExtra("thumbImage");
        //get userId from DB
        //check mandatory login is enabled or not
        //and also check user logged in or not
        if (PreferenceUtils.isLoggedIn(getContext())) {
            this.userId = PreferenceUtils.getUserId(getContext());
            PreferenceUtils.updateSubscriptionStatus(getContext());
        }else {
            //set a static user id to avoid app crashing
            this.userId = "1";
        }

        bgHelper = new BackgroundHelper(getActivity());
        bgHelper.prepareBackgroundManager();
        bgHelper.updateBackground(thumbUrl);

        setUpAdapter();
        setUpDetailsOverviewRow();
    }


    private void setUpAdapter() {
        // Create the FullWidthPresenter
        mFullWidthMovieDetailsPresenter = new CustomMovieDetailsPresenter(new MovieDetailsDescriptionPresenter(),
                new DetailsOverviewLogoPresenter());

        //mFullWidthMovieDetailsPresenter.setActionsBackgroundColor(ContextCompat.getColor(getActivity(), R.color.black_30));
        mFullWidthMovieDetailsPresenter.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.black_50));

        // Handle the transition, the Helper is mainly used because the ActivityTransition is being passed from
        // The Activity into the Fragment
        FullWidthDetailsOverviewSharedElementHelper helper = new FullWidthDetailsOverviewSharedElementHelper();
        helper.setSharedElementEnterTransition(getActivity(), TRANSITION_NAME); // the transition name is important
        mFullWidthMovieDetailsPresenter.setListener(helper); // Attach the listener
        // Define if this element is participating in the transition or not
        mFullWidthMovieDetailsPresenter.setParticipatingEntranceTransition(false);

        // Class presenter selector allows the Adapter to render Rows and the details
        // It can be used in any of the Adapters by the Leanback library
        ClassPresenterSelector classPresenterSelector = new ClassPresenterSelector();
        classPresenterSelector.addClassPresenter(DetailsOverviewRow.class, mFullWidthMovieDetailsPresenter);
        classPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(classPresenterSelector);

        // Sets the adapter to the fragment
        setAdapter(mAdapter);


    }

    private void setUpDetailsOverviewRow() {
        mDetailsOverviewRow = new DetailsOverviewRow(new MovieSingleDetails());
        mAdapter.add(mDetailsOverviewRow);
        loadImage(thumbUrl);

        if (type.equals("movie")) {
            // fetch movie details
            getData(type, id);
            getFavStatus();

        } else if (type.equals("tvseries")) {
            // fetch tv series details
            getTvSeries(type, id);
            getFavStatus();
        }
    }

    public void setActionAdapter(boolean favAdded) {
        if (type.equals("movie")) {
            setMovieActionAdapter(favAdded);
        } else if (type.equals("tvseries")) {
            setTvSeriesActionAdapter(favAdded);
        }

    }

    public void setMovieActionAdapter(final boolean favAdded) {
        final SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(new ActionButtonPresenter());
        //set play button text
        //if user has subscription, button text will be "play now"
        DatabaseHelper db = new DatabaseHelper(getContext());
        final String status = db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
        if (isPaid.equals("1")) {
            if (status.equals("active")) {
                adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.play_now)));
            } else {
                adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.go_premium)));
            }
        } else {
            adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.play_now)));
        }

        if (favAdded) {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.remove_from_fav)));
        } else {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.add_to_fav)));
        }

        mDetailsOverviewRow.setActionsAdapter(adapter);


        mFullWidthMovieDetailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == 1) {
                    PreferenceUtils.clearSubscriptionSavedData(getActivity());
                    if (movieDetails != null) {
                        if (movieDetails.getIsPaid().equals("1")) {
                            //check user is logged in or not
                            if (PreferenceUtils.isLoggedIn(getActivity())) {
                                if (status.equals("active")) {
                                    openServerDialog(movieDetails.getVideos());
                                } else {
                                    //subscription is not active
                                    //new PaidDialog(getActivity()).showPaidContentAlertDialog();
                                    PaidDialog dialog = new PaidDialog(getContext());
                                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                                    dialog.show();
                                }
                            }else {
                                // user is not logged in
                                // show an alert dialog
                                LoginAlertDialog dialog = new LoginAlertDialog(getActivity());
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                                dialog.show();
                            }
                        } else {
                            openServerDialog(movieDetails.getVideos());
                        }
                    }
                } else {

                    if (favStatus) {
                        // remove from fav
                        removeFromFav();
                    } else {
                        // add to fav
                        addToFav();
                    }

                }
            }
        });
    }

    public void setTvSeriesActionAdapter(boolean favAdded) {
        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(new ActionButtonPresenter());
        if (favAdded) {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.remove_from_fav)));
        } else {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.add_to_fav)));
        }

        mDetailsOverviewRow.setActionsAdapter(adapter);

        mFullWidthMovieDetailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (favStatus) {
                    // remove from fav
                    removeFromFav();
                } else {
                    // add to fav
                    addToFav();
                }
            }
        });
    }

    private void loadImage(String url) {
        /*mDetailsOverviewRow.setImageDrawable(getContext().getResources().
                getDrawable(R.drawable.logo));*/
        Picasso.get()
                .load(url)
                .resize(300, 500)
                .centerCrop()
                .placeholder(R.drawable.poster_placeholder)
                .into(new PicassoImageCardViewTarget());

    }

    private void bindMovieDetails(MovieSingleDetails singleDetails) {
        movieDetails = singleDetails;
        // Bind the details to the row
        mDetailsOverviewRow.setItem(movieDetails);
        loadImage(thumbUrl);
    }

    private void changePalette(Bitmap bmp) {
        Palette.from(bmp).generate(this);
    }

    @Override
    public void onGenerated(Palette palette) {
        PaletteColors colors = Utils.getPaletteColors(palette);
        mFullWidthMovieDetailsPresenter.setActionsBackgroundColor(colors.getStatusBarColor());
        mFullWidthMovieDetailsPresenter.setBackgroundColor(colors.getToolbarBackgroundColor());
    }

    class PicassoImageCardViewTarget implements Target {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mDetailsOverviewRow.setImageBitmap(getContext(), bitmap);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }


        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }

    public static int dpToPx(int dp, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public void openServerDialog(final List<Video> videos) {
        if (videos.size() != 0) {
            List<Video> videoList = new ArrayList<>();
            videoList.clear();

            for (Video video : videos) {
                if (!video.getFileType().equalsIgnoreCase("embed")) {
                    videoList.add(video);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_server_tv, null);
            RecyclerView serverRv = view.findViewById(R.id.serverRv);
            ServerAdapter serverAdapter = new ServerAdapter(getActivity(), videoList, "movie");
            serverRv.setLayoutManager(new LinearLayoutManager(getActivity()));
            serverRv.setHasFixedSize(true);
            serverRv.setAdapter(serverAdapter);

            Button closeBt = view.findViewById(R.id.close_bt);

            builder.setView(view);

            final AlertDialog dialog = builder.create();
            dialog.show();

            closeBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            final ServerAdapter.OriginalViewHolder[] viewHolder = {null};
            serverAdapter.setOnItemClickListener(new ServerAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, Video obj, int position, ServerAdapter.OriginalViewHolder holder) {
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                            .toBundle();

                    PlaybackModel video = new PlaybackModel();
                    video.setId(Long.parseLong(id));
                    video.setTitle(movieDetails.getTitle());
                    video.setDescription(movieDetails.getDescription());
                    video.setCategory("movie");
                    video.setVideo(obj);
                    ArrayList<Video> videoListForIntent = new ArrayList<>(videoList);
                    video.setVideoList(videoListForIntent);
                    video.setVideoUrl(obj.getFileUrl());
                    video.setVideoType(obj.getFileType());
                    video.setBgImageUrl(movieDetails.getPosterUrl());
                    video.setCardImageUrl(movieDetails.getThumbnailUrl());
                    video.setIsPaid(movieDetails.getIsPaid());

                    Intent playerIntent = new Intent(getActivity(), PlayerActivity.class);
                    playerIntent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, video);
                    startActivity(playerIntent);
                    dialog.dismiss();
                }
            });
        }else {
            new ToastMsg(getContext()).toastIconError("No video available.");
        }

    }

    private void getData(String vtype, final String vId) {

        final SpinnerFragment spinnerFragment = new SpinnerFragment();
        final FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(R.id.details_fragment, spinnerFragment).commit();

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<MovieSingleDetails> call = api.getSingleDetail(AppConfig.API_KEY, vtype, vId);
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (response.code() == 200) {
                    MovieSingleDetails singleDetails = new MovieSingleDetails();
                    singleDetails = response.body();
                    singleDetails.setType("movie");
                    isPaid = response.body().getIsPaid();
                    setMovieActionAdapter(favStatus);
                    bindMovieDetails(response.body());
                    Log.e("VideoDetails", "onResponse: " + singleDetails.getIsPaid() );

                    //new DetailRowBuilderTask().execute(singleDetails);
                    String[] subcategories = {
                            "You may also like"
                    };

                    ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new RelatedPresenter(getActivity()));
                    for (RelatedMovie model : response.body().getRelatedMovie()) {
                        model.setType("movie");
                        rowAdapter.add(model);
                    }
                    HeaderItem header = new HeaderItem(0, subcategories[0]);
                    mAdapter.add(new ListRow(header, rowAdapter));

                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                } else {
                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                }
            }

            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
            }
        });

    }

    private void getTvSeries(String vtype, final String vId) {

        final SpinnerFragment spinnerFragment = new SpinnerFragment();
        final FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(R.id.details_fragment, spinnerFragment).commit();

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<MovieSingleDetails> call = api.getSingleDetail(AppConfig.API_KEY, vtype, vId);
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (response.code() == 200) {
                    MovieSingleDetails singleDetails = new MovieSingleDetails();
                    singleDetails = response.body();
                    singleDetails.setType("tvseries");
                    isPaid = response.body().getIsPaid();
                    setTvSeriesActionAdapter(favStatus);
                    bindMovieDetails(response.body());

                    String[] subcategories = {
                            "You may also like"
                    };

                    List<Season> seasons = new ArrayList<Season>();
                    seasons.addAll(response.body().getSeason());

                    if (seasons.size() == 0) {
                        Toast.makeText(mContext, "Seasons are not found. :(", Toast.LENGTH_SHORT).show();
                    }

                    for (int i = 0; i <= seasons.size(); i++) { // <= for related content

                        if (i == seasons.size()) {
                            // set related content
                            ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new RelatedPresenter(getActivity()));
                            for (RelatedMovie model : response.body().getRelatedTvseries()) {
                                model.setType("tvseries");
                                rowAdapter.add(model);
                            }
                            HeaderItem header = new HeaderItem(i, subcategories[0]);
                            mAdapter.add(new ListRow(header, rowAdapter));
                        } else {
                            // set season content
                            Season season = seasons.get(i);
                            ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new EpisodPresenter());
                            for (Episode episode : season.getEpisodes()) {
                                episode.setIsPaid(isPaid);
                                episode.setSeasonName(season.getSeasonsName());
                                episode.setTvSeriesTitle(singleDetails.getTitle());
                                episode.setCardBackgroundUrl(singleDetails.getPosterUrl());

                                rowAdapter.add(episode);
                            }
                            HeaderItem header = new HeaderItem(i, "Season: " + season.getSeasonsName());
                            mAdapter.add(new ListRow(header, rowAdapter));

                        }

                    }

                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                } else {
                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                }
            }

            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
            }
        });

    }

    private void addToFav() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<FavoriteModel> call = api.addToFavorite(AppConfig.API_KEY, userId, id);
        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        favStatus = true;
                        new ToastMsg(getActivity()).toastIconSuccess(response.body().getMessage());
                        setActionAdapter(favStatus);
                    } else {
                        favStatus = false;
                        new ToastMsg(getActivity()).toastIconError(getString(R.string.you_are_not_logged_in));
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                new ToastMsg(getActivity()).toastIconError(getString(R.string.error_toast));
            }
        });

    }

    private void getFavStatus() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<FavoriteModel> call = api.verifyFavoriteList(AppConfig.API_KEY, userId, id);
        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        favStatus = true;
                        setActionAdapter(favStatus);
                    } else {
                        favStatus = false;
                        setActionAdapter(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                new ToastMsg(getActivity()).toastIconError(getString(R.string.fetch_error));
            }
        });
    }

    private void removeFromFav() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<FavoriteModel> call = api.removeFromFavorite(AppConfig.API_KEY, userId, id);
        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        favStatus = false;
                        new ToastMsg(getActivity()).toastIconSuccess(response.body().getMessage());
                        setActionAdapter(favStatus);
                    } else {
                        favStatus = true;
                        new ToastMsg(getActivity()).toastIconError(response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                new ToastMsg(getActivity()).toastIconError(getString(R.string.fetch_error));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        movieDetails = null;
    }
}
