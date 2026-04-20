# 地图配置说明

## 1. 文档目的

本文档说明 `src/main/resources/config/` 目录下当前地图系统使用的配置文件，重点描述：

1. 每个字段的含义。
2. 每个字段的推荐取值范围。
3. 配置文件之间的引用关系。
4. 调整配置时需要注意的校验点。

当前地图系统通过 `ConfigManager.getInstance()` 读取以下配置：

1. `terrain.json`
2. `resource-definitions.json`
3. `building-definitions.json`
4. `map-template.json`

## 2. 配置关系

当前四份配置的依赖关系如下：

1. `map-template.json` 中的 `terrainRows` 使用 `terrain.json` 中定义的 `code` 铺设地图格子。
2. `map-template.json` 中的 `buildings[].buildingId` 引用 `building-definitions.json` 中的建筑定义。
3. `terrain.json` 中的 `resourceId` 引用 `resource-definitions.json` 中的资源定义。
4. 运行时由 `ConfigManager` 先加载配置，再由 `GameMap.fromTemplate(...)` 构建地图。

如果引用的 `id` 或 `code` 不存在，地图初始化会直接失败。

## 3. terrain.json

### 3.1 顶层结构

- `terrains`：地形定义数组。

### 3.2 字段说明

#### `id`

- 含义：地形主键，供代码和回退模板逻辑引用。
- 类型：字符串。
- 取值范围：非空，且在本文件内唯一。
- 关联关系：旧模板逻辑里的 `defaultTerrainId`、`terrainOverrides[].terrainId` 会使用该字段。

#### `code`

- 含义：地图铺设使用的地形数值编码。
- 类型：整数。
- 取值范围：正整数，且在本文件内唯一。
- 关联关系：`map-template.json -> templates[].terrainRows[][]` 必须使用这里定义的编码。

#### `name`

- 含义：地形显示名称。
- 类型：字符串。
- 取值范围：非空。

#### `movementCosts`

- 含义：不同移动类型通过该地形时的消耗。
- 类型：对象，键为移动类型，值为整数。
- 当前支持键：`GROUND`、`FLYING`。
- 取值范围：值应为正整数。
- 关联关系：如果某种移动类型没有对应键，则表示该移动类型不可通过该地形。

#### `sightCost`

- 含义：视野消耗或遮挡强度预留值。
- 类型：整数。
- 取值范围：非负整数。
- 当前状态：已加载，但当前增量视野逻辑尚未消费该值。

#### `resourceId`

- 含义：该地形天然关联的资源类型。
- 类型：字符串。
- 取值范围：必须存在于 `resource-definitions.json` 中。
- 关联关系：`NONE` 表示该地形没有可采集资源。

### 3.3 编写约束

1. 可通行平地建议同时配置 `GROUND` 和 `FLYING`。
2. 地面单位不可通行的地形，应该省略 `GROUND` 键，而不是填入极大值。
3. 一旦模板开始使用某个 `code`，后续不应随意复用或改号。

## 4. resource-definitions.json

### 4.1 顶层结构

- `resources`：资源定义数组。

### 4.2 字段说明

#### `id`

- 含义：资源主键。
- 类型：字符串。
- 取值范围：非空，且在本文件内唯一。
- 关联关系：被 `terrain.json -> resourceId` 引用。

#### `name`

- 含义：资源显示名称。
- 类型：字符串。
- 取值范围：非空。

#### `category`

- 含义：资源分类，用于区分空地与可采集资源。
- 类型：字符串。
- 建议取值：`EMPTY`、`GATHERABLE`。
- 取值范围：非空。

#### `gatherPerHour`

- 含义：每小时理论采集速率。
- 类型：整数。
- 取值范围：非负整数。
- 关联关系：当 `category=EMPTY` 时应为 `0`。

#### `capacity`

- 含义：该资源点的理论总量或容量。
- 类型：整数。
- 取值范围：非负整数。
- 关联关系：当 `category=EMPTY` 时应为 `0`。

### 4.3 编写约束

1. 建议保留一个空资源哨兵项，例如 `NONE`。
2. 可采集资源应同时具备正的 `gatherPerHour` 和正的 `capacity`。

## 5. building-definitions.json

### 5.1 顶层结构

- `buildings`：建筑原型数组。

### 5.2 字段说明

#### `id`

- 含义：建筑主键。
- 类型：字符串。
- 取值范围：非空，且在本文件内唯一。
- 关联关系：被 `map-template.json -> buildings[].buildingId` 引用。

#### `code`

- 含义：建筑数值编码，便于导表和后续扩展。
- 类型：整数。
- 取值范围：正整数，且在本文件内唯一。

