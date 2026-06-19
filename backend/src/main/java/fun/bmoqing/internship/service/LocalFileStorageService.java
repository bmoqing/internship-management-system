/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.net.URLEncoder;

@Service
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "txt",
            "png", "jpg", "jpeg", "gif", "bmp", "webp",
            "zip", "rar"
    );

    private final Path basePath;
    private final long maxSizeBytes;
    private final String signSecret;
    private final long signExpireSeconds;

    public LocalFileStorageService(@Value("${app.upload.base-dir:./uploads}") String baseDir,
                                   @Value("${app.upload.max-size-mb:20}") long maxSizeMb,
                                   @Value("${app.upload.sign-secret:internship-file-sign-secret-change-in-production}") String signSecret,
                                   @Value("${app.upload.sign-expire-seconds:300}") long signExpireSeconds) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeMb * 1024 * 1024;
        this.signSecret = signSecret;
        this.signExpireSeconds = signExpireSeconds;
    }

    public UploadResult store(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持" + (maxSizeBytes / 1024 / 1024) + "MB");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(originalName)) {
            throw new IllegalArgumentException("无法识别文件名");
        }
        if (originalName.contains("..")) {
            throw new IllegalArgumentException("文件名不合法");
        }

        String extension = resolveExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("文件类型不支持，仅允许上传常见文档和图片");
        }

        String safeBizType = normalizeBizType(bizType);
        String monthDir = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = basePath.resolve(safeBizType).resolve(monthDir).normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();

        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("文件保存失败，请稍后重试", ex);
        }

        String path = "/files/" + safeBizType + "/" + monthDir + "/" + fileName;
        String accessUrl = buildSignedDownloadUrl(path);
        return new UploadResult(path, accessUrl, originalName, file.getSize(), file.getContentType());
    }

    public Path resolveReadableFile(String pathValue) {
        String normalizedRelativePath = normalizeToRelativePath(pathValue);
        Path targetFile = basePath.resolve(normalizedRelativePath).normalize();
        if (!targetFile.startsWith(basePath)) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            throw new IllegalArgumentException("文件不存在或已被删除");
        }
        return targetFile;
    }

    public String buildSignedDownloadUrl(String pathValue) {
        String normalizedPath = normalizeToPublicPath(pathValue);
        long expires = Instant.now().getEpochSecond() + signExpireSeconds;
        String sig = sign(normalizedPath, expires);
        return "/api/file/download?path=" + URLEncoder.encode(normalizedPath, StandardCharsets.UTF_8)
                + "&expires=" + expires
                + "&sig=" + URLEncoder.encode(sig, StandardCharsets.UTF_8);
    }

    public boolean verifySignedAccess(String pathValue, Long expires, String sig) {
        if (!StringUtils.hasText(pathValue) || expires == null || !StringUtils.hasText(sig)) {
            return false;
        }
        if (expires < Instant.now().getEpochSecond()) {
            return false;
        }

        String normalizedPath;
        try {
            normalizedPath = normalizeToPublicPath(pathValue);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        String expectedSig = sign(normalizedPath, expires);
        return MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeToPublicPath(String pathValue) {
        String relativePath = normalizeToRelativePath(pathValue);
        return "/files/" + relativePath;
    }

    private String normalizeToRelativePath(String pathValue) {
        if (!StringUtils.hasText(pathValue)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        String normalizedPath = pathValue.trim().replace('\\', '/');

        normalizedPath = extractPathParamIfNeeded(normalizedPath);

        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            normalizedPath = extractPathFromAbsoluteUrl(normalizedPath);
        }

        if (normalizedPath.startsWith("/files/")) {
            normalizedPath = normalizedPath.substring("/files/".length());
        } else if (normalizedPath.startsWith("files/")) {
            normalizedPath = normalizedPath.substring("files/".length());
        } else if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        if (!StringUtils.hasText(normalizedPath) || normalizedPath.contains("..")) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        return normalizedPath;
    }

    private String extractPathParamIfNeeded(String pathValue) {
        if (!pathValue.contains("/api/file/download")) {
            return pathValue;
        }

        int marker = pathValue.indexOf("path=");
        if (marker < 0) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        String pathPart = pathValue.substring(marker + 5);
        int ampIdx = pathPart.indexOf('&');
        if (ampIdx >= 0) {
            pathPart = pathPart.substring(0, ampIdx);
        }
        return URLDecoder.decode(pathPart, StandardCharsets.UTF_8);
    }

    private String extractPathFromAbsoluteUrl(String absoluteUrl) {
        if (absoluteUrl.contains("/api/file/download")) {
            return extractPathParamIfNeeded(absoluteUrl);
        }

        int marker = absoluteUrl.indexOf("/files/");
        if (marker < 0) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        return absoluteUrl.substring(marker);
    }

    private String sign(String normalizedPath, long expires) {
        String payload = normalizedPath + ":" + expires;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("生成访问签名失败", ex);
        }
    }

    private String normalizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return "common";
        }
        String safe = bizType.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        if (!StringUtils.hasText(safe)) {
            return "common";
        }
        if (safe.length() > 30) {
            return safe.substring(0, 30);
        }
        return safe;
    }

    private String resolveExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index <= 0 || index == fileName.length() - 1) {
            throw new IllegalArgumentException("文件后缀不能为空");
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public record UploadResult(String url, String accessUrl, String originalName, Long size, String contentType) {
    }
}
