package com.led.auth.controller;

import com.led.common.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 * 使用内存存储验证码答案（无 Redis 依赖），60秒过期
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    /** 验证码存储：key -> answer，60秒自动过期 */
    private static final ConcurrentHashMap<String, CaptchaEntry> CAPTCHA_STORE = new ConcurrentHashMap<>();
    private static final long CAPTCHA_TTL_MS = 60_000L; // 60秒

    static {
        // 每30秒清理过期验证码
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "captcha-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();
            CAPTCHA_STORE.entrySet().removeIf(e -> now - e.getValue().createdAt > CAPTCHA_TTL_MS);
        }, 30, 30, TimeUnit.SECONDS);
    }

    /** 生成验证码 */
    @GetMapping("/captcha")
    public R<Map<String, String>> captcha() {
        // 生成简单数学题
        int a = (int) (Math.random() * 20) + 1;
        int b = (int) (Math.random() * 20) + 1;
        int operator = (int) (Math.random() * 2); // 0=加, 1=减
        String expression;
        int answer;
        if (operator == 0) {
            expression = a + " + " + b + " = ?";
            answer = a + b;
        } else {
            // 确保结果为正
            if (a < b) { int tmp = a; a = b; b = tmp; }
            expression = a + " - " + b + " = ?";
            answer = a - b;
        }

        // 生成图片
        String base64Image = generateCaptchaImage(expression);

        // 存储
        String key = UUID.randomUUID().toString().replace("-", "");
        CAPTCHA_STORE.put(key, new CaptchaEntry(String.valueOf(answer)));

        Map<String, String> result = new java.util.HashMap<>();
        result.put("captchaKey", key);
        result.put("captchaImage", "data:image/png;base64," + base64Image);

        log.debug("生成验证码: key={}, answer={}", key, answer);
        return R.ok(result);
    }

    /** 验证验证码答案（供 AuthService 调用） */
    public static boolean verify(String key, String code) {
        if (key == null || code == null) return false;
        CaptchaEntry entry = CAPTCHA_STORE.remove(key);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createdAt > CAPTCHA_TTL_MS) return false;
        return entry.answer.equals(code.trim());
    }

    /** 生成验证码图片 */
    private String generateCaptchaImage(String text) {
        int width = 160;
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(new Color(240, 245, 255));
        g.fillRect(0, 0, width, height);

        // 干扰线
        g.setColor(new Color(180, 200, 230));
        for (int i = 0; i < 5; i++) {
            int x1 = (int) (Math.random() * width);
            int y1 = (int) (Math.random() * height);
            int x2 = (int) (Math.random() * width);
            int y2 = (int) (Math.random() * height);
            g.drawLine(x1, y1, x2, y2);
        }

        // 干扰点
        g.setColor(new Color(160, 180, 210));
        for (int i = 0; i < 30; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            g.fillOval(x, y, 2, 2);
        }

        // 文字
        g.setColor(new Color(40, 80, 160));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, (width - textWidth) / 2, (height + fm.getAscent()) / 2 - 4);

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("验证码图片生成失败", e);
            return "";
        }
    }

    /** 验证码存储条目 */
    private static class CaptchaEntry {
        final String answer;
        final long createdAt;

        CaptchaEntry(String answer) {
            this.answer = answer;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
