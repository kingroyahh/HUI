package com.hui.mapsystem;

import com.hui.mapsystem.config.ConfigManager;
import com.hui.mapsystem.model.GameMap;
import com.hui.mapsystem.model.MapBuilding;
import com.hui.mapsystem.model.MapEntity;
import com.hui.mapsystem.model.MoveComponent;
import com.hui.mapsystem.model.MoveType;
import com.hui.mapsystem.model.SquareCoordinate;
import com.hui.mapsystem.model.VisionComponent;
import com.hui.mapsystem.path.PathFinder;
import com.hui.mapsystem.vision.IncrementalVisionService;
import com.hui.mapsystem.vision.VisionDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSystemTest {
    private GameMap gameMap;

    @BeforeEach
    void setUp() {
        gameMap = GameMap.fromTemplate("starter-map");
        PathFinder.bind(gameMap);
    }

    @Test
    void shouldLoadConfigDrivenResourcesAndBuildings() {
        assertEquals("WOOD", gameMap.getCell(1, 1).getResourceType());
        assertEquals("ORE", gameMap.getCell(4, 4).getResourceType());
        assertEquals("Forest", ConfigManager.getInstance().getResource("WOOD").getName());

        MapBuilding city = gameMap.getBuilding("city-1");
        assertNotNull(city);
        assertEquals("Starter City", city.getBuildingConfig().getName());
        assertEquals(2, city.getBuildingConfig().getVisionBonus());
        assertEquals(4, city.getOccupiedCells().size());
        assertFalse(gameMap.isWalkable(new SquareCoordinate(4, 4), MoveType.GROUND));
        assertFalse(gameMap.isWalkable(new SquareCoordinate(5, 5), MoveType.GROUND));
    }

    @Test
    void shouldFindPathAroundBlockingBuilding() {
        List<SquareCoordinate> path = PathFinder.findPath(
            new SquareCoordinate(0, 0),
            new SquareCoordinate(4, 0),
            MoveType.GROUND
        );

        assertFalse(path.isEmpty());
        assertEquals(new SquareCoordinate(0, 0), path.get(0));
        assertEquals(new SquareCoordinate(4, 0), path.get(path.size() - 1));
        assertFalse(path.contains(new SquareCoordinate(2, 0)));
        assertTrue(path.size() > 5);
    }

    @Test
    void shouldReturnIncrementalVisionDeltaInsteadOfFullRefresh() {
        MapEntity scout = new MapEntity(
            "scout-1",
            new MoveComponent(MoveType.GROUND),
            new VisionComponent(1)
        );
        IncrementalVisionService visionService = new IncrementalVisionService();

        VisionDelta initialDelta = visionService.updateVision(gameMap, scout, new SquareCoordinate(1, 1));
        VisionDelta movedDelta = visionService.updateVision(gameMap, scout, new SquareCoordinate(2, 1));

        assertEquals(5, initialDelta.enteredCells().size());
        assertTrue(initialDelta.exitedCells().isEmpty());
        assertEquals(5, movedDelta.visibleCellCount());
        assertEquals(Set.of(
            new SquareCoordinate(3, 1),
            new SquareCoordinate(2, 0),
            new SquareCoordinate(2, 2)
        ), movedDelta.enteredCells());
        assertEquals(Set.of(
            new SquareCoordinate(0, 1),
            new SquareCoordinate(1, 0),
            new SquareCoordinate(1, 2)
        ), movedDelta.exitedCells());
    }
}