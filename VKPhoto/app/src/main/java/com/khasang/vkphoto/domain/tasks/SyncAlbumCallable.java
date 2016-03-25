package com.khasang.vkphoto.domain.tasks;

import android.support.annotation.NonNull;

import com.khasang.vkphoto.data.RequestMaker;
import com.khasang.vkphoto.data.local.LocalDataSource;
import com.khasang.vkphoto.data.local.LocalPhotoSource;
import com.khasang.vkphoto.presentation.model.MyVkRequestListener;
import com.khasang.vkphoto.presentation.model.Photo;
import com.khasang.vkphoto.presentation.model.PhotoAlbum;
import com.khasang.vkphoto.util.Constants;
import com.khasang.vkphoto.util.ErrorUtils;
import com.khasang.vkphoto.util.JsonUtils;
import com.khasang.vkphoto.util.Logger;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SyncAlbumCallable implements Callable<Boolean> {
    public static final int ATTEMPTS_COUNT = 3;
    List<Photo> vkPhotoList;
    private LocalDataSource localDataSource;
    private PhotoAlbum photoAlbum;
    private boolean success = false;

    public SyncAlbumCallable(PhotoAlbum photoAlbum, LocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
        this.photoAlbum = photoAlbum;
    }

    @Override
    public Boolean call() throws Exception {
        Logger.d("Entered SyncAlbumCallable " + photoAlbum.title + " call");
        final LocalPhotoSource localPhotoSource = localDataSource.getPhotoSource();
        photoAlbum.syncStatus = Constants.SYNC_FAILED;
        VKRequest vkRequest = getVkRequest();
        vkRequest.executeSyncWithListener(new MyVkRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    vkPhotoList = JsonUtils.getItems(response.json, Photo.class);
                    Logger.d("Got VKPhoto for photoAlbum " + photoAlbum.title);
                } catch (Exception e) {
                    Logger.d(e.toString());
                    sendError(ErrorUtils.JSON_PARSE_FAILED);
                }
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(3);
        removeDownloadedPhotos(vkPhotoList, localPhotoSource);
        List<Future<File>> futureList = new ArrayList<>();
        fillFutureList(vkPhotoList, executor, futureList, localPhotoSource);
        for (int i = 0; i < ATTEMPTS_COUNT; i++) {
            if (downloadPhotos(futureList) && !Thread.currentThread().isInterrupted()) {
                success = true;
                Logger.d("success download");
                photoAlbum.syncStatus = Constants.SYNC_SUCCESS;
                break;
            }
        }
        executor.shutdown();
        localDataSource.getAlbumSource().updateAlbum(photoAlbum, true);
        return success;
    }

    private boolean downloadPhotos(List<Future<File>> futureList) throws InterruptedException, java.util.concurrent.ExecutionException {
        Iterator<Future<File>> iterator = futureList.iterator();
        while (iterator.hasNext() && !Thread.currentThread().isInterrupted()) {
            Future<File> next = iterator.next();
            if (next.get().exists()) {
                iterator.remove();
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            Logger.d("download canceled");
            iterator = futureList.iterator();
            while (iterator.hasNext()) {
                Future<File> next = iterator.next();
                next.cancel(true);
            }
        }
        return futureList.isEmpty();
    }

    private void fillFutureList(List<Photo> vkPhotoList, ExecutorService executor, List<Future<File>> futureList, LocalPhotoSource localPhotoSource) {
        for (int i = 0; i < vkPhotoList.size(); i++) {
            Photo photo = vkPhotoList.get(i);
            DownloadPhotoCallable downloadPhotoCallable = new DownloadPhotoCallable(localPhotoSource, photo, photoAlbum);
            futureList.add(executor.submit(downloadPhotoCallable));
        }
    }

    private void removeDownloadedPhotos(List<Photo> vkPhotoList, LocalPhotoSource localPhotoSource) {
        List<Photo> localPhotoList = localPhotoSource.getSynchronizedPhotosByAlbumId(photoAlbum.id);
        for (int i = 0; i < localPhotoList.size(); i++) {
            Photo photo = localPhotoList.get(i);
            if (new File(photo.filePath).exists()) {
                vkPhotoList.remove(photo);
            }
        }
    }

    @NonNull
    private VKRequest getVkRequest() {
        VKRequest vkRequest = RequestMaker.getVkPhotosByAlbumIdRequest(photoAlbum.id);
        vkRequest.attempts = 5;
        return vkRequest;
    }


}
