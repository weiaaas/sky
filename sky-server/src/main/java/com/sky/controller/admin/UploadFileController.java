package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@Api(tags="通用接口")
/**
 * 文件上传接口
 */
public class UploadFileController {
    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("admin/common/upload")
    @ApiOperation("文件上传")
    public Result upload(MultipartFile file){
        log.info("上传文件",file);
        String url = null;
        try {
            /**
             * 这么做是为了防止文件重名
             */
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构造新文件名称
            String name=UUID.randomUUID().toString()+extension;
            url = aliOssUtil.upload(file.getBytes(),name);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败:{}",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
