package com.led.content.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** 缩略图生成工具 */
@Slf4j
public class ThumbnailUtil {

    /** 生成缩略图 */
    public static void generateThumbnail(String sourcePath, String targetPath, int width, int height) throws IOException {
        File sourceFile = new File(sourcePath);
        BufferedImage sourceImage = ImageIO.read(sourceFile);
        if (sourceImage == null) {
            throw new IOException("无法读取图片: " + sourcePath);
        }

        // 计算等比例缩放
        int srcW = sourceImage.getWidth();
        int srcH = sourceImage.getHeight();
        double ratio = Math.min((double) width / srcW, (double) height / srcH);
        int newW = (int) (srcW * ratio);
        int newH = (int) (srcH * ratio);

        Image scaledImage = sourceImage.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        String format = "jpg";
        if (targetPath.endsWith(".png")) format = "png";
        else if (targetPath.endsWith(".gif")) format = "gif";

        ImageIO.write(thumbnail, format, new File(targetPath));
        log.debug("缩略图生成成功: {}", targetPath);
    }
}
