package com.led.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 审计日志查询服务 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> pageQuery(int page, int size,
                                          String username, String module, String action,
                                          String startTime, String endTime) {
        StringBuilder sql = new StringBuilder("FROM t_audit_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isEmpty()) {
            sql.append(" AND username LIKE ?");
            params.add("%" + username + "%");
        }
        if (module != null && !module.isEmpty()) {
            sql.append(" AND module = ?");
            params.add(module);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append(" AND created_at >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            sql.append(" AND created_at <= ?");
            params.add(endTime + " 23:59:59");
        }

        // 总数
        String countSql = "SELECT COUNT(*) " + sql;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // 当前页数据
        int offset = (page - 1) * size;
        String dataSql = "SELECT id, user_id AS userId, username, module, action, description, ip, " +
                         "cost_time AS costTime, request_method AS requestMethod, request_url AS requestUrl, " +
                         "request_params AS requestParams, response_result AS responseResult, created_at AS createdAt " +
                         sql + " ORDER BY created_at DESC LIMIT ?, ?";
        params.add(offset);
        params.add(size);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
