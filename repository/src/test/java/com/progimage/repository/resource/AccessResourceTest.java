package com.progimage.repository.resource;

import com.amazonaws.util.IOUtils;
import com.google.common.collect.ImmutableList;
import com.progimage.repository.model.ImageMetadata;
import com.progimage.repository.service.CatalogService;
import com.progimage.repository.service.StorageService;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import rx.Observable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccessResourceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @MockBean
    private CatalogService catalogService;

    @MockBean
    private StorageService storageService;

    private final static String missingImageId = "a53cf7e6-5ea0-4f37-8e10-619ac2b9190c";
    private final static String existingImageId = "b53cf7e6-5ea0-4f37-8e10-619ac2b9190c";
    private final static String existingJpgStorageId = "c53cf7e6-5ea0-4f37-8e10-619ac2b9190c";
    private final static String existingPngStorageId = "d53cf7e6-5ea0-4f37-8e10-619ac2b9190c";

    private final static String s3RedirectUrlJpg = "http://somepath/image.jpq";
    private final static String s3RedirectUrlPng = "http://somepath/image.png";
    private final static String s3RedirectDefaultUrl = "http://somepath/image.ext";

    private static byte[] existingJpgData;

    static {
        try {
            existingJpgData = IOUtils.toByteArray(
                    AccessResourceTest.class.getResourceAsStream("/image.jpg"));
        } catch (IOException ignored) {
        }
    }

    @Before
    public void init() throws Exception {
        given(catalogService.read(missingImageId)).willReturn(null);

        ImageMetadata imageMetadata = new ImageMetadata();
        imageMetadata.setId(existingImageId);
        imageMetadata.setOriginalFormat("jpg");
        imageMetadata.setName("some_name.jpg");
        imageMetadata.setCreatedAt(new Date());
        imageMetadata.setUpdatedAt(new Date());
        imageMetadata.getFormatsMapping().put("jpg", existingJpgStorageId);
        imageMetadata.getFormatsMapping().put("png", existingPngStorageId);
        given(catalogService.read(existingImageId)).willReturn(imageMetadata);

        given(storageService.retrieve(existingJpgStorageId)).
                willReturn(Observable.just(new ByteArrayInputStream(existingJpgData)));

        given(storageService.resolveURL(anyString())).willReturn(new URL(s3RedirectDefaultUrl));

        given(storageService.resolveURL(existingJpgStorageId)).willReturn(new URL(s3RedirectUrlJpg));
        given(storageService.resolveURL(existingPngStorageId)).willReturn(new URL(s3RedirectUrlPng));


        given(storageService.store(
                any(String.class), any(String.class), any(InputStream.class), anyLong())).
                willReturn(Observable.just(null));

        stubFor(post(urlEqualTo("/progimage/conversion?format=bmp"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "image/bmp")
                        .withBody(new byte[] {1, 2, 3})));
    }

    @Test
    public void getImageByIdNotFound() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/progimage/repository/{id}", String.class, missingImageId);
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void getExistingImageDefaultFormatById() {
        ResponseEntity<byte[]> response =
                restTemplate.getForEntity("/progimage/repository/{id}", byte[].class, existingImageId);
        assertTrue(response.getStatusCode() == HttpStatus.FOUND);
        assertEquals(s3RedirectUrlJpg, response.getHeaders().getLocation().toString());
        Mockito.verify(storageService, times(0)).store(anyString(), anyString(), any(InputStream.class), anyLong());
    }

    @Test
    public void getExistingImagePngFormatById() {
        ResponseEntity<byte[]> response =
                restTemplate.getForEntity("/progimage/repository/{id}.{ext}", byte[].class, existingImageId, "png");
        assertTrue(response.getStatusCode() == HttpStatus.FOUND);
        assertEquals(s3RedirectUrlPng, response.getHeaders().getLocation().toString());
        Mockito.verify(storageService, times(0)).store(anyString(), anyString(), any(InputStream.class), anyLong());
    }

    @Test
    public void getExistingImageBmpFormatById() {
        ResponseEntity<byte[]> response =
                restTemplate.getForEntity("/progimage/repository/{id}.{ext}", byte[].class, existingImageId, "bmp");
        assertTrue(response.getStatusCode() == HttpStatus.FOUND);
        assertEquals(s3RedirectDefaultUrl, response.getHeaders().getLocation().toString());
        Mockito.verify(storageService, times(1)).store(anyString(), anyString(), any(InputStream.class), anyLong());
    }

    @Test
    public void getExistingImageBulk() {
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/progimage/repository/urls", ImmutableList.of(existingImageId), Map.class);
        assertTrue(response.getStatusCode() == HttpStatus.OK);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() == 1);
        assertEquals(s3RedirectUrlJpg, response.getBody().get(existingImageId));
    }
}
