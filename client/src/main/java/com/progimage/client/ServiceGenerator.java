package com.progimage.client;

import com.squareup.okhttp.OkHttpClient;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public class ServiceGenerator {
    private static final RestAdapter.Builder builder = new RestAdapter.Builder()
            .setClient(new OkClient(new OkHttpClient()));

    public static synchronized <S> S createService(Class<S> serviceClass, String serviceUrl) {
        RestAdapter adapter = builder.setEndpoint(serviceUrl).build();
        return adapter.create(serviceClass);
    }
}
