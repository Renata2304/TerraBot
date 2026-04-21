package params;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fileio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Commands extends InputParams {

    public static List<List<List<InputParams>>> buildMap(SimulationInput simulationInput) {
        List<List<List<InputParams>>> map = new ArrayList<>();
        String dim = simulationInput.getTerritoryDim();
        String[] dimensions = dim.split("x");
        int rows = Integer.parseInt(dimensions[0]);
        int cols = Integer.parseInt(dimensions[1]);

        for (int i = 0; i < rows; i++) {
            List<List<InputParams>> row = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                row.add(new ArrayList<>());
            }
            map.add(row);
        }

        TerritorySectionParamsInput params = simulationInput.getTerritorySectionParams();
        List<SoilInput> soils = params.getSoil();
        List<AnimalInput> animals = params.getAnimals();
        List<PlantInput> plants = params.getPlants();
        List<AirInput> airs = params.getAir();
        List<WaterInput> waters = params.getWater();

        if (soils != null) {
            for (SoilInput soil : soils) {
                if (soil.getSections() != null) {
                    for (PairInput pos : soil.getSections()) {
                        soil.setSoilQuality(soil.getSoilQuality());
                        map.get(pos.getX()).get(pos.getY()).add(soil);
                    }
                }
            }
        }
        if (animals != null) {
            for (AnimalInput animal : animals) {
                if (animal.getSections() != null) {
                    for (PairInput pos : animal.getSections()) {
                        map.get(pos.getX()).get(pos.getY()).add(animal);
                    }
                }
            }
        }
        if (plants != null) {
            for (PlantInput plant : plants) {
                if (plant.getSections() != null) {
                    for (PairInput pos : plant.getSections()) {
                        map.get(pos.getX()).get(pos.getY()).add(plant);
                    }
                }
            }
        }
        if (airs != null) {
            for (AirInput air : airs) {
                if (air.getSections() != null) {
                    air.setWeatherEventTimestamp(-1);
                    air.calculateAirQuality();
                    for (PairInput pair : air.getSections()) {
                        map.get(pair.getX()).get(pair.getY()).add(air);
                    }
                }
            }
        }
        if (waters != null) {
            for (WaterInput water : waters) {
                if (water.getSections() != null) {
                    for (PairInput pair : water.getSections()) {
                        map.get(pair.getX()).get(pair.getY()).add(water);
                    }
                }
            }
        }

        return map;
    }

    public static void processAnimalFeeding(AnimalInput animal, List<InputParams> cell) {
        if (!animal.isScanned() || "sick".equals(animal.getStatus())) {
            return;
        }

        boolean isCarnivore = "Carnivores".equals(animal.getType()) || "Parasites".equals(animal.getType());
        boolean hasEaten = false;

        AnimalInput prey = null;
        PlantInput plant = null;
        WaterInput water = null;

        for (InputParams param : cell) {
            if (param instanceof AnimalInput a && a != animal) prey = a;
            else if (param instanceof PlantInput p) plant = p;
            else if (param instanceof WaterInput w) water = w;
        }

        if (isCarnivore && prey != null) {
            animal.setMass(animal.getMass() + prey.getMass());
            animal.setPendingFertilizer(0.5);
            cell.remove(prey);
            hasEaten = true;
        } else {
            boolean plantUnlocked = plant != null && plant.isScanned();
            boolean waterUnlocked = water != null && water.isScanned();

            if (plantUnlocked && waterUnlocked) {
                double waterToDrink = Math.min(animal.getMass() * 0.08, water.getMass());
                water.setMass(water.getMass() - waterToDrink);
                if (water.getMass() <= 0) cell.remove(water);

                animal.setMass(animal.getMass() + waterToDrink + plant.getMass());
                animal.setPendingFertilizer(0.8);
                plant.setStatus("dead");
                cell.remove(plant);
                hasEaten = true;
            } else if (plantUnlocked) {
                animal.setMass(animal.getMass() + plant.getMass());
                animal.setPendingFertilizer(0.5);
                plant.setStatus("dead");
                cell.remove(plant);
                hasEaten = true;
            } else if (waterUnlocked) {
                double waterToDrink = Math.min(animal.getMass() * 0.08, water.getMass());
                water.setMass(water.getMass() - waterToDrink);
                if (water.getMass() <= 0) cell.remove(water);

                animal.setMass(animal.getMass() + waterToDrink);
                animal.setPendingFertilizer(0.5);
                hasEaten = true;
            }
        }

        if (hasEaten) {
            animal.setStatus("well-fed");
        } else {
            animal.setStatus("hungry");
            animal.setPendingFertilizer(0.0);
        }
    }

    public static void processAnimalMovement(AnimalInput animal, int currentX, int currentY,
                                             List<List<List<InputParams>>> map) {
        int rows = map.size();
        int cols = map.getFirst().size();

        int[] dx = { 0, 1, 0, -1 };
        int[] dy = { 1, 0, -1, 0 };

        int bestX = -1, bestY = -1;
        double bestWaterQuality = -1.0;

        for (int i = 0; i < 4; i++) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols) {
                List<InputParams> neighborCell = map.get(nx).get(ny);

                boolean hasPlant = false;
                boolean hasWater = false;
                double waterQuality = 0.0;

                for (InputParams param : neighborCell) {
                    if (param instanceof PlantInput) {
                        hasPlant = true;
                    }
                    if (param instanceof WaterInput w) {
                        hasWater = true;
                        waterQuality = w.getWaterQuality();
                    }
                }

                if (hasPlant && hasWater) {
                    if (waterQuality > bestWaterQuality) {
                        bestX = nx; bestY = ny;
                        bestWaterQuality = waterQuality;
                    }
                } else if (hasPlant) {
                    bestX = nx; bestY = ny;
                } else if (hasWater) {
                    if (waterQuality > bestWaterQuality) {
                        bestX = nx; bestY = ny;
                        bestWaterQuality = waterQuality;
                    }
                } else if (bestX == -1) {
                    bestX = nx; bestY = ny;
                }
            }
        }

        if (bestX != -1 && bestY != -1) {
            map.get(currentX).get(currentY).remove(animal);
            map.get(bestX).get(bestY).add(animal);
        }
    }

    public static List<List<List<InputParams>>> updateMap(List<List<List<InputParams>>> map,
                                                          final long currentTimestamp) {
        int rows = map.size();
        int cols = map.getFirst().size();

        for (List<List<InputParams>> lists : map) {
            for (int y = 0; y < cols; y++) {
                List<InputParams> cell = lists.get(y);
                if (cell != null) {
                    for (InputParams param : cell) {
                        if (param instanceof AnimalInput an) {
                            an.setMovedThisTurn(false);
                            an.setFedThisTurn(false);
                        } else if (param instanceof PlantInput p) {
                            p.setGrownFromSoilThisTurn(false);
                            p.setGrownFromWaterThisTurn(false);
                            p.setBreathedThisTurn(false);
                        } else if (param instanceof WaterInput w) {
                            w.setActedOnSoilThisTurn(false);
                            w.setActedOnAirThisTurn(false);
                        } else if (param instanceof AirInput a) {
                            a.setQualityUpdatedThisTurn(false);
                        } else if (param instanceof SoilInput s) {
                            s.setQualityUpdatedThisTurn(false);
                        }
                    }
                }
            }
        }

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                List<InputParams> cell = map.get(x).get(y);
                if (cell == null || cell.isEmpty()) {
                    continue;
                }

                AirInput air = null;
                SoilInput soil = null;
                WaterInput water = null;
                PlantInput plant = null;
                AnimalInput animal = null;

                for (InputParams param : cell) {
                    if (param instanceof AirInput a) air = a;
                    else if (param instanceof SoilInput s) soil = s;
                    else if (param instanceof WaterInput w) water = w;
                    else if (param instanceof PlantInput p) plant = p;
                    else if (param instanceof AnimalInput an) animal = an;
                }

                if (soil != null && plant != null && plant.isScanned()) {
                    if (!plant.isGrownFromSoilThisTurn()) {
                        plant.addGrowth(0.2);
                        plant.setGrownFromSoilThisTurn(true);
                    }
                }

                if (water != null && water.isScanned()) {
                    long timeSinceWaterScan = currentTimestamp - water.getScanTimestamp();

                    if (timeSinceWaterScan >= 2 && timeSinceWaterScan % 2 == 0) {
                        if (soil != null && !water.isActedOnSoilThisTurn()) {
                            BigDecimal bd = new BigDecimal(soil.getWaterRetention() + 0.1)
                                    .setScale(2, RoundingMode.HALF_UP);
                            soil.setWaterRetention(bd.doubleValue());
                            water.setActedOnSoilThisTurn(true);
                        }
                    }

                    if (timeSinceWaterScan % 2 == 0) {
                        if (air != null && !water.isActedOnAirThisTurn()) {
                            double humidity = air.getHumidity() + 0.1;
                            double normalizeScore = Math.clamp(humidity, 0.0, 100.0);
                            humidity = Math.round(normalizeScore * 100.0) / 100.0;
                            air.setHumidity(humidity);
                            water.setActedOnAirThisTurn(true);
                        }
                    }

                    if (plant != null && plant.isScanned()) {
                        if (!plant.isGrownFromWaterThisTurn()) {
                            plant.addGrowth(0.2);
                            plant.setGrownFromWaterThisTurn(true);
                        }
                    }
                }

                if (air != null && plant != null && plant.isScanned()) {
                    if (!"dead".equals(plant.getStatus())) {
                        if (!plant.isBreathedThisTurn()) {
                            double newOxygenLevel = air.getOxygenLevel() +
                                    plant.getOxygenContribution();
                            BigDecimal bd = new BigDecimal(newOxygenLevel).
                                    setScale(2, RoundingMode.HALF_UP);
                            air.setOxygenLevel(bd.doubleValue());
                            plant.setBreathedThisTurn(true);
                        }
                    }
                }

                if (animal != null && animal.isScanned()) {
                    if (!animal.isFedThisTurn()) {
                        processAnimalFeeding(animal, cell);
                        animal.setFedThisTurn(true);
                    }

                    if (!animal.isMovedThisTurn()) {
                        if (currentTimestamp > 0 && currentTimestamp % 2 == 0) {
                            processAnimalMovement(animal, x, y, map);
                        }
                        animal.setMovedThisTurn(true);
                    }
                }

                if (air != null) {
                    boolean hasActiveEvent = (air.getWeatherEventTimestamp() != -1);
                    boolean isWeatherPersistent = hasActiveEvent &&
                            (currentTimestamp < air.getWeatherEventTimestamp() + 2);
                    if (!isWeatherPersistent) {
                        air.calculateAirQuality();
                        if (hasActiveEvent) {
                            air.setWeatherEventTimestamp(-1);
                        }
                    }
                    air.setQualityUpdatedThisTurn(true);
                }
                if (soil != null && !soil.isQualityUpdatedThisTurn()) {
                    soil.calculateSoilQuality();
                    soil.setQualityUpdatedThisTurn(true);
                }

                if (plant != null && "dead".equals(plant.getStatus())) {
                    cell.remove(plant);
                }
            }
        }

        return map;
    }

    public static PairInput pickNextBestCell(List<List<List<InputParams>>> map, PairInput crtCell) {
        PairInput bestCell = null;
        long bestScore = Integer.MAX_VALUE;

        int rows = map.size();
        int cols = map.getFirst().size();

        int currentX = crtCell.getX();
        int currentY = crtCell.getY();

        int[] dx = { 0, 1, 0, -1 };
        int[] dy = { 1, 0, -1, 0 };

        for (int i = 0; i < 4; i++) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols) {
                List<InputParams> neighborCell = map.get(nx).get(ny);

                long currentScore = getCellQuality(neighborCell);

                if (bestCell == null || currentScore < bestScore) {
                    bestScore = currentScore;

                    bestCell = new PairInput();
                    bestCell.setX(nx);
                    bestCell.setY(ny);
                }
            }
        }

        return bestCell;
    }

    public static long getCellQuality(List<InputParams> cell) {
        if (cell == null || cell.isEmpty()) {
            return 0;
        }

        double possibilityToGetStuckInSoil = 0.0;
        double possibilityToGetDamagedByAir = 0.0;
        double possibilityToBeAttackedByAnimal = 0.0;
        double possibilityToGetStuckInPlants = 0.0;

        int validEntitiesCount = 0;

        for (InputParams param : cell) {
            if (param instanceof AirInput a) {
                a.calculateToxicity();
                possibilityToGetDamagedByAir = a.getToxicityAQ();
                validEntitiesCount++;
            } else if (param instanceof SoilInput s) {
                possibilityToGetStuckInSoil = s.calculateBlockProbability();
                validEntitiesCount++;
            } else if (param instanceof PlantInput p) {
                possibilityToGetStuckInPlants = p.calculateBlockProbability();
                validEntitiesCount++;
            } else if (param instanceof AnimalInput an) {
                possibilityToBeAttackedByAnimal = an.calculateAttackProbability();
                validEntitiesCount++;
            }
        }

        if (validEntitiesCount == 0) {
            return 0;
        }

        double sum = possibilityToGetStuckInSoil + possibilityToGetDamagedByAir +
                possibilityToBeAttackedByAnimal + possibilityToGetStuckInPlants;

        double mean = sum / validEntitiesCount;

        return Math.round(mean);
    }

    public static String scanObject(final CommandInput commandInput,
                                    final List<List<List<InputParams>>> map,
                                    final PairInput crtPos) {
        final int x = crtPos.getX(), y = crtPos.getY();
        String color = commandInput.getColor();
        String smell = commandInput.getSmell();
        String sound = commandInput.getSound();

        String type = null;
        if ("none".equals(color)) {
            type = "water";
        } else if ("none".equals(sound)) {
            type = "plant";
        } else {
            type = "animal";
        }

        List<InputParams> crtMapPos = map.get(x).get(y);
        for (InputParams param : crtMapPos) {
            if (param instanceof WaterInput w && type.equals("water")) {
                w.setScanned(true);
                w.setScanTimestamp(commandInput.getTimestamp());
                return "water";
            }
            else if (param instanceof PlantInput p && type.equals("plant")) {
                p.setScanned(true);
                p.setScanTimestamp(commandInput.getTimestamp());
                return "a plant";
            }
            else if (param instanceof AnimalInput a && type.equals("animal")) {
                a.setScanned(true);
                a.setScanTimestamp(commandInput.getTimestamp());
                return "an animal";
            }
        }
        return null;
    }

    private static boolean canWeatherChange(final List<List<List<InputParams>>> map, final String type) {
        int rows = map.size(), cols = map.getFirst().size();
        for (List<List<InputParams>> lists : map) {
            for (int y = 0; y < cols; y++) {
                List<InputParams> cell = lists.get(y);
                if (cell == null || cell.isEmpty()) {
                    continue;
                }
                for (InputParams param : cell) {
                    if (param instanceof AirInput) {
                        boolean isMatch = airTypeChangeWeather(type, param);

                        if (isMatch) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void changeWeatherConditions(List<List<List<InputParams>>> map,
                                               final String type, final ObjectMapper objectMapper,
                                               final ArrayNode output, final CommandInput commandInput) {
        if (!canWeatherChange(map, type)) {
            OutPrint.printMessage(objectMapper, output, commandInput,
                    "ERROR: The weather change does not affect the environment. Cannot perform action");
            return;
        }

        int rows = map.size(), cols = map.getFirst().size();
        for (List<List<InputParams>> lists : map) {
            for (int y = 0; y < cols; y++) {
                List<InputParams> cell = lists.get(y);
                if (cell == null || cell.isEmpty()) {
                    continue;
                }
                for (InputParams param : cell) {
                    if (param instanceof AirInput) {
                        boolean isTargetAir = airTypeChangeWeather(type, param);

                        if (isTargetAir) {
                            ((AirInput) param).applyWeatherEvent(commandInput);
                        }
                    }
                }
            }
        }
        OutPrint.printMessage(objectMapper, output, commandInput, "The weather has changed.");
    }

    private static boolean airTypeChangeWeather(String type, InputParams param) {
        if (type == null) return false;
        return switch (type) {
            case "desertStorm" -> param.getType().equals("DesertAir");
            case "peopleHiking" -> param.getType().equals("MountainAir");
            case "newSeason" -> param.getType().equals("TemperateAir");
            case "polarStorm" -> param.getType().equals("PolarAir");
            case "rainfall" -> param.getType().equals("TropicalAir");
            default -> false;
        };
    }

}