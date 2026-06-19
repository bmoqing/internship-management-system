package fun.bmoqing.internship.controller;

import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.service.LocalFileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final LocalFileStorageService localFileStorageService;

    public FileController(LocalFileStorageService localFileStorageService) {
        this.localFileStorageService = localFileStorageService;
    }

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(defaultValue = "common") String bizType) {
        if (AuthUtil.currentUserId() == null) {
            return Result.unauthorized("请先登录");
        }

        LocalFileStorageService.UploadResult result = localFileStorageService.store(file, bizType);
        return Result.success(result);
    }

    @GetMapping("/access-url")
    public Result<?> accessUrl(@RequestParam("path") String path) {
        if (AuthUtil.currentUserId() == null) {
            return Result.unauthorized("请先登录");
        }
        localFileStorageService.resolveReadableFile(path);
        String accessUrl = localFileStorageService.buildSignedDownloadUrl(path);
        return Result.success(accessUrl);
    }

    @GetMapping("/download")
    public ResponseEntity<?> download(@RequestParam("path") String path,
                                      @RequestParam(required = false) Long expires,
                                      @RequestParam(required = false) String sig) throws MalformedURLException {
        boolean signedAccess = localFileStorageService.verifySignedAccess(path, expires, sig);
        if (AuthUtil.currentUserId() == null && !signedAccess) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("下载链接已过期或无权限访问"));
        }

        Path filePath = localFileStorageService.resolveReadableFile(path);
        Resource resource = new UrlResource(filePath.toUri());
        String encodedName = URLEncoder.encode(filePath.getFileName().toString(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException ex) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}
