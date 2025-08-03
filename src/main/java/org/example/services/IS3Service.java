package org.example.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IS3Service {

    public String uploadFile(MultipartFile file) throws IOException;
    public String downLoadFile(String fileName) throws IOException;
    public List<String> listFiles() throws IOException;
    public String deleteFile(String fileName) throws IOException;
    public String renameFile(String oldFileName, String newFileName) throws IOException;
    public String updateFile(MultipartFile file, String oldFileName) throws IOException;

}
