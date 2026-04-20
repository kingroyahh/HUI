# HUI

基于 Java 17 的地图系统示例工程，当前实现了配置驱动的地图构建、A* 寻路、建筑占格、资源地形绑定以及增量视野计算。

## 功能概览

- 通过 `ConfigManager` 统一加载 `src/main/resources/config/` 下的地图配置。
- 通过 `GameMap.fromTemplate(...)` 按模板构建运行时地图。
- 通过 `PathFinder.findPath(...)` 执行地面或飞行单位寻路。
- 通过 `IncrementalVisionService` 计算单位移动后的视野增量。
- 通过 `MapGridPool.obtain()` 复用地图格子对象，避免在地图构建循环中频繁创建对象。

## 环境要求

- JDK 17 或更高版本
- Maven 3.9 或兼容版本

## 项目结构

- `src/main/java/com/hui/mapsystem/config/`：配置模型与配置加载器
- `src/main/java/com/hui/mapsystem/model/`：地图、格子、建筑、实体与组件
- `src/main/java/com/hui/mapsystem/path/`：A* 寻路实现与调用入口
- `src/main/java/com/hui/mapsystem/vision/`：增量视野计算
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

### 3. 打包

```bash
mvn package
```

## 当前使用方式

这个仓库当前是地图系统模块示例，还没有提供独立的启动类或服务端入口。主要使用方式是：

1. 通过测试验证功能行为。
2. 在业务代码中调用现有 API 集成地图能力。
3. 通过修改 `src/main/resources/config/` 下的配置文件调整地图内容。

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

- 增加独立的示例启动入口，方便手工调试地图行为
- 将视野遮挡规则与 `sightCost` 真正联动
- 在模板加载阶段增加更严格的配置校验与错误提示
