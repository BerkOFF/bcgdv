package com.progimage.client.service;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import retrofit.mime.TypedByteArray;
import rx.Observable;

import java.net.URL;

public interface ConversionService {

    @POST("/progimage/conversion")
    Observable<Response> convertImagePayload(@Body TypedByteArray payload, @Query("format") String format);

    @GET("/progimage/conversion")
    Observable<Response> convertImageFromUrl(@Query("url") URL url, @Query("format") String format);
}
