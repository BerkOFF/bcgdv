package com.progimage.conversion.resource;

import com.progimage.client.ServiceGenerator;
import com.progimage.client.service.RepositoryAccessService;
import com.progimage.conversion.service.FormatConversionService;
import com.progimage.exception.BadRequestException;
import com.progimage.exception.UnsupportedFormatException;
import com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

@RestController
@RequestMapping("/progimage/conversion")
public class ConversionResource {

    @Autowired
    private FormatConversionService formatConversionService;

    @Value("${repository.service.url}")
    private String repositoryServiceUrl;

    private RepositoryAccessService repositoryAccessService;

    @PostConstruct
    public void init() {
        repositoryAccessService = ServiceGenerator.createService(RepositoryAccessService.class, repositoryServiceUrl);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void convertFromPayload(@RequestParam String format, HttpServletRequest request,
                                   HttpServletResponse response) throws UnsupportedFormatException, IOException {
        ensureFormatSupported(format);
        response.setContentType("image/" + format);
        formatConversionService.convert(request.getInputStream(), response.getOutputStream(), format);
    }

    @RequestMapping(method = RequestMethod.GET)
    public void convertFromUrlOrRepository(@RequestParam(required = false) String url,
                                           @RequestParam(required = false) String id, @RequestParam String format,
                                           HttpServletResponse response) throws UnsupportedFormatException, IOException {
        if (StringUtils.isEmpty(url) && StringUtils.isEmpty(id) ||
                StringUtils.hasText(url) && StringUtils.hasText(id)) {
            throw new BadRequestException("Either 'id' or 'url' should be provided");
        }

        ensureFormatSupported(format);
        response.setContentType("image/" + format);

        if (StringUtils.hasText(url)) {
            formatConversionService.convert(
                    new URL(url).openConnection().getInputStream(),
                    response.getOutputStream(), format);
        } else {
            repositoryAccessService.downloadOriginal(id).
                    map(repositoryResponse -> {
                        try {
                            formatConversionService.convert(
                                    repositoryResponse.getBody().in(),
                                    response.getOutputStream(), format);
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                        return null;
                    }).toBlocking().single();
        }
    }

    private void ensureFormatSupported(String format) throws UnsupportedFormatException {
        if (!formatConversionService.isFormatSupported(format)) {
            throw new UnsupportedFormatException(String.format("Requested image format [%s] is not supported", format));
        }
    }
}
