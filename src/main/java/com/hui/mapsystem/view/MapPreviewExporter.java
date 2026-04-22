package com.hui.mapsystem.view;

import com.hui.mapsystem.model.GameMap;

import java.nio.file.Path;

/**
 * 地图预览导出入口，用于显式生成 HTML 可视化文件。
 */
public final class MapPreviewExporter {
    private MapPreviewExporter() {
    }

    /**
     * 生成指定地图模板的 HTML 预览。
     *
     * @param args 第一个参数可选，表示模板 id，默认 starter-map。
     */
    public static void main(String[] args) {
        String templateId = args.length > 0 ? args[0] : "starter-map";
        GameMap gameMap = GameMap.fromTemplate(templateId);
        Path outputPath = Path.of("target", "map-preview", templateId + ".html");
        Path exportedPath = MapHtmlRenderer.export(gameMap, outputPath);
        System.out.println("Map preview exported to: " + exportedPath.toAbsolutePath());
    }
}