package com.project.ds_helper.domain.post.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageWriteParam;
import java.io.IOException;

@Slf4j
class WebpImageWriteParamConfigurer {

    void configure(ImageWriteParam writeParam, float quality) {
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] compressionTypes = writeParam.getCompressionTypes();
            if (compressionTypes != null && compressionTypes.length > 0) {
                String lossyCompressionType = findLossyCompressionType(compressionTypes);
                writeParam.setCompressionType(lossyCompressionType);
                log.debug("ImageCompressionUtil.writeCompressedBytes selected compressionType={}", lossyCompressionType);
            }
            writeParam.setCompressionQuality(quality);
        }
    }

    String findLossyCompressionType(String[] compressionTypes) {
        for (String compressionType : compressionTypes) {
            if ("lossy".equalsIgnoreCase(compressionType)) {
                return compressionType;
            }
        }
        return compressionTypes[0];
    }

    void verifyCompressedBytes(byte[] bytes) throws IOException {
        if (bytes.length == 0) {
            throw new IOException("Compressed image verification failed");
        }
    }
}
