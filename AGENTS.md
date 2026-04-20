# [游戏项目名称] - AI 开发指令 (Java SLG)

## 1. 项目概述
这是一个使用 **Java 17+** 开发的**多人在线 SLG (策略类)** 游戏服务端。项目基于 [Netty/Mina] 实现长连接通信，核心玩法包含：大地图移动、实时战斗、联盟系统、城池建设。

- **构建工具**: [Maven / Gradle]
- **数据存储**: [MySQL + Redis / MongoDB]
- **通信协议**: [Protobuf 3 / JSON]

## 2. 核心架构约束 (极其重要)
请严格遵守以下架构设定，它们是保证游戏稳定运行的基石：

### 2.1 游戏逻辑线程模型
- **单线程 Tick 驱动**：所有游戏核心逻辑（地图格子计算、战斗、资源增长）**必须**运行在 `LogicThread` 单线程内。
- **严禁并发**：严禁在 `Tick` 循环或处理玩家请求的逻辑中使用 `parallelStream()` 或手动创建线程修改共享游戏数据。
- **异步操作规范**：数据库访问、HTTP 调用、推送消息**必须**封装为 `Runnable` 投递到独立的 `ExecutorService` 中执行，避免阻塞主逻辑线程。

### 2.2 实体与组件系统 (ECS 模式)
- **禁止深层继承**：**禁止**出现 `PlayerSoldier extends Soldier extends BaseUnit` 这种超过 2 层的继承关系。
- **使用组合**：采用 **ECS (Entity-Component-System)** 理念或**接口组合**。
    - 示例：`Entity` 持有 `MoveComponent`、`FightComponent`、`PropertyComponent`。
    - 新增功能时，优先考虑**新增 Component**，而不是修改基类。

### 2.3 数据配置规范
- **代码与配置分离**：任何玩法数值（兵种攻击力、建筑升级时间、地图格子属性）**严禁**硬编码在 Java 文件中。
- **配置来源**：所有配置定义在 `src/main/resources/config/` 下的 `.xlsx` 或 `.json` 文件中。
- **配置文档**：配置文件必须附带详细说明文档，解释每个字段的含义、取值范围、与其他字段的关系。（用中文）
- **读取方式**：必须通过 `ConfigManager.getInstance().getXXX(id)` 获取配置对象。

### 2.4 内存敏感
- **地图相关**： 地图格子对象是复用池管理的，严禁在循环中 new MapCell()，必须调用 MapGridPool.obtain()

## 3. 常用命令 (Build & Test)
AI 执行任务时，请使用以下精确命令：

| 场景 | 命令 (Maven) | 命令 (Gradle) |
| :--- | :--- | :--- |
| **编译协议文件** | `mvn generate-sources` | `gradle generateProto` |
| **运行所有单元测试** | `mvn test` | `gradle test` |
| **运行单个测试类** | `mvn test -Dtest=MoveTest` | `gradle test --tests MoveTest` |
| **打包可运行 Jar** | `mvn package -Pgame` | `gradle shadowJar` |
| **启动本地开发服** | `java -jar target/server.jar --env=dev` | `gradle run` |

## 4. 代码风格速查 (Java)
- **注释**：所有公共方法必须有 Javadoc 注释，解释方法功能、参数含义、返回值意义、异常情况。
- **命名**：地图格子 X/Y 坐标变量命名为 `hexX`, `hexY`，严禁使用 `x1`, `x2` 等无意义名称。
- **注解**：新增玩家请求处理器必须使用 `@MsgHandler(cmd = CmdEnum.MOVE)` 注解。

## 5. 引用文件
@docs/protocol-definition.md      # Protobuf 协议详细定义规范
@docs/excel-config-guide.md       # Excel 导表规则说明

## 6. 补充
-- 允许所有Command执行