package com.progimage.conversion.service;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

@Service
public class FormatConversionService {
    private final static HashSet SUPPORTED_FORMATS = Sets.newHashSet(ImageIO.getWriterFormatNames());

    public boolean isFormatSupported(String format) {
        return SUPPORTED_FORMATS.contains(format);
    }

    public void convert(InputStream input, OutputStream outputStream, String format) throws IOException {
        BufferedImage image = ImageIO.read(input);
        ImageIO.write(image, format, outputStream);
    }
}
