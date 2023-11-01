package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import com.heima.minio.MinIOApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.checkerframework.checker.units.qual.A;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinioTest {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 把list.html文件上传到minio，并且可以在浏览器中访问
     */
    @Test
    public void test() throws FileNotFoundException {
        FileInputStream fileInputStream=new FileInputStream("E:\\Code\\springcloud_heima\\css\\index.css");
        String path = fileStorageService.uploadHtmlFile("", "plugins/css/index.html", fileInputStream);
        System.out.println(path);
    }

    /**
     * 把list.html文件上传到minio，并且可以在浏览器中访问
     */
    public static void main(String[] args) {
        try {
            FileInputStream fileInputStream=new FileInputStream("E:\\Code\\springcloud_heima\\js\\axios.min.js");
            //获取minio的链接信息，创建一个minio客户端
            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123").endpoint("http://192.168.150.101:9000").build();

            //上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("plugins/js/axios.min.js")//文件名称
                    .contentType("text/javascript")//文件类型
                    .bucket("leadnews")//桶名称
                    .stream(fileInputStream, fileInputStream.available(), -1).build();
            minioClient.putObject(putObjectArgs);

            //访问路径
            System.out.println("http://192.168.150.101:9000/leadnews/list.html");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
