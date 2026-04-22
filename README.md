# HUI

基于 Java 25 LTS 的地图系统示例工程，当前实现了配置驱动的地图构建、A* 寻路、建筑占格、资源地形绑定、增量视野计算以及地图 HTML 预览。

## 功能概览

- 通过 `ConfigManager` 统一加载 `src/main/resources/config/` 下的地图配置。
- 通过 `GameMap.fromTemplate(...)` 按模板构建运行时地图。
- 通过 `PathFinder.findPath(...)` 执行地面或飞行单位寻路。
- 通过 `IncrementalVisionService` 计算单位移动后的视野增量。
- 通过 `MapGridPool.obtain()` 复用地图格子对象，避免在地图构建循环中频繁创建对象。
- 通过 `MapPreviewExporter` 导出地图 HTML 可视化文件。
- 通过 `MapPreviewServer` 启动本地 HTTP 服务实时预览地图。

## 环境要求

- JDK 25 LTS 或更高版本
- Maven 3.9 或兼容版本（推荐 Maven 4.x）

## 项目结构

- `src/main/java/com/hui/mapsystem/config/`：配置模型与配置加载器
- `src/main/java/com/hui/mapsystem/model/`：地图、格子、建筑、实体与组件
- `src/main/java/com/hui/mapsystem/path/`：A* 寻路实现与调用入口
- `src/main/java/com/hui/mapsystem/vision/`：增量视野计算
- `src/main/java/com/hui/mapsystem/view/`：地图 HTML 渲染、预览导出与本地预览服务
- `src/main/resources/config/`：地图系统配置文件
- `src/test/java/com/hui/mapsystem/`：聚焦测试用例
- `docs/excel-config-guide.md`：中文配置说明文档

## 快速开始

### 1. 编译项目

```bash
mvn compile
```

### 2. 运行测试

运行全部测试：

```bash
mvn test
```

只运行地图系统测试：

```bash
mvn test -Dtest=MapSystemTest
```

### 3. 导出地图 HTML 预览

将指定地图模板渲染为静态 HTML 文件，输出到 `target/map-preview/<templateId>.html`：

```bash
mvn exec:java -Dexec.mainClass="com.hui.mapsystem.view.MapPreviewExporter" -Dexec.args="starter-map"
```

### 4. 启动地图预览服务

启动本地 HTTP 服务，在浏览器中实时预览地图（默认端口 8765）：

```bash
mvn exec:java -Dexec.mainClass="com.hui.mapsystem.view.MapPreviewServer" -Dexec.args="starter-map 8765"
```

启动后访问：`http://127.0.0.1:8765/`

### 5. 打包

```bash
mvn package
```

## 当前使用方式

这个仓库当前是地图系统模块示例，主要使用方式是：

1. 通过测试验证功能行为。
2. 在业务代码中调用现有 API 集成地图能力。
3. 通过修改 `src/main/resources/config/` 下的配置文件调整地图内容。
4. 通过 `MapPreviewExporter` 或 `MapPreviewServer` 可视化调试地图布局。

下面是典型调用路径：

```java
GameMap gameMap = GameMap.fromTemplate("starter-map");
PathFinder.bind(gameMap);

List<SquareCoordinate> path = PathFinder.findPath(
	new SquareCoordinate(0, 0),
	new SquareCoordinate(4, 0),
	MoveType.GROUND
);
```

增量视野计算示例：

```java
MapEntity scout = new MapEntity("scout-1", new SquareCoordinate(1, 1), new MoveComponent(MoveType.GROUND), new VisionComponent(1));
IncrementalVisionService visionService = new IncrementalVisionService();

VisionDelta initialDelta = visionService.updateVision(gameMap, scout);
scout.setPosition(new SquareCoordinate(2, 1));
VisionDelta movedDelta = visionService.updateVision(gameMap, scout);
```

## 配置说明

当前地图系统使用以下配置文件：

- `terrain.json`：地形定义、移动消耗、资源绑定
- `resource-definitions.json`：资源类型与采集参数
- `building-definitions.json`：建筑原型、占地和视野加成等参数
- `map-template.json`：地图模板、地形矩阵、建筑落点

详细字段说明、取值范围和配置关联关系见 `docs/excel-config-guide.md`。

## 已验证能力

当前测试覆盖了以下核心行为：

- 配置驱动的资源和建筑加载
- 建筑占格后的寻路绕行
- 单位移动后的增量视野变更

## 后续扩展建议

- 将视野遮挡规则与 `sightCost` 真正联动
- 在模板加载阶段增加更严格的配置校验与错误提示
- 升级 Maven 至 4.x（当前使用 3.9，Java 25 推荐 4.0+）

## 版本历史

| 版本 | 变更内容 |
|------|----------|
| 当前 | **地图功能迭代**：新增 FOOD（粮食矿/farmland）和 STONE（石头矿/quarry）地形与资源类型；新增建筑归属（`ownerId`）字段，同归属方单位可穿越己方建筑，`AStarPathFinder` / `PathFinder` 新增归属感知寻路重载；新增 `GameMap.removeBuilding()` 与 `isResourceCovered()` 支持建筑覆盖/恢复资源；`starter-map` 补充粮食地和石矿格子；新增 3 项回归测试，全量 6/6 通过；新增 `GameSession` + 改造 `MapPreviewServer` 为可交互游戏入口（实体移动、视野高亮、路径预览） |
| v1.1 | 升级 Java 运行时至 25 LTS；新增地图 HTML 预览（`MapHtmlRenderer`、`MapPreviewExporter`、`MapPreviewServer`）；升级 `maven-compiler-plugin` 至 3.13.0，`maven-surefire-plugin` 至 3.5.0 |
| v1.0 | 地图系统核心实现：配置加载、A* 寻路、建筑占格、增量视野 |
