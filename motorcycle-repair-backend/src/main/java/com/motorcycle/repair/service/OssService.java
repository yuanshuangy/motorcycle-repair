package com.motorcycle.repair.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.motorcycle.repair.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class OssService {

    @Autowired
    private OssConfig ossConfig;

    public String uploadFile(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectName = "uploads/" + fileName;

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret());

            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(file.getContentType());

            InputStream is = file.getInputStream();
            ossClient.putObject(ossConfig.getBucketName(), objectName, is, meta);
            is.close();

            String urlPrefix = ossConfig.getUrlPrefix();
            if (urlPrefix == null || urlPrefix.isEmpty()) {
                urlPrefix = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
            }
            if (urlPrefix.endsWith("/")) {
                urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
            }
            return urlPrefix + "/" + objectName;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
