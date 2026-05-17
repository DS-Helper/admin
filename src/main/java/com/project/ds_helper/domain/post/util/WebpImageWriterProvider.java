package com.project.ds_helper.domain.post.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

@Slf4j
class WebpImageWriterProvider {

    private final Function<String, Iterator<ImageWriter>> writerLookup;

    WebpImageWriterProvider() {
        this(ImageIO::getImageWritersByFormatName);
    }

    WebpImageWriterProvider(Function<String, Iterator<ImageWriter>> writerLookup) {
        this.writerLookup = writerLookup;
    }

    ImageWriter findWebpWriter() {
        ImageIO.scanForPlugins();

        for (String formatName : new String[]{"webp", "WebP", "WEBP"}) {
            Iterator<ImageWriter> writers = writerLookup.apply(formatName);
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                log.debug("ImageCompressionUtil.findWebpWriter selected formatName={}, writer={}",
                        formatName, writer.getClass().getName());
                return writer;
            }
        }

        throw new IllegalStateException("No ImageWriter found for WebP. availableWriterFormats="
                + Arrays.toString(ImageIO.getWriterFormatNames()));
    }
}
