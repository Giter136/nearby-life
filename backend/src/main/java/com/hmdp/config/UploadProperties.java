package com.hmdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片上传目录配置。
 *
 * <p>解析顺序：
 * <ol>
 *   <li>环境变量 {@code HMDP_UPLOAD_DIR} 或配置项 {@code hmdp.upload.dir}（写了就用它）；</li>
 *   <li>都没写时，从当前工作目录逐级向上查找 {@code frontend/html/hmdp/imgs}。</li>
 * </ol>
 *
 * <p>这样从 {@code backend/} 或仓库根目录启动都能自动定位，不必再把本机绝对路径写死在代码里。
 */
@Data
@Component
@ConfigurationProperties(prefix = "hmdp.upload")
public class UploadProperties {

    /** 图片上传根目录；留空则自动查找 */
    private String dir;

    /** 相对于项目根目录的上传目录 */
    private static final String RELATIVE_DIR = "frontend/html/hmdp/imgs";

    /** 向上查找的最大层数，够覆盖 backend/target 这类启动目录 */
    private static final int MAX_SEARCH_DEPTH = 5;

    /**
     * 解析出上传根目录的绝对路径。
     *
     * @throws IllegalStateException 配置了路径但无效，或自动查找失败
     */
    public Path resolveDir() {
        if (dir != null && !dir.trim().isEmpty()) {
            return Paths.get(dir.trim()).toAbsolutePath().normalize();
        }
        Path cursor = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < MAX_SEARCH_DEPTH && cursor != null; depth++) {
            Path candidate = cursor.resolve(RELATIVE_DIR);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException(
                "未找到图片上传目录，请设置环境变量 HMDP_UPLOAD_DIR 或配置 hmdp.upload.dir");
    }
}