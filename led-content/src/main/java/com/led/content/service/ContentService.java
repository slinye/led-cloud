package com.led.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.Content;
import com.led.common.exception.BusinessException;
import com.led.content.mapper.ContentMapper;
import com.led.content.util.ThumbnailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ContentService extends ServiceImpl<ContentMapper, Content> {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public Page<Content> pageContents(int page, int size, String keyword, String type) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Content::getName, keyword);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Content::getType, type);
        }
        wrapper.orderByDesc(Content::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    public List<Content> listAll() {
        return baseMapper.selectAllOrderByTime();
    }

    @Transactional
    public Content createContent(Content content, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            saveUploadFile(content, file);
        }
        if (content.getDuration() == null) {
            content.setDuration(10);
        }
        save(content);
        return content;
    }

    @Transactional
    public Content updateContent(Content content, MultipartFile file) throws IOException {
        Content existing = getById(content.getId());
        if (existing == null) throw new BusinessException(404, "内容不存在");

        // 如果上传了新文件，删除旧文件
        if (file != null && !file.isEmpty()) {
            deleteFileIfExists(existing.getFilePath());
            deleteFileIfExists(existing.getThumbnailPath());
            saveUploadFile(content, file);
        } else {
            // 保留原有文件
            content.setFilePath(existing.getFilePath());
            content.setFileSize(existing.getFileSize());
            content.setThumbnailPath(existing.getThumbnailPath());
        }
        if (content.getDuration() == null) {
            content.setDuration(existing.getDuration());
        }
        updateById(content);
        return content;
    }

    @Transactional
    public void deleteContent(Long id) {
        Content content = getById(id);
        if (content == null) throw new BusinessException(404, "内容不存在");
        deleteFileIfExists(content.getFilePath());
        deleteFileIfExists(content.getThumbnailPath());
        removeById(id);
    }

    /** 保存上传文件并设置Content属性 */
    private void saveUploadFile(Content content, MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".") ?
                originalName.substring(originalName.lastIndexOf(".")) : "";
        String fileName = UUID.randomUUID().toString() + ext;
        File dest = new File(dir, fileName);
        file.transferTo(dest);

        content.setFilePath("/uploads/" + fileName);
        content.setFileSize(file.getSize());

        // 生成缩略图（图片类型）
        if (content.getType() != null && content.getType().startsWith("image/")) {
            try {
                String thumbName = "thumb_" + fileName;
                ThumbnailUtil.generateThumbnail(dest.getAbsolutePath(),
                        new File(dir, thumbName).getAbsolutePath(), 200, 200);
                content.setThumbnailPath("/uploads/" + thumbName);
            } catch (Exception e) {
                log.warn("缩略图生成失败: {}", e.getMessage());
            }
        }
    }

    private void deleteFileIfExists(String path) {
        if (path != null && path.startsWith("/uploads/")) {
            File file = new File(uploadDir, path.substring("/uploads/".length()));
            if (file.exists()) file.delete();
        }
    }
}
