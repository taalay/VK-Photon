package com.khasang.vkphoto.presentation.fragments;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.khasang.vkphoto.R;
import com.khasang.vkphoto.data.AlbumsCursorLoader;
import com.khasang.vkphoto.data.local.LocalAlbumSource;
import com.khasang.vkphoto.domain.adapters.PhotoAlbumsCursorAdapter;
import com.khasang.vkphoto.domain.interfaces.FabProvider;
import com.khasang.vkphoto.domain.interfaces.SyncServiceProvider;
import com.khasang.vkphoto.domain.listeners.RecyclerViewOnScrollListener;
import com.khasang.vkphoto.presentation.custom_classes.GridSpacingItemDecoration;
import com.khasang.vkphoto.presentation.model.PhotoAlbum;
import com.khasang.vkphoto.presentation.presenter.albums.VKAlbumsPresenter;
import com.khasang.vkphoto.presentation.presenter.albums.VKAlbumsPresenterImpl;
import com.khasang.vkphoto.presentation.view.AlbumsView;
import com.khasang.vkphoto.util.Constants;
import com.khasang.vkphoto.util.ErrorUtils;
import com.khasang.vkphoto.util.Logger;
import com.khasang.vkphoto.util.ToastUtils;
import com.vk.sdk.api.model.VKPrivacy;

import static com.khasang.vkphoto.util.Constants.ALBUMS_SPAN_COUNT;

public class AlbumsFragment extends Fragment implements AlbumsView, LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = AlbumsFragment.class.getSimpleName();
    public static final String ACTION_MODE_ACTIVE = "action_mode_active";
    private VKAlbumsPresenter vKAlbumsPresenter;
    private PhotoAlbumsCursorAdapter adapter;
    private MultiSelector multiSelector;
    private TextView tvCountOfAlbums;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean refreshing;

    public AlbumsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        multiSelector = new MultiSelector();
        vKAlbumsPresenter = new VKAlbumsPresenterImpl(this, ((SyncServiceProvider) getActivity()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums, container, false);
        getActivity().getSupportLoaderManager().initLoader(0, null, this);
        tvCountOfAlbums = (TextView) view.findViewById(R.id.tv_count_of_albums);
        initSwipeRefreshLayout(view);
        initRecyclerView(view);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(ACTION_MODE_ACTIVE)) {
                vKAlbumsPresenter.selectAlbum(multiSelector, (AppCompatActivity) getActivity());
            }
            if (refreshing) {
                displayRefresh(true);
            }
        }

        return view;
    }

    private void initSwipeRefreshLayout(View view) {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        Resources resources = getResources();
        swipeRefreshLayout.setColorSchemeColors(resources.getColor(R.color.colorPrimary),
                resources.getColor(R.color.colorAccentLight),
                resources.getColor(R.color.colorAccent),
                resources.getColor(R.color.colorAccentDark));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                vKAlbumsPresenter.getAllVKAlbums();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void displayRefresh(final boolean refreshing) {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                Logger.d("Refreshing " + refreshing);
                AlbumsFragment.this.refreshing = refreshing;
                swipeRefreshLayout.setRefreshing(refreshing);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Logger.d("AlbumsFragment onStart()");
        vKAlbumsPresenter.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.d("AlbumsFragment onResume()");
        setOnClickListenerFab();
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.d("AlbumsFragment onStop()");
        vKAlbumsPresenter.onStop();
    }

    //AlbumsView implementations
    @Override
    public void displayVkSaveAlbum(PhotoAlbum photoAlbum) {
        Logger.d("displayVkSaveAlbum");
    }

    @Override
    public void displayAlbums() {
        Logger.d("displayAlbums");
        displayRefresh(false);
        getActivity().getSupportLoaderManager().getLoader(0).forceLoad();
    }


    @Override
    public Cursor getAdapterCursor() {
        return adapter.getCursor();
    }

    @Override
    public void showError(int errorCode) {
        Logger.d(TAG + " error " + errorCode);
        switch (errorCode) {
            case ErrorUtils.NO_INTERNET_CONNECTION_ERROR:
                displayRefresh(false);
                ToastUtils.showError(ErrorUtils.getErrorMessage(errorCode, getContext()), getContext());
                break;
        }
    }

    @Override
    public void confirmDelete(final MultiSelector multiSelector) {
        new MaterialDialog.Builder(getContext())
                .content(R.string.sync_delete_album_question)
                .positiveText(R.string.delete)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        vKAlbumsPresenter.deleteSelectedAlbums(multiSelector);
                    }
                })
                .show();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AlbumsCursorLoader(getContext(), new LocalAlbumSource(getContext()));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!initAdapter(data)) {
            adapter.changeCursor(data);
        }
        int itemCount = adapter.getItemCount();
        tvCountOfAlbums.setText(getResources().getQuantityString(R.plurals.count_of_albums, itemCount, itemCount));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ACTION_MODE_ACTIVE, multiSelector.isSelectable());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.changeCursor(null);
    }

    private boolean initAdapter(Cursor cursor) {
        if (adapter == null) {
            adapter = new PhotoAlbumsCursorAdapter(getContext(), cursor, multiSelector, vKAlbumsPresenter);
            return true;
        }
        return false;
    }

    private void setOnClickListenerFab() {
        ((FabProvider) getActivity()).getFloatingActionButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.d("AlbumsFragment add album");
//                vKAlbumsPresenter.addAlbum();
                new MaterialDialog.Builder(getContext())
                        .title(R.string.create_album)
                        .customView(R.layout.fragment_vk_add_album, true)
                        .positiveText(R.string.create)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                View dialogView = dialog.getView();
                                vKAlbumsPresenter.addAlbum(((EditText) dialogView.findViewById(R.id.et_album_title)).getText().toString(),
                                        ((EditText) dialogView.findViewById(R.id.et_album_description)).getText().toString(),
                                        VKPrivacy.PRIVACY_ALL,
                                        VKPrivacy.PRIVACY_ALL);
                            }
                        })
                        .show();
            }
        });
    }

    private void initRecyclerView(View view) {
        RecyclerView albumsRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            albumsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        } else {
            albumsRecyclerView.setHasFixedSize(true);
            albumsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), ALBUMS_SPAN_COUNT, LinearLayoutManager.VERTICAL, false));
            albumsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(ALBUMS_SPAN_COUNT, Constants.RECYCLERVIEW_SPACING, false));
        }
        initAdapter(null);
        albumsRecyclerView.setAdapter(adapter);
        albumsRecyclerView.addOnScrollListener(new RecyclerViewOnScrollListener(((FabProvider) getActivity()).getFloatingActionButton()));
    }
}
