package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.service.FileStorageService;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
    
    private final FileStorageService fileStorageService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileId = fileStorageService.uploadFile(file);
            
            Map<String, String> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("filename", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));
            result.put("url", "/api/file/download/" + fileId); // 返回下载URL
            
            return ApiResponse.success(result);
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 下载/查看文件
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        try {
            GridFsResource resource = fileStorageService.getFile(fileId);
            
            if (resource == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 读取文件内容
            byte[] content = resource.getInputStream().readAllBytes();
            
            // 检测文件类型
            String contentType = resource.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            String filename = resource.getFilename();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
