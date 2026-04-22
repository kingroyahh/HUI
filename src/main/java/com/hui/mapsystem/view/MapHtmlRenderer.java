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

    /**
     * 将地图渲染为可交互的游戏 HTML 页面。
     * 页面通过 fetch 调用 {@code /api/state}、{@code /api/move}、{@code /api/path} 接口驱动实体交互。
     *
     * @param gameMap 游戏地图。
     * @return 可直接作为 HTTP 响应体的 HTML 字符串。
     */
    public static String renderGame(GameMap gameMap) {
        int svgWidth = GRID_PADDING * 2 + gameMap.getWidth() * CELL_SIZE + Math.max(0, gameMap.getWidth() - 1) * CELL_GAP;
        int svgHeight = GRID_PADDING * 2 + gameMap.getHeight() * CELL_SIZE + Math.max(0, gameMap.getHeight() - 1) * CELL_GAP;
        Map<SquareCoordinate, MapBuilding> buildingsByCell = indexBuildings(gameMap);

        StringBuilder cellsMarkup = new StringBuilder();
        for (int hexY = 0; hexY < gameMap.getHeight(); hexY++) {
            for (int hexX = 0; hexX < gameMap.getWidth(); hexX++) {
                MapCell mapCell = gameMap.getCell(hexX, hexY);
                appendCellMarkup(cellsMarkup, mapCell, buildingsByCell.get(new SquareCoordinate(hexX, hexY)));
            }
        }

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s — Game</title>
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
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        font-family: "Segoe UI", "PingFang SC", sans-serif;
                        color: var(--ink);
                        background:
                            radial-gradient(circle at top left, rgba(198,159,89,0.18), transparent 28%%),
                            linear-gradient(160deg, #efe8dc 0%%, #f8f4eb 46%%, #ece2d4 100%%);
                        min-height: 100vh;
                    }
                    .page { max-width: 1400px; margin: 0 auto; padding: 32px 20px 56px; }
                    .hero {
                        display: flex; flex-wrap: wrap; justify-content: space-between;
                        gap: 20px; margin-bottom: 22px;
                    }
                    .hero h1 { margin: 0 0 8px; font-size: clamp(30px,5vw,52px); line-height: 0.95; letter-spacing: -0.04em; }
                    .hero p, .legend p { margin: 0; color: var(--muted); max-width: 720px; }
                    .meta { display: grid; grid-template-columns: repeat(3,minmax(110px,1fr)); gap: 12px; min-width: min(100%%,360px); }
                    .meta-card, .panel, .legend {
                        background: var(--panel-bg); backdrop-filter: blur(12px);
                        border: 1px solid rgba(31,42,46,0.08); border-radius: 24px; box-shadow: var(--shadow);
                    }
                    .meta-card { padding: 16px 18px; }
                    .meta-card strong { display: block; font-size: 28px; line-height: 1; margin-bottom: 6px; }
                    .meta-card span { color: var(--muted); font-size: 13px; }
                    .panel { overflow: auto; padding: 18px; }
                    .preview-shell { display: grid; grid-template-columns: minmax(760px,1fr) 300px; gap: 18px; align-items: start; }
                    .map-stage {
                        background: linear-gradient(180deg,rgba(255,255,255,0.58),rgba(242,233,219,0.76));
                        border-radius: 20px; padding: 14px;
                    }
                    .game-panel {
                        position: sticky; top: 18px; padding: 18px; border-radius: 20px;
                        background: linear-gradient(180deg,rgba(255,255,255,0.9),rgba(245,236,224,0.92));
                        border: 1px solid rgba(31,42,46,0.08); display: flex; flex-direction: column; gap: 14px;
                    }
                    .game-panel h3 { margin: 0 0 10px; font-size: 17px; }
                    .info-row { padding: 9px 12px; border-radius: 12px; background: rgba(255,255,255,0.65); margin-bottom: 6px; }
                    .info-row span { display: block; color: var(--muted); font-size: 12px; margin-bottom: 3px; }
                    .info-row strong { display: block; font-size: 14px; }
                    .info-empty { color: var(--muted); font-size: 14px; padding: 10px 0; }
                    .action-log { border-top: 1px solid rgba(31,42,46,0.08); padding-top: 12px; }
                    .log-item {
                        padding: 7px 10px; border-radius: 10px; font-size: 13px;
                        background: rgba(255,255,255,0.5); margin-bottom: 6px; color: var(--ink);
                    }
                    .log-item:first-child { background: rgba(159,79,47,0.10); color: var(--accent); font-weight: 600; }
                    .game-hint {
                        font-size: 13px; color: var(--muted); padding: 10px 12px;
                        background: rgba(255,255,255,0.4); border-radius: 12px; line-height: 1.6;
                    }
                    svg { width: 100%%; height: auto; min-width: 980px; display: block; }
                    .map-cell { cursor: pointer; }
                    .map-cell .cell-outline { transition: stroke 0.15s ease, stroke-width 0.15s ease; }
                    .map-cell:hover .cell-outline { stroke: rgba(193,73,36,0.58); stroke-width: 3; }
                    .map-cell.is-selected .cell-outline { stroke: var(--selected-stroke); stroke-width: 4; }
                    .map-cell.is-selected .cell-focus { opacity: 1; }
                    .cell-focus { opacity: 0; }
                    .legend { margin-top: 18px; padding: 18px 20px; }
                    .legend h2 { margin: 0 0 12px; font-size: 18px; }
                    .legend-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(180px,1fr)); gap: 12px; }
                    .legend-item { display: flex; align-items: center; gap: 10px; color: var(--muted); font-size: 14px; }
                    .swatch { width: 14px; height: 14px; border-radius: 999px; border: 1px solid rgba(31,42,46,0.18); flex: none; }
                    @media (max-width: 900px) {
                        .page { padding-inline: 14px; }
                        .panel { padding: 10px; }
                        .preview-shell { grid-template-columns: 1fr; }
                        .game-panel { position: static; }
                        svg { min-width: 920px; }
                    }
                </style>
            </head>
            <body>
                <main class="page">
                    <section class="hero">
                        <div>
                            <p>Config-driven strategy map — interactive game</p>
                            <h1>%s</h1>
                            <p>点击单位选中，再点击目标格子移动。视野范围以黄色高亮显示，路径以蓝色预览。</p>
                        </div>
                        <div class="meta">
                            <div class="meta-card"><strong>%d x %d</strong><span>Map Size</span></div>
                            <div class="meta-card"><strong>%d</strong><span>Buildings</span></div>
                            <div class="meta-card"><strong>%d</strong><span>Cells</span></div>
                        </div>
                    </section>
                    <section class="panel">
                        <div class="preview-shell">
                            <div class="map-stage">
                                <svg viewBox="0 0 %d %d" role="img" aria-label="%s game map">
                                    %s
                                    <g id="entity-layer"></g>
                                </svg>
                            </div>
                            <aside class="game-panel">
                                <div>
                                    <h3>单位状态</h3>
                                    <div id="entity-info"><p class="info-empty">未选中单位</p></div>
                                </div>
                                <div class="action-log">
                                    <h3>行动记录</h3>
                                    <div id="action-log"><p class="info-empty">尚无行动</p></div>
                                </div>
                                <div class="game-hint">
                                    🟡 黄色 = 视野范围<br>
                                    🔵 蓝色 = 路径预览<br>
                                    点击单位选中 → 悬停查看路径 → 点击目标移动<br>
                                    再次点击已选单位取消选中
                                </div>
                            </aside>
                        </div>
                    </section>
                    <section class="legend">
                        <h2>Legend</h2>
                        <div class="legend-grid">
                            <div class="legend-item"><span class="swatch" style="background:#9ec27f"></span>Plain</div>
                            <div class="legend-item"><span class="swatch" style="background:#4f8a5b"></span>Forest / Wood</div>
                            <div class="legend-item"><span class="swatch" style="background:#8b6b4d"></span>Ore Vein</div>
                            <div class="legend-item"><span class="swatch" style="background:#5a83b6"></span>Water</div>
                            <div class="legend-item"><span class="swatch" style="background:rgba(159,79,47,0.75)"></span>Building</div>
                            <div class="legend-item"><span class="swatch" style="background:#2a7abf;border-radius:50%%"></span>Unit</div>
                        </div>
                    </section>
                </main>
                <script>
                    (() => {
                        const CELL_SIZE = %d;
                        const CELL_GAP = %d;
                        const GRID_PADDING = %d;
                        const NS = 'http://www.w3.org/2000/svg';

                        const entityLayer = document.getElementById('entity-layer');
                        const entityInfoEl = document.getElementById('entity-info');
                        const actionLogEl = document.getElementById('action-log');

                        let state = null;
                        let selectedEntityId = null;
                        let pathPreview = [];
                        let busy = false;

                        function cellCenter(hexX, hexY) {
                            return {
                                x: GRID_PADDING + hexX * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2,
                                y: GRID_PADDING + hexY * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2
                            };
                        }

                        function cellTopLeft(hexX, hexY) {
                            return {
                                x: GRID_PADDING + hexX * (CELL_SIZE + CELL_GAP),
                                y: GRID_PADDING + hexY * (CELL_SIZE + CELL_GAP)
                            };
                        }

                        async function fetchState() {
                            const res = await fetch('/api/state');
                            state = await res.json();
                            render();
                        }

                        async function requestMove(entityId, toHexX, toHexY) {
                            if (busy) return false;
                            busy = true;
                            try {
                                const res = await fetch('/api/move', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ entityId, toHexX, toHexY })
                                });
                                const result = await res.json();
                                if (result.ok) {
                                    state = result.state;
                                    appendLog(entityId + ' 移动至 (' + toHexX + ',' + toHexY + ')');
                                } else {
                                    appendLog('无法到达 (' + toHexX + ',' + toHexY + ')');
                                }
                                render();
                                return result.ok;
                            } finally {
                                busy = false;
                            }
                        }

                        async function fetchPathPreview(entityId, toHexX, toHexY) {
                            const res = await fetch(
                                '/api/path?entityId=' + encodeURIComponent(entityId) +
                                '&toHexX=' + toHexX + '&toHexY=' + toHexY
                            );
                            const result = await res.json();
                            pathPreview = result.path || [];
                            render();
                        }

                        function appendLog(msg) {
                            const first = actionLogEl.querySelector('.info-empty');
                            if (first) first.remove();
                            const item = document.createElement('div');
                            item.className = 'log-item';
                            item.textContent = msg;
                            actionLogEl.prepend(item);
                            while (actionLogEl.children.length > 6) {
                                actionLogEl.lastChild.remove();
                            }
                        }

                        function svgEl(tag, attrs) {
                            const el = document.createElementNS(NS, tag);
                            for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
                            return el;
                        }

                        function render() {
                            entityLayer.innerHTML = '';
                            if (!state) return;

                            const selectedEntity = state.entities.find(e => e.entityId === selectedEntityId);

                            // 视野范围（黄色半透明覆盖）
                            if (selectedEntity) {
                                for (const [vx, vy] of selectedEntity.visibleCells) {
                                    const { x, y } = cellTopLeft(vx, vy);
                                    entityLayer.appendChild(svgEl('rect', {
                                        x: x + 4, y: y + 4,
                                        width: CELL_SIZE - 8, height: CELL_SIZE - 8,
                                        rx: 26, fill: 'rgba(255,220,60,0.28)',
                                        'pointer-events': 'none'
                                    }));
                                }
                            }

                            // 路径预览（蓝色覆盖 + 步骤序号）
                            for (let i = 0; i < pathPreview.length; i++) {
                                const [px, py] = pathPreview[i];
                                const { x, y } = cellTopLeft(px, py);
                                const { x: cx, y: cy } = cellCenter(px, py);
                                entityLayer.appendChild(svgEl('rect', {
                                    x: x + 10, y: y + 10,
                                    width: CELL_SIZE - 20, height: CELL_SIZE - 20,
                                    rx: 20, fill: 'rgba(60,140,255,0.32)',
                                    stroke: 'rgba(30,100,220,0.55)', 'stroke-width': 2,
                                    'pointer-events': 'none'
                                }));
                                const stepText = svgEl('text', {
                                    x: cx, y: cy + 6, 'text-anchor': 'middle',
                                    'font-size': 17, fill: 'rgba(10,55,180,0.9)',
                                    'font-weight': 'bold', 'pointer-events': 'none',
                                    'font-family': 'Segoe UI, PingFang SC, sans-serif'
                                });
                                stepText.textContent = i;
                                entityLayer.appendChild(stepText);
                            }

                            // 实体（蓝色/橙色圆形）
                            for (const entity of state.entities) {
                                const { x: cx, y: cy } = cellCenter(entity.hexX, entity.hexY);
                                const isSelected = entity.entityId === selectedEntityId;

                                if (isSelected) {
                                    entityLayer.appendChild(svgEl('circle', {
                                        cx, cy, r: 36, fill: 'none',
                                        stroke: '#c14924', 'stroke-width': 3,
                                        'stroke-dasharray': '8 4', 'pointer-events': 'none'
                                    }));
                                }

                                const circle = svgEl('circle', {
                                    cx, cy, r: isSelected ? 26 : 22,
                                    fill: isSelected ? '#e05a20' : '#2a7abf',
                                    stroke: 'white', 'stroke-width': 3, cursor: 'pointer',
                                    'data-entity-id': entity.entityId
                                });
                                circle.addEventListener('click', evt => {
                                    evt.stopPropagation();
                                    if (selectedEntityId === entity.entityId) {
                                        selectedEntityId = null;
                                        pathPreview = [];
                                        renderEntityInfo(null);
                                    } else {
                                        selectedEntityId = entity.entityId;
                                        pathPreview = [];
                                        renderEntityInfo(entity);
                                    }
                                    render();
                                });
                                entityLayer.appendChild(circle);

                                const label = svgEl('text', {
                                    x: cx, y: cy + 5, 'text-anchor': 'middle',
                                    'font-size': 12, fill: 'white', 'font-weight': 'bold',
                                    'pointer-events': 'none',
                                    'font-family': 'Segoe UI, PingFang SC, sans-serif'
                                });
                                label.textContent = entity.entityId.replace('scout-', 'S');
                                entityLayer.appendChild(label);
                            }
                        }

                        function renderEntityInfo(entity) {
                            if (!entity) {
                                entityInfoEl.innerHTML = '<p class="info-empty">未选中单位</p>';
                                return;
                            }
                            entityInfoEl.innerHTML =
                                row('单位 ID', entity.entityId) +
                                row('位置', '(' + entity.hexX + ', ' + entity.hexY + ')') +
                                row('移动类型', entity.moveType) +
                                row('视野半径', entity.visionRange);
                        }

                        function row(label, value) {
                            return '<div class="info-row"><span>' + label + '</span><strong>' + value + '</strong></div>';
                        }

                        // 格子点击：移动或选中
                        document.querySelectorAll('.map-cell').forEach(cell => {
                            cell.addEventListener('click', async () => {
                                if (!selectedEntityId) return;
                                const [hexX, hexY] = cell.dataset.coordinate.split(',').map(Number);
                                const clickedEntity = state?.entities.find(e => e.hexX === hexX && e.hexY === hexY);
                                if (clickedEntity) {
                                    selectedEntityId = clickedEntity.entityId;
                                    pathPreview = [];
                                    renderEntityInfo(clickedEntity);
                                    render();
                                    return;
                                }
                                pathPreview = [];
                                const moved = await requestMove(selectedEntityId, hexX, hexY);
                                if (moved) {
                                    const updated = state?.entities.find(e => e.entityId === selectedEntityId);
                                    renderEntityInfo(updated || null);
                                    selectedEntityId = null;
                                }
                            });

                            cell.addEventListener('mouseenter', () => {
                                if (!selectedEntityId || busy) return;
                                const [hexX, hexY] = cell.dataset.coordinate.split(',').map(Number);
                                const entity = state?.entities.find(e => e.entityId === selectedEntityId);
                                if (entity && (entity.hexX !== hexX || entity.hexY !== hexY)) {
                                    fetchPathPreview(selectedEntityId, hexX, hexY);
                                }
                            });

                            cell.addEventListener('mouseleave', () => {
                                if (selectedEntityId && pathPreview.length > 0) {
                                    pathPreview = [];
                                    render();
                                }
                            });
                        });

                        fetchState();
                    })();
                </script>
            </body>
            </html>
            """.formatted(
            escapeHtml(gameMap.getTemplateId()),
            escapeHtml(gameMap.getTemplateId()),
            gameMap.getWidth(), gameMap.getHeight(),
            gameMap.getBuildings().size(),
            gameMap.getWidth() * gameMap.getHeight(),
            svgWidth, svgHeight,
            escapeHtml(gameMap.getTemplateId()),
            cellsMarkup,
            CELL_SIZE, CELL_GAP, GRID_PADDING
        );
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