package com.progimage.client.service;

import retrofit.http.*;
import retrofit.mime.TypedFile;
import rx.Observable;

import java.util.List;

public interface RepositoryUploadService {

    @Multipart
    @POST("/progimage/repository/upload/bulk")
    Observable<List<String>> bulkUpload(@Part("file") TypedFile[] images);

    @Multipart
    @POST("/progimage/repository/upload")
    Observable<String> upload(@Part("file") TypedFile image);
}
