package org.example.services.impl;

import org.example.services.IS3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import software.amazon.awssdk.services.s3.model.S3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3ServiceImpl implements IS3Service {

    @Value("${upload.s3.localPath}")
    private String localPath;

    @Value("${aws.s3.bucketName}") // Inyecta el nombre del bucket
    private String bucketName;

    private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class); // Inicializa un logger


    private final S3Client s3Client;
     // Inyección de dependencias por constructor
    public S3ServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        try{
            String fileName = file.getOriginalFilename();
            if(file.isEmpty() || fileName.trim().isEmpty()){
                throw new IllegalArgumentException("The file name cannot be empty or null.");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            logger.info("Archivo '{}' subido exitosamente al bucket '{}'.", fileName, bucketName );
            return "Archivo subido correctamente";

        } catch (IOException e) {
            logger.error("Error de E/S al intentar subir el archivo '{}':'{}'", file.getOriginalFilename(), e.getMessage());
            throw new IOException("Error de lectura/escritura al subir el archivo: " + e.getMessage(), e);
        } catch (S3Exception e) {
            logger.error("Error de S3 al intentar subir el archivo '{}': {}", file.getOriginalFilename(), e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Error del servicio S3 al intentar subir el archivo.'" + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) { // Un catch genérico para cualquier otra excepción inesperada
            logger.error("Error inesperado al subir el archivo '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new IOException("Error inesperado al subir el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String downLoadFile(String fileName) throws IOException {

        if(!doesObjectExist(fileName)){
            return "Archivo no encontrado";
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
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

    @Override
    public List<String> listFiles() throws IOException {
        try{
            ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                    .bucket(bucketName)
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

    @Override
    public String renameFile(String oldFileName, String newFileName) throws IOException {
        if(oldFileName == null || oldFileName.trim().isEmpty() || newFileName == null || newFileName.trim().isEmpty()){
            logger.warn("Intento de renombrar el archivo con nombres nulos o vacíos. Old: '{}', New: '{}'", oldFileName, newFileName);
            throw new IllegalArgumentException("Los nombres de archivo no pueden ser nulos o vacíos.");
        }
        if(oldFileName.equals(newFileName)){
            logger.info("Intento de renombrar el archivo a sí mismo: '{}'. No se realizó ninguna operación.", oldFileName);
            return "El archivo ya tiene ese nombre, no se realizó ninguna operación.";
        }

        if(!doesObjectExist(oldFileName)){
            logger.warn("Intento de renombrar archivo '{}' que no existe.", oldFileName);
            return "Archivo no encontrado";
        }
        try{
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .destinationBucket(bucketName)
                    .copySource("/"+bucketName+"/"+oldFileName)
                    .destinationKey(newFileName)
                    .build();

            logger.info("Copiando objeto '{}' a '{}' en el bucket '{}'.", oldFileName, newFileName, bucketName);
            s3Client.copyObject(copyObjectRequest);
            logger.info("Objeto '{}' copiado exitosamente a '{}'.", oldFileName, newFileName);

            String deleteStatus = deleteFile(oldFileName);
            if ("Archivo eliminado".equals(deleteStatus)) {
                logger.info("Objeto original '{}' eliminado exitosamente después de la copia.", oldFileName);
                return "Archivo renombrado con éxito a " + newFileName;
            } else {
                logger.error("El objeto original '{}' NO pudo ser eliminado después de la copia. Estado: {}", oldFileName, deleteStatus);
                return "Archivo renombrado con éxito a " + newFileName + ", pero el archivo original NO se eliminó correctamente.";
            }

        } catch (S3Exception e) {
            // Captura S3Exception: Errores específicos de S3 durante la copia
            logger.error("Error de S3 al intentar renombrar el archivo de '{}' a '{}': {}", oldFileName, newFileName, e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Error del servicio S3 al renombrar el archivo: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            logger.error("Error de E/S al intentar renombrar el archivo de '{}' a '{}' : '{}'", oldFileName, newFileName, e.getMessage(), e );
            throw new IOException("Error de E/S al renombrar el archivo: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error inesperado al intentar renombrar el archivo de '{}' a '{}': {}", oldFileName, newFileName, e.getMessage(), e);
            throw new IOException("Error inesperado al renombrar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String updateFile(MultipartFile file, String oldFileName) throws IOException {
        if(!doesObjectExist(oldFileName)){
            return "Archivo no encontrado";
        }

        try{
            String newFileName = file.getOriginalFilename();
            deleteFile(oldFileName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newFileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            return "Archivo actualizado con éxito a " + newFileName;

        } catch (S3Exception e){
            throw new IOException(e.getMessage());
        }

    }

    @Override
    public String deleteFile(String fileName) throws IOException{
        if(!doesObjectExist(fileName)){
            return "Archivo no encontrado";
        }
        try{
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            return "Archivo eliminado";
        } catch(S3Exception e){
            throw new IOException(e.getMessage());
        }

    }





    private boolean doesObjectExist(String objectKey){
        try{
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
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
