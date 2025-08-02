package org.example.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IS3Service {

    public String uploadFile(MultipartFile file) throws IOException;
    public String downLoadFile(String fileName) throws IOException;

}
