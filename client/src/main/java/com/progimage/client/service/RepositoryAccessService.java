package com.progimage.client.service;

import retrofit.client.Response;
import retrofit.http.*;
import rx.Observable;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface RepositoryAccessService {

    @GET("/progimage/repository/{id}")
    Observable<Response> downloadOriginal(@Path("id") String id);

    @GET("/progimage/repository/{id}.{ext}")
    Observable<Response> downloadInFormat(@Path("id") String id, @Path("{ext}") String format);

    @POST("/progimage/repository/urls")
    Observable<Map<String, URL>> downloadURLs(@Body List<String> ids, @Query(value = "format") String format);
}
