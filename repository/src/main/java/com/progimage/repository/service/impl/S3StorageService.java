package com.progimage.repository.service.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.progimage.repository.service.StorageService;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class S3StorageService implements StorageService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Autowired
    private TransferManager transferManager;

    @Autowired
    private AmazonS3 amazonS3;

    private final static Logger LOGGER = LoggerFactory.getLogger(S3StorageService.class);

    @Override
    public Observable<String> store(String key, String format, InputStream data, long length) {
        return Observable.just(null).
                subscribeOn(Schedulers.io()).
                map(ignored -> uploadToS3(key, format, data, length)).
                map(result -> key);
    }

    @Override
    public Observable<InputStream> retrieve(String key) {
        return Observable.just(key).
                subscribeOn(Schedulers.io()).
                map(ignored -> retrieveFromS3(key).getObjectContent());
    }

    @Override
    public URL resolveURL(String key) {
        return amazonS3.generatePresignedUrl(bucketName, key,
                Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), HttpMethod.GET);
    }

    private S3Object retrieveFromS3(String id) {
        return transferManager.getAmazonS3Client().
                getObject(bucketName, id);
    }

    private UploadResult uploadToS3(String key, String format, InputStream data, long length) {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType("image/" + format);
            objectMetadata.setContentLength(length);
            objectMetadata.setContentDisposition(String.format("inline; filename=\"%s.%s\"", key, format));
            return transferManager.upload(bucketName, key, data, objectMetadata).
                    waitForUploadResult();
        } catch (Exception exc) {
            LOGGER.error("Unable to upload file to S3", exc);
            throw Throwables.propagate(exc);
        }
    }
}
