package com.khasang.vkphoto.presentation.presenter.albums;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.khasang.vkphoto.data.local.LocalPhotoSource;
import com.khasang.vkphoto.presentation.model.PhotoAlbum;
import com.khasang.vkphoto.presentation.presenter.Presenter;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;

public interface AlbumsPresenter extends Presenter {

    void checkActionModeFinish(MultiSelector multiSelector);

    void goToPhotoAlbum(Context context, PhotoAlbum photoAlbum);

    File getAlbumThumb(final LocalPhotoSource localPhotoSource, final PhotoAlbum photoAlbum, final ExecutorService executor);

    void selectAlbum(MultiSelector multiSelector, AppCompatActivity activity);

    void exportAlbums(MultiSelector multiSelector);

    void syncAlbums(MultiSelector multiSelector);
}
