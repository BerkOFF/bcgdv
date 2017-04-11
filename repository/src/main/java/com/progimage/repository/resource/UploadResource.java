package com.progimage.repository.resource;

import com.progimage.repository.model.ImageMetadata;
import com.progimage.repository.service.CatalogService;
import com.progimage.repository.service.StorageService;
import com.progimage.repository.validators.ValidImage;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.List;

import static org.springframework.util.MimeTypeUtils.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/progimage/repository/upload")
@Validated
public class UploadResource {

    @Autowired
    private StorageService storageService;

    @Autowired
    private CatalogService catalogService;

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadResource.class);

    @RequestMapping(value = "/bulk", method = RequestMethod.POST, consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DeferredResult<List<String>> batchUpload(@RequestParam("file") @ValidImage MultipartFile[] multipartFiles) {
        DeferredResult<List<String>> result = new DeferredResult<>();
        Observable.from(multipartFiles).
                subscribeOn(Schedulers.io()).
                flatMap(this::store).toList().subscribe(result::setResult, result::setErrorResult);

        return result;
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DeferredResult<String> upload(@RequestParam("file") @ValidImage MultipartFile multipartFile) {
        DeferredResult<String> result = new DeferredResult<>();
        Observable.just(multipartFile).
                subscribeOn(Schedulers.io()).
                flatMap(this::store).subscribe(result::setResult, result::setErrorResult);

        return result;
    }

    private Observable<String> store(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        LOGGER.info("Uploading file [{}] with size [{}]",
                fileName, file.getSize());
        try {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            ImageMetadata imageMetadata = new ImageMetadata();
            imageMetadata.setOriginalFormat(extension);
            imageMetadata.setName(fileName);
            catalogService.create(imageMetadata);
            return storageService.store(imageMetadata.resolveStorageKey(null), extension, file.getInputStream(), file.getSize()).
                    map(ignored -> imageMetadata.getId());
        } catch (IOException e) {
            LOGGER.error("Unable to upload file", e);
            throw Throwables.propagate(e);
        }
    }
}
