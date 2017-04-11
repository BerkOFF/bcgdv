package com.progimage.repository.service;
import rx.Observable;

import java.io.InputStream;
import java.net.URL;

public interface StorageService {
    Observable<String> store(String key, String format, InputStream data, long length);
    Observable<InputStream> retrieve(String key);
    URL resolveURL(String key);
}
