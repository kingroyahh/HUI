package com.hui.mapsystem.view;

import com.hui.mapsystem.config.BuildingConfig;
import com.hui.mapsystem.config.ConfigManager;
import com.hui.mapsystem.config.ResourceConfig;
import com.hui.mapsystem.config.TerrainConfig;
import com.hui.mapsystem.model.GameMap;
import com.hui.mapsystem.model.MapBuilding;
import com.hui.mapsystem.model.MapCell;
import com.hui.mapsystem.model.SquareCoordinate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 将地图对象导出为可直接打开的 HTML 预览页。
 */
public final class MapHtmlRenderer {
    private static final int CELL_SIZE = 132;
    private static final int CELL_GAP = 12;
    private static final int GRID_PADDING = 48;

    private MapHtmlRenderer() {
    }

    /**
     * 将地图渲染为完整 HTML 文本。
     *
     * @param gameMap 待渲染地图。
     * @return 可直接保存为 .html 的页面字符串。
     */
    public static String render(GameMap gameMap) {
        int svgWidth = GRID_PADDING * 2 + gameMap.getWidth() * CELL_SIZE + Math.max(0, gameMap.getWidth() - 1) * CELL_GAP;
        int svgHeight = GRID_PADDING * 2 + gameMap.getHeight() * CELL_SIZE + Math.max(0, gameMap.getHeight() - 1) * CELL_GAP;
        Map<SquareCoordinate, MapBuilding> buildingsByCell = indexBuildings(gameMap);

        StringBuilder cellsMarkup = new StringBuilder();
        String initialSelectedCellId = null;
        for (int hexY = 0; hexY < gameMap.getHeight(); hexY++) {
            for (int hexX = 0; hexX < gameMap.getWidth(); hexX++) {
                MapCell mapCell = gameMap.getCell(hexX, hexY);
                if (initialSelectedCellId == null) {
                    initialSelectedCellId = cellId(mapCell);
                }
                appendCellMarkup(cellsMarkup, mapCell, buildingsByCell.get(new SquareCoordinate(hexX, hexY)));
            }
        }

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s Preview</title>
                <style>
                    :root {
                        --page-bg: #f4efe6;
                        --panel-bg: rgba(255, 252, 246, 0.92);
                        --ink: #1f2a2e;
                        --muted: #5d6a6f;
                        --accent: #9f4f2f;
                        --accent-soft: rgba(159, 79, 47, 0.14);
                        --grid-stroke: rgba(31, 42, 46, 0.18);
                        --building-stroke: rgba(31, 42, 46, 0.44);
                        --selected-stroke: #c14924;
                        --shadow: 0 20px 50px rgba(82, 53, 30, 0.14);
                    }

                    * {
                        box-sizing: border-box;
                    }

                    body {
                        margin: 0;
                        font-family: "Segoe UI", "PingFang SC", sans-serif;
                        color: var(--ink);
                        background:
                            radial-gradient(circle at top left, rgba(198, 159, 89, 0.18), transparent 28%%),
                            linear-gradient(160deg, #efe8dc 0%%, #f8f4eb 46%%, #ece2d4 100%%);
                        min-height: 100vh;
                    }

                    .page {
                        max-width: 1400px;
                        margin: 0 auto;
                        padding: 32px 20px 56px;
                    }

                    .hero {
                        display: flex;
                        flex-wrap: wrap;
                        justify-content: space-between;
                        gap: 20px;
                        margin-bottom: 22px;
                    }

                    .hero h1 {
                        margin: 0 0 8px;
                        font-size: clamp(30px, 5vw, 52px);
                        line-height: 0.95;
                        letter-spacing: -0.04em;
                    }

                    .hero p,
                    .legend p {
                        margin: 0;
                        color: var(--muted);
                        max-width: 720px;
                    }

                    .meta {
                        display: grid;
                        grid-template-columns: repeat(3, minmax(110px, 1fr));
                        gap: 12px;
                        min-width: min(100%%, 360px);
                    }

                    .meta-card,
                    .panel,
                    .legend {
                        background: var(--panel-bg);
                        backdrop-filter: blur(12px);
                        border: 1px solid rgba(31, 42, 46, 0.08);
                        border-radius: 24px;
                        box-shadow: var(--shadow);
                    }

                    .meta-card {
                        padding: 16px 18px;
                    }

                    .meta-card strong {
                        display: block;
                        font-size: 28px;
                        line-height: 1;
                        margin-bottom: 6px;
                    }

                    .meta-card span {
                        color: var(--muted);
                        font-size: 13px;
                    }

                    .panel {
                        overflow: auto;
                        padding: 18px;
                    }

                    .preview-shell {
                        display: grid;
                        grid-template-columns: minmax(760px, 1fr) 320px;
                        gap: 18px;
                        align-items: start;
                    }

                    .map-stage {
                        background: linear-gradient(180deg, rgba(255,255,255,0.58), rgba(242, 233, 219, 0.76));
                        border-radius: 20px;
                        padding: 14px;
                    }

                    .inspector {
                        position: sticky;
                        top: 18px;
                        padding: 18px;
                        border-radius: 20px;
                        background: linear-gradient(180deg, rgba(255,255,255,0.9), rgba(245, 236, 224, 0.92));
                        border: 1px solid rgba(31, 42, 46, 0.08);
                    }

                    .inspector h3 {
                        margin: 0 0 12px;
                        font-size: 18px;
                    }

                    .inspector-grid {
                        display: grid;
                        gap: 10px;
                    }

                    .inspector-row {
                        padding: 10px 12px;
                        border-radius: 14px;
                        background: rgba(255, 255, 255, 0.65);
                    }

                    .inspector-row span {
                        display: block;
                        color: var(--muted);
                        font-size: 12px;
                        margin-bottom: 4px;
                    }

                    .inspector-row strong {
                        display: block;
                        font-size: 15px;
                    }

                    .inspector-tip {
                        margin-top: 14px;
                        font-size: 13px;
                        color: var(--muted);
                    }

                    svg {
                        width: 100%%;
                        height: auto;
                        min-width: 980px;
                        display: block;
                    }

                    .map-cell {
                        cursor: pointer;
                    }

                    .map-cell .cell-outline {
                        transition: stroke 0.18s ease, stroke-width 0.18s ease, transform 0.18s ease;
                    }

                    .map-cell:hover .cell-outline {
                        stroke: rgba(193, 73, 36, 0.58);
                        stroke-width: 3;
                    }

                    .map-cell.is-selected .cell-outline {
                        stroke: var(--selected-stroke);
                        stroke-width: 4;
                    }

                    .map-cell.is-selected .cell-focus {
                        opacity: 1;
                    }

                    .cell-focus {
                        opacity: 0;
                    }

                    .legend {
                        margin-top: 18px;
                        padding: 18px 20px;
                    }

                    .legend h2 {
                        margin: 0 0 12px;
                        font-size: 18px;
                    }

                    .legend-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                        gap: 12px;
                    }

                    .legend-item {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        color: var(--muted);
                        font-size: 14px;
                    }

                    .swatch {
                        width: 14px;
                        height: 14px;
                        border-radius: 999px;
                        border: 1px solid rgba(31, 42, 46, 0.18);
                        flex: none;
                    }

                    @media (max-width: 900px) {
                        .page {
                            padding-inline: 14px;
                        }

                        .panel {
                            padding: 10px;
                        }

                        .preview-shell {
                            grid-template-columns: 1fr;
                        }

                        .inspector {
                            position: static;
                        }

                        svg {
                            min-width: 920px;
                        }
                    }
                </style>
            </head>
            <body>
                <main class="page">
                    <section class="hero">
                        <div>
                            <p>Config-driven strategy map preview</p>
                            <h1>%s</h1>
                            <p>每个格子显示地形、移动代价、资源和建筑占格信息，直接对应当前 Java 地图对象。</p>
                        </div>
                        <div class="meta">
                            <div class="meta-card">
                                <strong>%d x %d</strong>
                                <span>Map Size</span>
                            </div>
                            <div class="meta-card">
                                <strong>%d</strong>
                                <span>Buildings</span>
                            </div>
                            <div class="meta-card">
                                <strong>%d</strong>
                                <span>Cells</span>
                            </div>
                        </div>
                    </section>

                    <section class="panel">
                        <div class="preview-shell">
                            <div class="map-stage">
                                <svg viewBox="0 0 %d %d" role="img" aria-label="%s map preview">
                                    %s
                                </svg>
                            </div>
                            <aside class="inspector">
                                <h3>Cell Inspector</h3>
                                <div class="inspector-grid">
                                    <div class="inspector-row"><span>坐标</span><strong id="cell-coordinate">-</strong></div>
                                    <div class="inspector-row"><span>地形</span><strong id="cell-terrain">-</strong></div>
                                    <div class="inspector-row"><span>资源</span><strong id="cell-resource">-</strong></div>
                                    <div class="inspector-row"><span>建筑</span><strong id="cell-building">-</strong></div>
                                    <div class="inspector-row"><span>地面移动代价</span><strong id="cell-movement">-</strong></div>
                                    <div class="inspector-row"><span>地面通行</span><strong id="cell-walkable">-</strong></div>
                                </div>
                                <p class="inspector-tip">点击任意格子查看完整信息。格内只保留摘要，避免文字重叠。</p>
                            </aside>
                        </div>
                    </section>

                    <section class="legend">
                        <h2>Legend</h2>
                        <div class="legend-grid">
                            <div class="legend-item"><span class="swatch" style="background:#9ec27f"></span>Plain</div>
                            <div class="legend-item"><span class="swatch" style="background:#4f8a5b"></span>Forest / Wood</div>
                            <div class="legend-item"><span class="swatch" style="background:#8b6b4d"></span>Ore Vein / Ore</div>
                            <div class="legend-item"><span class="swatch" style="background:#5a83b6"></span>Water</div>
                            <div class="legend-item"><span class="swatch" style="background:rgba(159,79,47,0.75)"></span>Building Footprint</div>
                        </div>
                        <p>左上角为坐标，中央为地形简称，右下角为地面移动代价，底部为资源或建筑标签。</p>
                    </section>
                </main>
                <script>
                    (() => {
                        const cells = Array.from(document.querySelectorAll('.map-cell'));
                        const inspector = {
                            coordinate: document.getElementById('cell-coordinate'),
                            terrain: document.getElementById('cell-terrain'),
                            resource: document.getElementById('cell-resource'),
                            building: document.getElementById('cell-building'),
                            movement: document.getElementById('cell-movement'),
                            walkable: document.getElementById('cell-walkable')
                        };

                        function selectCell(cell) {
                            cells.forEach((item) => item.classList.remove('is-selected'));
                            cell.classList.add('is-selected');
                            inspector.coordinate.textContent = cell.dataset.coordinate;
                            inspector.terrain.textContent = cell.dataset.terrain;
                            inspector.resource.textContent = cell.dataset.resource;
                            inspector.building.textContent = cell.dataset.building;
                            inspector.movement.textContent = cell.dataset.movement;
                            inspector.walkable.textContent = cell.dataset.walkable;
                        }

                        cells.forEach((cell) => {
                            cell.addEventListener('click', () => selectCell(cell));
                        });

                        const initialCell = document.getElementById('%s');
                        if (initialCell) {
                            selectCell(initialCell);
                        }
                    })();
                </script>
            </body>
            </html>
            """.formatted(
            escapeHtml(gameMap.getTemplateId()),
            escapeHtml(gameMap.getTemplateId()),
            gameMap.getWidth(),
            gameMap.getHeight(),
            gameMap.getBuildings().size(),
            gameMap.getWidth() * gameMap.getHeight(),
            svgWidth,
            svgHeight,
            escapeHtml(gameMap.getTemplateId()),
            cellsMarkup,
            initialSelectedCellId
        );
    }

    /**
     * 将地图导出为 HTML 文件。
     *
     * @param gameMap 待导出地图。
     * @param outputPath 输出文件路径。
     * @return 输出文件路径。
     */
    public static Path export(GameMap gameMap, Path outputPath) {
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, render(gameMap), StandardCharsets.UTF_8);
            return outputPath;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to export map preview to " + outputPath, exception);
        }
    }

    private static void appendCellMarkup(StringBuilder markup, MapCell mapCell, MapBuilding mapBuilding) {
        int left = GRID_PADDING + mapCell.getHexX() * (CELL_SIZE + CELL_GAP);
        int top = GRID_PADDING + mapCell.getHexY() * (CELL_SIZE + CELL_GAP);
        TerrainConfig terrainConfig = ConfigManager.getInstance().getTerrain(mapCell.getTerrainId());
        String resourceLabel = resourceLabel(mapCell);
        String buildingLabel = mapBuildingLabel(mapBuilding);

        markup.append("<g class=\"map-cell\" id=\"").append(cellId(mapCell)).append("\"")
            .append(" data-coordinate=\"").append(attribute(mapCell.getHexX() + "," + mapCell.getHexY())).append("\"")
            .append(" data-terrain=\"").append(attribute(terrainConfig.getName())).append("\"")
            .append(" data-resource=\"").append(attribute(resourceLabel)).append("\"")
            .append(" data-building=\"").append(attribute(buildingLabel)).append("\"")
            .append(" data-movement=\"").append(attribute(String.valueOf(mapCell.getMovementCost()))).append("\"")
            .append(" data-walkable=\"").append(attribute(mapCell.isWalkable(com.hui.mapsystem.model.MoveType.GROUND) ? "Yes" : "No")).append("\">");
        markup.append("<rect x=\"").append(left).append("\" y=\"").append(top)
            .append("\" width=\"").append(CELL_SIZE).append("\" height=\"").append(CELL_SIZE)
            .append("\" rx=\"30\" fill=\"").append(terrainColor(terrainConfig.getId()))
            .append("\" stroke=\"var(--grid-stroke)\" stroke-width=\"1.5\" class=\"cell-outline\" />");

        markup.append("<rect x=\"").append(left + 6).append("\" y=\"").append(top + 6)
            .append("\" width=\"").append(CELL_SIZE - 12).append("\" height=\"").append(CELL_SIZE - 12)
            .append("\" rx=\"24\" fill=\"var(--accent-soft)\" class=\"cell-focus\" />");

        if (mapBuilding != null) {
            markup.append("<rect x=\"").append(left + 10).append("\" y=\"").append(top + 10)
                .append("\" width=\"").append(CELL_SIZE - 20).append("\" height=\"").append(CELL_SIZE - 20)
                .append("\" rx=\"24\" fill=\"rgba(159,79,47,0.75)\" stroke=\"var(--building-stroke)\" stroke-width=\"2.5\" />");
        }

        markup.append(text(left + 14, top + 22, 13, "rgba(31,42,46,0.72)", mapCell.getHexX() + "," + mapCell.getHexY()));
        markup.append(text(left + 16, top + 52, 20, "#122227", shortTerrainLabel(terrainConfig)));
        markup.append(text(left + 16, top + 78, 13, "rgba(18,34,39,0.76)", shortResourceLabel(mapCell)));
        markup.append(text(left + 16, top + 100, 13, "rgba(18,34,39,0.76)", shortBuildingLabel(mapBuilding)));
        markup.append(text(left + CELL_SIZE - 16, top + CELL_SIZE - 18, 13, "rgba(18,34,39,0.78)", "MP " + mapCell.getMovementCost(), "end"));
        markup.append("</g>");
    }

    private static String cellId(MapCell mapCell) {
        return "cell-" + mapCell.getHexX() + "-" + mapCell.getHexY();
    }

    private static String mapBuildingLabel(MapBuilding mapBuilding) {
        if (mapBuilding == null) {
            return "Open Tile";
        }
        BuildingConfig buildingConfig = mapBuilding.getBuildingConfig();
        return buildingConfig.getName();
    }

    private static String resourceLabel(MapCell mapCell) {
        String resourceType = mapCell.getResourceType();
        if (resourceType == null || "NONE".equals(resourceType)) {
            return mapCell.isWalkable(com.hui.mapsystem.model.MoveType.GROUND) ? "Walkable" : "Blocked";
        }
        ResourceConfig resourceConfig = ConfigManager.getInstance().getResource(resourceType);
        return "Resource: " + resourceConfig.getName();
    }

    private static String shortTerrainLabel(TerrainConfig terrainConfig) {
        return switch (terrainConfig.getId()) {
            case "plain" -> "PLAIN";
            case "forest" -> "FOREST";
            case "mine" -> "ORE";
            case "water" -> "WATER";
            default -> terrainConfig.getName();
        };
    }

    private static String shortResourceLabel(MapCell mapCell) {
        String resourceType = mapCell.getResourceType();
        if (resourceType == null || "NONE".equals(resourceType)) {
            return mapCell.isWalkable(com.hui.mapsystem.model.MoveType.GROUND) ? "PATH OPEN" : "BLOCKED";
        }
        return "RES " + resourceType;
    }

    private static String shortBuildingLabel(MapBuilding mapBuilding) {
        if (mapBuilding == null) {
            return "NO BUILDING";
        }
        return switch (mapBuilding.getBuildingConfig().getId()) {
            case "city" -> "BLDG CITY";
            case "watchtower" -> "BLDG TOWER";
            default -> "BLDG " + mapBuilding.getBuildingConfig().getId().toUpperCase();
        };
    }

    private static Map<SquareCoordinate, MapBuilding> indexBuildings(GameMap gameMap) {
        Map<SquareCoordinate, MapBuilding> buildingsByCell = new HashMap<>();
        for (MapBuilding mapBuilding : gameMap.getBuildings()) {
            for (SquareCoordinate occupiedCell : mapBuilding.getOccupiedCells()) {
                buildingsByCell.put(occupiedCell, mapBuilding);
            }
        }
        return buildingsByCell;
    }

    private static String terrainColor(String terrainId) {
        return switch (terrainId) {
            case "forest" -> "#4f8a5b";
            case "mine" -> "#8b6b4d";
            case "water" -> "#5a83b6";
            case "plain" -> "#9ec27f";
            default -> "#c8c3b5";
        };
    }

    private static String text(int x, int y, int size, String color, String content) {
        return text(x, y, size, color, content, "start");
    }

    private static String text(int x, int y, int size, String color, String content, String anchor) {
        return "<text x=\"" + x + "\" y=\"" + y + "\" font-size=\"" + size
            + "\" fill=\"" + color + "\" text-anchor=\"" + anchor
            + "\" font-family=\"Segoe UI, PingFang SC, sans-serif\">"
            + escapeHtml(content) + "</text>";
    }

    private static String attribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }

    private static String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}