package com.hui.mapsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 地图配置管理器，负责统一加载并索引地图相关配置。
 */
public final class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();

    private final Map<String, TerrainConfig> terrains;
    private final Map<Integer, TerrainConfig> terrainsByCode;
    private final Map<String, ResourceConfig> resources;
    private final Map<String, BuildingConfig> buildings;
    private final Map<String, MapTemplateConfig> templates;

    private ConfigManager() {
        ObjectMapper objectMapper = new ObjectMapper();
        terrains = loadCatalog(objectMapper, "/config/terrain.json", TerrainCatalog.class, TerrainCatalog::getTerrains);
        terrainsByCode = terrains.values().stream().collect(Collectors.toUnmodifiableMap(TerrainConfig::getCode, Function.identity()));
        resources = loadCatalog(objectMapper, "/config/resource-definitions.json", ResourceCatalog.class, ResourceCatalog::getResources);
        buildings = loadCatalog(objectMapper, "/config/building-definitions.json", BuildingCatalog.class, BuildingCatalog::getBuildings);
        templates = loadCatalog(objectMapper, "/config/map-template.json", TemplateCatalog.class, TemplateCatalog::getTemplates);
    }

    /**
     * 返回全局配置管理器实例。
     *
     * @return 单例配置管理器。
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * 按地形 id 获取地形配置。
     *
     * @param id 地形配置 id。
     * @return 对应地形配置。
     * @throws IllegalArgumentException 当 id 不存在时抛出。
     */
    public TerrainConfig getTerrain(String id) {
        return requireConfig(terrains, id, "terrain");
    }

    /**
     * 按地形编码获取地形配置。
     *
     * @param code 地形数值编码。
     * @return 对应地形配置。
     * @throws IllegalArgumentException 当编码不存在时抛出。
     */
    public TerrainConfig getTerrainByCode(int code) {
        TerrainConfig terrainConfig = terrainsByCode.get(code);
        if (terrainConfig == null) {
            throw new IllegalArgumentException("Unknown terrain code: " + code);
        }
        return terrainConfig;
    }

    /**
     * 按资源 id 获取资源配置。
     *
     * @param id 资源配置 id。
     * @return 对应资源配置。
     * @throws IllegalArgumentException 当 id 不存在时抛出。
     */
    public ResourceConfig getResource(String id) {
        return requireConfig(resources, id, "resource");
    }

    /**
     * 按建筑 id 获取建筑配置。
     *
     * @param id 建筑配置 id。
     * @return 对应建筑配置。
     * @throws IllegalArgumentException 当 id 不存在时抛出。
     */
    public BuildingConfig getBuilding(String id) {
        return requireConfig(buildings, id, "building");
    }

    /**
     * 按模板 id 获取地图模板配置。
     *
     * @param id 地图模板 id。
     * @return 对应地图模板配置。
     * @throws IllegalArgumentException 当 id 不存在时抛出。
     */
    public MapTemplateConfig getMapTemplate(String id) {
        return requireConfig(templates, id, "mapTemplate");
    }

    private static <T> T requireConfig(Map<String, T> configs, String id, String type) {
        T config = configs.get(id);
        if (config == null) {
            throw new IllegalArgumentException("Unknown " + type + " config: " + id);
        }
        return config;
    }

    private static <C, T> Map<String, T> loadCatalog(ObjectMapper objectMapper,
                                                      String resourcePath,
                                                      Class<C> catalogType,
                                                      Function<C, List<T>> extractor) {
        try (InputStream inputStream = ConfigManager.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing config resource: " + resourcePath);
            }
            C catalog = objectMapper.readValue(inputStream, catalogType);
            return extractor.apply(catalog).stream().collect(Collectors.toUnmodifiableMap(ConfigManager::extractId, Function.identity()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load config resource: " + resourcePath, exception);
        }
    }

    private static String extractId(Object config) {
        if (config instanceof TerrainConfig terrainConfig) {
            return terrainConfig.getId();
        }
        if (config instanceof BuildingConfig buildingConfig) {
            return buildingConfig.getId();
        }
        if (config instanceof ResourceConfig resourceConfig) {
            return resourceConfig.getId();
        }
        if (config instanceof MapTemplateConfig mapTemplateConfig) {
            return mapTemplateConfig.getId();
        }
        throw new IllegalArgumentException("Unsupported config type: " + config.getClass().getName());
    }

    /**
     * 地形配置目录对象。
     */
    public static class TerrainCatalog {
        private List<TerrainConfig> terrains;

        /**
         * 返回地形配置列表。
         *
         * @return 地形配置集合；未配置时返回空列表。
         */
        public List<TerrainConfig> getTerrains() {
            return terrains == null ? List.of() : terrains;
        }
    }

    /**
     * 建筑配置目录对象。
     */
    public static class BuildingCatalog {
        private List<BuildingConfig> buildings;

        /**
         * 返回建筑配置列表。
         *
         * @return 建筑配置集合；未配置时返回空列表。
         */
        public List<BuildingConfig> getBuildings() {
            return buildings == null ? List.of() : buildings;
        }
    }

    /**
     * 资源配置目录对象。
     */
    public static class ResourceCatalog {
        private List<ResourceConfig> resources;

        /**
         * 返回资源配置列表。
         *
         * @return 资源配置集合；未配置时返回空列表。
         */
        public List<ResourceConfig> getResources() {
            return resources == null ? List.of() : resources;
        }
    }

    /**
     * 地图模板目录对象。
     */
    public static class TemplateCatalog {
        private List<MapTemplateConfig> templates;

        /**
         * 返回地图模板列表。
         *
         * @return 模板配置集合；未配置时返回空列表。
         */
        public List<MapTemplateConfig> getTemplates() {
            return templates == null ? List.of() : templates;
        }
    }
}