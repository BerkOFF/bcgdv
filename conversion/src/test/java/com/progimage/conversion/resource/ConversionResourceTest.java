package com.progimage.conversion.resource;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConversionResourceTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private static byte[] existingJpgData;
    private static byte[] existingPngData;

    static {
        try {
            existingJpgData = IOUtils.toByteArray(
                    ConversionResourceTest.class.getResourceAsStream("/image.jpg"));
            existingPngData = IOUtils.toByteArray(
                    ConversionResourceTest.class.getResourceAsStream("/image.png"));
        } catch (IOException ignored) {
        }
    }

    @Before
    public void init() throws IOException {
        stubFor(get(urlEqualTo("/download/image.jpg"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "image/jpg")
                        .withBody(existingJpgData)));

        stubFor(get(urlEqualTo("/progimage/repository/f53cf7e6-5ea0-4f37-8e10-619ac2b9190c"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "image/jpg")
                        .withBody(existingJpgData)));
    }

    @Test
    public void convertFromUrlPayloadSuccess() {
        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/progimage/conversion?format={format}", existingJpgData,
                byte[].class, "png");

        assertTrue(response.getStatusCode() == HttpStatus.OK);
        assertEquals(response.getHeaders().getContentType(), MediaType.IMAGE_PNG);
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertArrayEquals(existingPngData, response.getBody());
    }

    @Test
    public void convertFromUrlSuccess() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                "/progimage/conversion?url={url}&format={format}", byte[].class,
                "http://localhost:8089/download/image.jpg", "png");

        assertTrue(response.getStatusCode() == HttpStatus.OK);
        assertEquals(response.getHeaders().getContentType(), MediaType.IMAGE_PNG);
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertArrayEquals(existingPngData, response.getBody());
    }

    @Test
    public void convertFromRepositorySuccess() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                "/progimage/conversion?id={id}&format={format}", byte[].class,
                "f53cf7e6-5ea0-4f37-8e10-619ac2b9190c", "png");

        assertTrue(response.getStatusCode() == HttpStatus.OK);
        assertEquals(response.getHeaders().getContentType(), MediaType.IMAGE_PNG);
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertArrayEquals(existingPngData, response.getBody());
    }

    @Test
    public void convertFromUrlFormatMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/progimage/conversion?url={url}", String.class,
                "http://localhost:8089/download/image.jpg");

        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Required String parameter 'format' is not present"));
    }

    @Test
    public void convertFromUrlUnsupportedFormat() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/progimage/conversion?url={url}&format={format}", String.class,
                "http://localhost:8089/download/image.jpg", "pdf");

        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Requested image format [pdf] is not supported"));
    }

    @Test
    public void convertFromUrlIdOrUrlMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/progimage/conversion?format={format}", String.class,
                "jpg");

        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Either 'id' or 'url' should be provided"));
    }
}
