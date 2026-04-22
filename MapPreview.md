# Map Preview 使用手册

本文档说明如何在当前工程中生成并查看地图可视化预览。

## 1. 功能说明

当前地图预览能力由以下几个部分组成：

- `MapHtmlRenderer`：将 `GameMap` 渲染为 HTML + SVG 页面。
- `MapPreviewExporter`：将指定地图模板导出为静态 HTML 文件。
- `MapPreviewServer`：启动本地 HTTP 服务，在浏览器中查看地图页面。

地图预览当前会展示这些信息：

- 地形类型
- 地面移动代价
- 资源点信息
- 建筑占格信息
- 格子坐标

## 2. 环境要求

- JDK 17 或更高版本
- Maven 3.9 或兼容版本

## 3. 相关代码位置

- `src/main/java/com/hui/mapsystem/view/MapHtmlRenderer.java`
- `src/main/java/com/hui/mapsystem/view/MapPreviewExporter.java`
- `src/main/java/com/hui/mapsystem/view/MapPreviewServer.java`
- `src/test/java/com/hui/mapsystem/MapSystemTest.java`

## 4. 先编译项目

在仓库根目录执行：

```bash
mvn compile
```

## 5. 方式一：导出静态 HTML 文件

如果你只想生成页面文件，不启动服务，执行：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewExporter
```

默认会导出 `starter-map` 模板。

导出成功后，文件位置为：

```text
target/map-preview/starter-map.html
```

如果后续你想指定别的模板，可以给程序传模板参数。当前项目已经支持命令行参数，示例：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewExporter -Dexec.args="starter-map"
```

## 6. 方式二：启动本地预览服务

如果你希望直接在浏览器里访问地图页面，执行：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewServer
```

默认行为：

- 地图模板：`starter-map`
- 服务端口：`8765`

启动成功后，终端会输出类似内容：

```text
Map preview server started: http://127.0.0.1:8765/
Serving template: starter-map
```

然后在浏览器中打开：

```text
http://127.0.0.1:8765/
```

## 7. 自定义模板和端口

`MapPreviewServer` 支持两个参数：

- 第一个参数：地图模板 id
- 第二个参数：服务端口

例如启动 `starter-map`，端口改为 `8767`：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewServer -Dexec.args="starter-map 8767"
```

浏览器访问：

```text
http://127.0.0.1:8767/
```

## 8. 推荐使用方式

建议优先使用本地 HTTP 服务，不建议直接依赖 `file://` 打开导出的 HTML 文件。

原因：

- 某些环境里 `file://` 路径可能出现 `ERR_FILE_NOT_FOUND`
- HTTP 方式更稳定，路径也更明确
- 后续如果要加交互层、接口层或动态叠加信息，也更容易扩展

## 9. 验证方式

当前地图预览相关实现已经通过以下测试验证：

```bash
mvn test -Dtest=MapSystemTest
```

测试覆盖了：

- 配置驱动的资源和建筑加载
- 建筑占格后的寻路绕行
- 增量视野计算
- 地图 HTML 预览导出

## 10. 常见问题

### 10.1 浏览器打开本地 HTML 报 `ERR_FILE_NOT_FOUND`

不要优先使用本地文件协议。

请改用：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewServer
```

然后访问：

```text
http://127.0.0.1:8765/
```

### 10.2 直接执行 `java -cp target/classes ...` 启动失败

不建议这样启动，因为运行时依赖里包含 Jackson，单独使用 `target/classes` 不会自动把依赖包加进类路径。

请使用 Maven 执行：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewServer
```

或：

```bash
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewExporter
```

### 10.3 页面内容没有变化

如果你修改了：

- `src/main/resources/config/map-template.json`
- `src/main/resources/config/terrain.json`
- `src/main/resources/config/building-definitions.json`
- `src/main/resources/config/resource-definitions.json`

请重新执行导出或重启预览服务。

建议流程：

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.hui.mapsystem.view.MapPreviewServer
```

## 11. 当前限制

当前预览页是静态地图可视化，主要用于调试和确认地图配置，不包含以下高级能力：

- 点击交互
- 寻路轨迹叠加
- 视野迷雾叠加
- 动态实体移动演示

这些能力如果后续需要，可以继续在现有预览服务基础上扩展。