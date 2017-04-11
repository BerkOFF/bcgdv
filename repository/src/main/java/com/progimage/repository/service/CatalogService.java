package com.progimage.repository.service;

import com.progimage.repository.model.ImageMetadata;

public interface CatalogService {
    String create(ImageMetadata imageMetadata);

    void update(ImageMetadata imageMetadata);

    ImageMetadata read(String id);

    void delete(ImageMetadata imageMetadata);
}
