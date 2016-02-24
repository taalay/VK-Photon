package com.khasang.vkphoto.domain.services;


import com.khasang.vkphoto.presentation.model.Photo;
import com.khasang.vkphoto.presentation.model.PhotoAlbum;

import java.util.List;
import java.util.Vector;

/**
 * интерфейс сервиса синхронизации
 */
public interface SyncService {

    void getAllAlbums();

    void getPhotosByAlbumId(int albumId);

    void addPhotos(Vector<String> listUploadedFiles, PhotoAlbum photoAlbum);

    void deleteVkPhotoById(int photoId);

    void deleteVKAlbumById(int albumId);

    /**
     * Регистрирует изменения доступа к альбому
     */
    boolean changeAlbumPrivacy(int i);

    /**
     * Синхронизирует альбом
     */
    void syncAlbums(List<PhotoAlbum> photoAlbumList);

    /**
     * Получает фотографию
     */
    Photo getPhoto();

    /**
     * Создаёт альбом
     */
    PhotoAlbum createAlbum();

}
      