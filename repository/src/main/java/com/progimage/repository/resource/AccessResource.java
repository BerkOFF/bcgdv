package com.progimage.repository.resource;

import com.amazonaws.util.IOUtils;
import com.progimage.client.service.ConversionService;
import com.progimage.client.ServiceGenerator;
import com.progimage.exception.NotFoundException;
import com.progimage.exception.UnsupportedFormatException;
import com.progimage.repository.model.ImageMetadata;
import com.progimage.repository.service.CatalogService;
import com.progimage.repository.service.StorageService;
import com.progimage.repository.validators.ValidImage;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import retrofit.mime.TypedByteArray;
import rx.Observable;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequestMapping("/progimage/repository/")
public class AccessResource {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private StorageService storageService;

    @Value("${conversion.service.url}")
    private String conversionServiceUrl;

    private ConversionService conversionService;

    @PostConstruct
    public void init() {
        conversionService = ServiceGenerator.createService(ConversionService.class, conversionServiceUrl);
    }

    @RequestMapping(value = "{id:[^\\.]+}")
    public void getImage(@PathVariable String id,
                         HttpServletResponse response) throws UnsupportedFormatException {
        ImageMetadata imageMetadata = catalogService.read(id);
        if (imageMetadata == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            response.sendRedirect(storageService.resolveURL(imageMetadata.resolveStorageKey(null)).toString());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @RequestMapping(value = "{id}.{extension}")
    public void getImageInRequestedFormat(@PathVariable String id, @PathVariable String extension,
                                          HttpServletResponse response) throws UnsupportedFormatException {

        ensureFormatSupported(extension);
        ImageMetadata imageMetadata = catalogService.read(id);
        if (imageMetadata == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!imageMetadata.supports(extension)) {
            convertImageToFormatAndStore(imageMetadata, extension).toBlocking().single();
        }

        try {
            response.sendRedirect(storageService.resolveURL(
                    imageMetadata.resolveStorageKey(extension)).toString());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @RequestMapping(value = "urls", method = RequestMethod.POST)
    public DeferredResult<Map<String, URL>> bulkRetrieval(@RequestBody Set<String> ids, @RequestParam(required = false) String format)
            throws UnsupportedFormatException, NotFoundException {
        if (!StringUtils.isEmpty(format)) {
            ensureFormatSupported(format);
        }
        Map<String, ImageMetadata> imagesMetadata =
                ids.parallelStream().map(catalogService::read).filter(imageMetadata -> imageMetadata != null).
                        collect(Collectors.toMap(ImageMetadata::getId, imageMetadata -> imageMetadata));

        ids.removeAll(imagesMetadata.keySet());
        if (!ids.isEmpty()) {
            throw new NotFoundException(
                    String.format("Following ids are not found: [%s]", Joiner.on(',').join(ids)));
        }

        DeferredResult<Map<String, URL>> result = new DeferredResult<>();

        Observable.from(imagesMetadata.values()).
                flatMap(imageMetadata -> {
                    if (imageMetadata.supports(format)) {
                        return Observable.just(imageMetadata.getId());
                    } else {
                        return convertImageToFormatAndStore(imageMetadata, format).
                                map(dummy -> imageMetadata.getId());
                    }
                }).
                map(id ->
                        Collections.singletonMap(id, storageService.resolveURL(
                                imagesMetadata.get(id).resolveStorageKey(format)))
                ).reduce(Maps.<String, URL>newHashMap(), (m1, m2) -> {
            (m1).putAll(m2);
            return m1;
        }).subscribe(result::setResult, result::setErrorResult);

        return result;
    }

    private Observable<Void> convertImageToFormatAndStore(ImageMetadata imageMetadata, String format) {
        return storageService.retrieve(imageMetadata.resolveStorageKey(null)).
                map(is -> {
                    try {
                        return IOUtils.toByteArray(is);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                }).flatMap(data ->
                conversionService.convertImagePayload(new TypedByteArray(APPLICATION_OCTET_STREAM_VALUE, data), format)).
                flatMap(conversionResponse -> {
                    try {
                        return storageService.store(
                                imageMetadata.resolveStorageKey(format), format,
                                conversionResponse.getBody().in(),
                                conversionResponse.getBody().length());
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                }).map(ignored -> {
            catalogService.update(imageMetadata);
            return null;
        });
    }

    private void ensureFormatSupported(String format) throws UnsupportedFormatException {
        if (!ValidImage.SUPPORTED_FORMATS.contains(format)) {
            throw new UnsupportedFormatException(
                    String.format("Requested image format [%s] is not supported", format));
        }
    }
}
