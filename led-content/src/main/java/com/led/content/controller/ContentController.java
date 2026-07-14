package com.led.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.Content;
import com.led.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    /** 分页查询 */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Page<Content>> list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type) {
        return R.ok(contentService.pageContents(page, size, keyword, type));
    }

    /** 查询全部 */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<List<Content>> all() {
        return R.ok(contentService.listAll());
    }

    /** 查询单个 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Content> getById(@PathVariable Long id) {
        return R.ok(contentService.getById(id));
    }

    /** 创建内容（含文件上传） */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @AuditLog(module = "内容管理", action = "CREATE", description = "上传内容")
    public R<Content> create(@RequestParam(value = "file", required = false) MultipartFile file,
                             @RequestParam("name") String name,
                             @RequestParam("type") String type,
                             @RequestParam(value = "textContent", required = false) String textContent,
                             @RequestParam(value = "fontSize", required = false) Integer fontSize,
                             @RequestParam(value = "fontColor", required = false) String fontColor,
                             @RequestParam(value = "bgColor", required = false) String bgColor,
                             @RequestParam(value = "scrollSpeed", required = false) Integer scrollSpeed) throws IOException {
        Content content = new Content();
        content.setName(name);
        content.setType(type);
        content.setTextContent(textContent);
        content.setFontSize(fontSize);
        content.setFontColor(fontColor);
        content.setBgColor(bgColor);
        content.setScrollSpeed(scrollSpeed);
        return R.ok(contentService.createContent(content, file));
    }

    /** 编辑内容 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @AuditLog(module = "内容管理", action = "UPDATE", description = "编辑内容")
    public R<Content> update(@PathVariable Long id,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             @RequestParam("name") String name,
                             @RequestParam("type") String type,
                             @RequestParam(value = "textContent", required = false) String textContent,
                             @RequestParam(value = "fontSize", required = false) Integer fontSize,
                             @RequestParam(value = "fontColor", required = false) String fontColor,
                             @RequestParam(value = "bgColor", required = false) String bgColor,
                             @RequestParam(value = "scrollSpeed", required = false) Integer scrollSpeed) throws IOException {
        Content content = new Content();
        content.setId(id);
        content.setName(name);
        content.setType(type);
        content.setTextContent(textContent);
        content.setFontSize(fontSize);
        content.setFontColor(fontColor);
        content.setBgColor(bgColor);
        content.setScrollSpeed(scrollSpeed);
        return R.ok(contentService.updateContent(content, file));
    }

    /** 删除内容 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", action = "DELETE", description = "删除内容")
    public R<Void> delete(@PathVariable Long id) {
        contentService.deleteContent(id);
        return R.okMsg("删除成功");
    }
}
