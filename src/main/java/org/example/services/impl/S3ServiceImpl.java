package org.example.services.impl;

import org.example.services.IS3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3ServiceImpl implements IS3Service {

    @Value("${upload.s3.localPath}")
    private String localPath;

    private final S3Client s3Client;
     // Inyección de dependencias por constructor
    public S3ServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        try{
            String fileName = file.getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket("bucket-first-api")
                    .key(fileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            return "Archivo subido correctamente";
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String downLoadFile(String fileName) throws IOException {

        if(!doesObjectExist(fileName)){
            return "Archivo no encontrado";
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("bucket-first-api")
                .key(fileName)
                .build();

        ResponseInputStream<GetObjectResponse> result = s3Client.getObject(request);

        try(FileOutputStream fos = new FileOutputStream(localPath + fileName)){
            byte[] read_buf = new byte[1024];
            int read_len = 0;

            while ((read_len = result.read(read_buf)) != -1) {
                fos.write(read_buf, 0, read_len);
            }
        } catch(IOException e){
            throw new IOException(e.getMessage());
        }
        return "Archivo encontrado";
    }

    public List<String> listFiles() throws IOException {
        try{
            ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                    .bucket("bucket-first-api")
                    .build();
            List<S3Object> objects = s3Client.listObjects(listObjectsRequest).contents();
            List<String> fileNames = new ArrayList<>();

            for (S3Object object : objects) {
                fileNames.add(object.key());
            }
            return fileNames;
        } catch (S3Exception e){
            throw new IOException(e.getMessage());
        }
    }

    private boolean doesObjectExist(String objectKey){
        try{
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket("bucket-first-api")
                    .key(objectKey)
                    .build();
            s3Client.headObject(headObjectRequest);

        } catch (S3Exception e){
            if(e.statusCode() == 404){
                return false;
            }
        }
        return true;
    }
}
