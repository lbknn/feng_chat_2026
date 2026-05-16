package org.example.fengbushi.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {
    
    private final GridFsTemplate gridFsTemplate;
    
    /**
     * 上传文件到GridFS
     * @param file 上传的文件
     * @return 文件ID
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("文件不能为空");
        }
        
        // 验证文件大小（最大10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IOException("文件大小不能超过10MB");
        }
        
        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("文件名无效");
        }
        
        // 存储文件到GridFS
        ObjectId fileId = gridFsTemplate.store(
            file.getInputStream(),
            originalFilename,
            file.getContentType()
        );
        
        log.info("文件上传成功到GridFS: fileId={}, filename={}", fileId, originalFilename);
        
        return fileId.toString();
    }
    
    /**
     * 获取文件信息
     * @param fileId 文件ID
     * @return 文件资源
     */
    public GridFsResource getFile(String fileId) {
        try {
            ObjectId objectId = new ObjectId(fileId);
            GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(objectId)));
            
            if (gridFSFile == null) {
                log.warn("文件不存在: {}", fileId);
                return null;
            }
            
            return gridFsTemplate.getResource(gridFSFile);
        } catch (Exception e) {
            log.error("获取文件失败: {}", fileId, e);
            return null;
        }
    }
    
    /**
     * 删除文件
     * @param fileId 文件ID
     */
    public void deleteFile(String fileId) {
        try {
            ObjectId objectId = new ObjectId(fileId);
            gridFsTemplate.delete(Query.query(Criteria.where("_id").is(objectId)));
            log.info("文件删除成功: {}", fileId);
        } catch (Exception e) {
            log.error("删除文件失败: {}", fileId, e);
        }
    }
}
