package org.example.controllers;

import org.example.services.IS3Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    @GetMapping("/list")
    public List<String> getList() throws IOException {
        return is3Service.listFiles();
    }

    @DeleteMapping("/delete/{fileName}")
    public String deleteFile(@PathVariable("fileName") String fileName) throws IOException {
        return is3Service.deleteFile(fileName);
    }

    @PutMapping("/{oldFileName}/{newFileName}")
    public String updateName(@PathVariable("oldFileName") String oldFileName, @PathVariable("newFileName") String newFileName) throws IOException {
        return is3Service.renameFile(oldFileName, newFileName);
    }

    @PutMapping("/update/{oldFileName}")
    public String updateFile(@RequestParam("file") MultipartFile file, @PathVariable("oldFileName") String oldFileName) throws IOException {
        return is3Service.updateFile(file, oldFileName);
    }






}
