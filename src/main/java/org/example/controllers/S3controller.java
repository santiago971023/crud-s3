package org.example.controllers;

import org.example.services.IS3Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class S3controller {

    private final IS3Service is3Service;

    public S3controller(IS3Service is3Service) {
        this.is3Service = is3Service;
    }

    @GetMapping("/download/{fileName}")
    public String downloadFile(@PathVariable("fileName") String fileName) throws IOException {
        return is3Service.downLoadFile(fileName);
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return is3Service.uploadFile(file);
    }



}
