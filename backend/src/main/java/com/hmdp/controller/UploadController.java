package com.hmdp.controller;

import cn.hutool.core.util.StrUtil;
import com.hmdp.config.UploadProperties;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    private final UploadProperties uploadProperties;

    public UploadController(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 生成新文件名（返回给前端的相对路径，形如 /blogs/a/b/uuid.jpg）
            String fileName = createNewFileName(image.getOriginalFilename());
            // 解析成上传根目录下的真实路径
            Path target = resolveUnderRoot(fileName);
            // 按需创建 /blogs/d1/d2 目录
            Files.createDirectories(target.getParent());
            // 保存文件
            image.transferTo(target.toFile());
            log.debug("文件上传成功，{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        Path target;
        try {
            target = resolveUnderRoot(filename);
        } catch (IllegalArgumentException e) {
            log.warn("拒绝非法的删除请求: {}", filename);
            return Result.fail("非法的文件名称");
        }
        if (Files.isDirectory(target)) {
            return Result.fail("错误的文件名称");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.error("删除文件失败: {}", target, e);
            return Result.fail("删除失败");
        }
        return Result.ok();
    }

    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 返回给前端的路径，前端会拼成 /imgs/blogs/{d1}/{d2}/{uuid}.{suffix}
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }

    /**
     * 把接口传入的路径转换成上传根目录下的真实路径。
     *
     * <p>兼容三种写法：{@code /blogs/a/b/x.jpg}、{@code /imgs/blogs/a/b/x.jpg}、
     * {@code imgs/blogs/a/b/x.jpg}（前端删除接口传的是第二种）。
     *
     * <p>会拒绝任何跳出上传根目录的路径，防止 {@code ../../} 目录穿越。
     */
    private Path resolveUnderRoot(String name) {
        if (name == null) {
            throw new IllegalArgumentException("文件名为空");
        }
        String cleaned = name.replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("imgs/")) {
            cleaned = cleaned.substring("imgs/".length());
        }
        if (cleaned.isEmpty() || cleaned.contains("..")) {
            throw new IllegalArgumentException("非法的文件名称");
        }
        Path root = uploadProperties.resolveDir();
        Path target = root.resolve(cleaned).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法的文件名称");
        }
        return target;
    }
}