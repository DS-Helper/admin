package com.project.ds_helper.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "file.upload")
@Slf4j
public class FileUtil {

    private DataSize maxSize;
    private DataSize maxRequestSize;
    private String[] allowedFileExtensions;

    public boolean isValidSizeAndExtension(MultipartFile file) throws IOException {
        // Allowed File Extensions Logging
        StringBuilder sb = new StringBuilder();
        for(String extension : allowedFileExtensions){sb.append(extension).append("_");}
        log.info("allowed file extensions : {}", sb.toString());

        // Size Prove
        if(file == null || file.isEmpty() || maxSize.compareTo(DataSize.ofBytes(file.getSize())) < 0){
            throw new IOException("File Is Empty Or Size Invalid");
        }
        // Extension Proving
        for(String extension : allowedFileExtensions){if(extension.equals(Objects.requireNonNull(file.getContentType()).split("/")[1])){return true;}}
        return false;
    }

    // Null Check
    public List<MultipartFile> checkIfListIsNull(List<MultipartFile> files){
        if(files == null || files.isEmpty())return new ArrayList<MultipartFile>();
        return files;
    }

    // size check 2 or less
    public void checkIfListSizeNotBiggerThanOne(List<MultipartFile> images){
        log.info("images size : {}", images.size());
        if(images.size() > 1){throw new IllegalArgumentException("File Size Invalid");}
    }

    // null + empty + size check
    public List<MultipartFile> checkNullEmptySizeOne(List<MultipartFile> images){
        checkIfListSizeNotBiggerThanOne(images);
        return checkIfListIsNull(images);
    }
}
