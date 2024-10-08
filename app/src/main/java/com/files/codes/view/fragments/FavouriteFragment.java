package com.files.codes.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import com.files.codes.AppConfig;
import com.dreamfilmseu.R;
import com.files.codes.model.Movie;
import com.files.codes.model.VideoContent;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.VideoDetailsActivity;
import com.files.codes.view.fragments.testFolder.GridFragment;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.presenter.VerticalCardPresenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FavouriteFragment extends GridFragment {
    public static final String FAVORITE = "favorite";
    private static final String TAG = "FavouriteFragment";
    private static final int NUM_COLUMNS = 8;
    //private BackgroundHelper bgHelper;
    private LinkedHashMap<String, List<VideoContent>> mVideoLists = null;
    private ArrayObjectAdapter mAdapter;
    private HomeNewActivity activity;
    private List<Movie> movies = new ArrayList<>();
    private boolean dataAvailable;
    private int pageCount = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (HomeNewActivity) getActivity();

        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        setupFragment();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);
        mAdapter = new ArrayObjectAdapter(new VerticalCardPresenter(FAVORITE));
        setAdapter(mAdapter);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fetchFavouriteData();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        activity = (HomeNewActivity) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        activity = (HomeNewActivity) getActivity();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (HomeNewActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (HomeNewActivity) getActivity();
        return super.onCreateView(inflater, container, savedInstanceState);

    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {
                Movie movie = (Movie) o;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra("id", movie.getVideosId());
                if (movie.getIsTvseries().equals("0")) {
                    intent.putExtra("type", "movie");
                } else {
                    intent.putExtra("type", "tvseries");
                }
                intent.putExtra("thumbImage", movie.getThumbnailUrl());
                startActivity(intent);
            }
        };
    }

    // selected listener for setting blur background each time when the item will select.
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // pagination
                if (dataAvailable) {
                    int itemPos = mAdapter.indexOf(item);
                    if (itemPos == movies.size() - 1) {
                        pageCount++;
                        fetchFavouriteData();
                    }
                }
                if (item instanceof Movie) {
                    /*bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((Movie) item).getThumbnailUrl());*/

                }

            }
        };
    }

    public void fetchFavouriteData() {
        if (!new NetworkInst(activity).isNetworkAvailable()) {
            Intent intent = new Intent(activity, ErrorActivity.class);
            startActivity(intent);
            activity.finish();
            return;
        }
        if (!PreferenceUtils.isLoggedIn(getContext())) {
            Toast.makeText(getContext(), getResources().getString(R.string.login_first), Toast.LENGTH_SHORT).show();

            return;
        }
        String userId = PreferenceUtils.getUserId(getContext());

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<List<Movie>> call = api.getFavoriteList(AppConfig.API_KEY, userId, pageCount);
        Log.e(TAG, "fetchFavouriteData: user Id" + userId);
        call.enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                Log.e(TAG, "onResponse: favourite: " + response.message());
                if (response.code() == 200) {
                    List<Movie> movieList = response.body();
                    if (movieList.size() == 0) {
                        dataAvailable = false;
                        if (activity != null)
                            //Toast.makeText(getContext(), getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();

                            return;
                    }
                    for (Movie movie : movieList) {
                        mAdapter.add(movie);
                    }

                    mAdapter.notifyArrayItemRangeChanged(movieList.size() - 1, movieList.size() + movies.size());
                    movies.addAll(movieList);
                    //setAdapter(mAdapter);

                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getLocalizedMessage());
                t.printStackTrace();
                // hide the spinner
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }
}