#### `name`

- 含义：建筑显示名称。
- 类型：字符串。
- 取值范围：非空。

#### `type`

- 含义：建筑行为类型。
- 类型：字符串。
- 当前示例：`CITY`、`WATCHTOWER`。
- 取值范围：非空。

#### `category`

- 含义：建筑内容分类，便于策划分组和平衡。
- 类型：字符串。
- 当前示例：`SETTLEMENT`、`OUTPOST`。
- 取值范围：非空。

#### `blocksMovement`

- 含义：建筑是否阻挡地面移动。
- 类型：布尔值。
- 取值范围：`true` 或 `false`。
- 关联关系：当前运行时仅对阻挡型建筑登记占格信息。

#### `durability`

- 含义：建筑耐久或结构强度预留值。
- 类型：整数。
- 取值范围：正整数。

#### `visionBonus`

- 含义：建筑附加视野预留值。
- 类型：整数。
- 取值范围：非负整数。

#### `footprint`

- 含义：建筑占地定义，相对于建筑原点的偏移集合。
- 类型：数组。
- 取值范围：至少包含一个格子。
- 关联关系：每个偏移项会叠加到 `map-template.json` 中的放置原点上。

#### `footprint[].hexX`

- 含义：相对原点的 X 偏移。
- 类型：整数。
- 取值范围：任意整数，但落地后必须仍在地图边界内。

#### `footprint[].hexY`

- 含义：相对原点的 Y 偏移。
- 类型：整数。
- 取值范围：任意整数，但落地后必须仍在地图边界内。

### 5.3 编写约束

1. 建筑完整占地在任何模板中的落点都必须保持在地图边界内。
2. 两个阻挡型建筑的最终占格不能重叠。
3. 除特殊设计外，建议把原点格 `(0, 0)` 放进 `footprint`。

## 6. map-template.json

### 6.1 顶层结构

- `templates`：地图模板数组。

### 6.2 字段说明

#### `id`

- 含义：地图模板主键。
- 类型：字符串。
- 取值范围：非空，且在本文件内唯一。
- 关联关系：由 `GameMap.fromTemplate(id)` 使用。

#### `name`

- 含义：地图模板显示名称。
- 类型：字符串。
- 取值范围：非空。

#### `width`

- 含义：地图宽度，即每一行的格子数。
- 类型：整数。
- 取值范围：正整数。
- 关联关系：必须等于 `terrainRows` 每一行的列数。

#### `height`

- 含义：地图高度，即总行数。
- 类型：整数。
- 取值范围：正整数。
- 关联关系：必须等于 `terrainRows` 的总行数。

#### `terrainRows`

- 含义：完整地图地形矩阵。
- 类型：二维整数数组。
- 取值范围：必须严格满足 `height x width`。
- 关联关系：矩阵中的每个值都必须存在于 `terrain.json -> terrains[].code`。

#### `buildings`

- 含义：地图上的建筑实例列表。
- 类型：数组。
- 取值范围：可为空；存在时每项都必须合法。
- 关联关系：每项通过 `buildingId` 引用建筑原型。

#### `buildings[].instanceId`

- 含义：模板内唯一的建筑实例标识。
- 类型：字符串。
- 取值范围：非空，且在同一模板内唯一。

#### `buildings[].buildingId`

- 含义：建筑原型标识。
- 类型：字符串。
- 取值范围：必须存在于 `building-definitions.json -> buildings[].id`。

#### `buildings[].hexX`

- 含义：建筑放置原点的 X 坐标。
- 类型：整数。
- 取值范围：叠加完整 footprint 后必须仍位于地图内。

#### `buildings[].hexY`

- 含义：建筑放置原点的 Y 坐标。
- 类型：整数。
- 取值范围：叠加完整 footprint 后必须仍位于地图内。

### 6.3 编写约束

1. 当前完整地图推荐直接使用 `terrainRows` 铺图。
2. 所有建筑实例展开 footprint 后都必须满足 `0 <= hexX < width` 且 `0 <= hexY < height`。
3. 阻挡型建筑之间不能发生占格重叠。

## 7. 改表检查清单

每次新增或修改地图配置时，建议至少检查以下内容：

1. 新增地形的 `code` 是否唯一。
2. `resourceId` 是否都能在 `resource-definitions.json` 中找到。
3. `buildingId` 是否都能在 `building-definitions.json` 中找到。
4. `terrainRows` 的行数是否等于 `height`。
5. `terrainRows` 的列数是否都等于 `width`。
6. 建筑 footprint 展开后是否越界。
7. 建筑之间是否有重叠。
8. 执行 `mvn test -Dtest=MapSystemTest` 是否通过。