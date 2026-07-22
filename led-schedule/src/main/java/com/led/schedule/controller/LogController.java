package com.led.schedule.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.led.common.dto.R;
import com.led.common.entity.PlayLog;
import com.led.schedule.service.PlayLogService;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final PlayLogService playLogService;

    /** 分页查询播放日志（支持名称+状态+时间范围筛选） */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<IPage<PlayLog>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String screenName,
                                   @RequestParam(required = false) String programName,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return R.ok(playLogService.pageWithDetails(page, size, screenName, programName, status, startTime, endTime));
    }

    /** 导出播放日志为Excel */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public void export(@RequestParam(required = false) String screenName,
                       @RequestParam(required = false) String programName,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                       HttpServletResponse response) throws IOException {
        List<PlayLog> logs = playLogService.getAllForExport(screenName, programName, status, startTime, endTime);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("播放日志_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx",
                "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);

        // 写Excel
        List<List<String>> head = Arrays.asList(
                Collections.singletonList("ID"),
                Collections.singletonList("屏幕名称"),
                Collections.singletonList("节目名称"),
                Collections.singletonList("操作"),
                Collections.singletonList("状态"),
                Collections.singletonList("详情"),
                Collections.singletonList("时间")
        );

        List<List<Object>> dataList = new ArrayList<>(logs.size());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (PlayLog log : logs) {
            List<Object> row = new ArrayList<>();
            row.add(log.getId());
            row.add(log.getScreenName() != null ? log.getScreenName() : "-");
            row.add(log.getProgramName() != null ? log.getProgramName() : "-");
            row.add(log.getAction() != null ? log.getAction() : "-");
            row.add("success".equals(log.getStatus()) ? "成功" : "失败");
            row.add(log.getMessage() != null ? log.getMessage() : "-");
            row.add(log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "-");
            dataList.add(row);
        }

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("播放日志")
                .doWrite(dataList);
    }
}
