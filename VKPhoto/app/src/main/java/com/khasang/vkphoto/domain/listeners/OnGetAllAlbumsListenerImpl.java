package com.khasang.vkphoto.domain.listeners;

import com.khasang.vkphoto.model.album.PhotoAlbum;
import com.khasang.vkphoto.ui.view.MainView;
import com.khasang.vkphoto.util.Logger;
import com.vk.sdk.api.VKError;

import java.util.List;

public class OnGetAllAlbumsListenerImpl implements OnGetAllAlbumsListener {
    private MainView mainView;

    public OnGetAllAlbumsListenerImpl(MainView mainView) {
        this.mainView = mainView;
    }

    @Override
    public void onSuccess(List<PhotoAlbum> photoAlbums) {
        mainView.displayVkAlbums(photoAlbums);
    }

    @Override
    public void onVKError(VKError e) {
        Logger.d(e.errorMessage);
        mainView.showConnectionError();
    }

    @Override
    public void onSyncServiceError() {
        mainView.showSyncServiceError();
    }
}