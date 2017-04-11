package com.progimage.repository.resource;

import com.amazonaws.util.IOUtils;
import com.progimage.client.ServiceGenerator;
import com.progimage.client.service.RepositoryAccessService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import retrofit.client.Response;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepositoryIntegrationTest {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private RepositoryAccessService repositoryAccessService;

    @Before
    public void init() {
        repositoryAccessService =
                ServiceGenerator.createService(RepositoryAccessService.class, "http://localhost:" + port);
    }

    @Test
    public void uploadDownloadTest() throws Exception {
        byte[] imageData = IOUtils.toByteArray(
                this.getClass().getResourceAsStream("/image.jpg"));
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new ClassPathResource("/image.jpg"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(
                map, headers);
        ResponseEntity<String> result = restTemplate.exchange(
                "/progimage/repository/upload", HttpMethod.POST, requestEntity,
                String.class);

        assertTrue(result.getStatusCode() == HttpStatus.CREATED);
        assertNotNull(requestEntity.getBody());
        String id = result.getBody();

        Response response = repositoryAccessService.downloadOriginal(id).toBlocking().single();

        assertTrue(response.getStatus() == HttpStatus.OK.value());

        assertNotNull(response.getBody());

        assertArrayEquals(imageData, IOUtils.toByteArray(response.getBody().in()));
    }
}
