package Params;

import fileio.*;

import java.util.ArrayList;
import java.util.List;

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
                    for (PairInput pair : air.getSections()) {
                        air.setAirQuality(air.getAirQuality());
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
                cell.remove(plant);
                hasEaten = true;
            } else if (plantUnlocked) {
                animal.setMass(animal.getMass() + plant.getMass());
                animal.setPendingFertilizer(0.5);
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
        int cols = map.get(0).size();

        int[] dx = {-1, 0, 1, 0};
        int[] dy = {0, 1, 0, -1};

        int bestX = -1, bestY = -1;
        int priorityLevel = 4;
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
                    if (priorityLevel > 1 || waterQuality > bestWaterQuality) {
                        bestX = nx; bestY = ny;
                        priorityLevel = 1;
                        bestWaterQuality = waterQuality;
                    }
                } else if (hasPlant && priorityLevel > 2) {
                    bestX = nx; bestY = ny;
                    priorityLevel = 2;
                } else if (hasWater && priorityLevel >= 3) {
                    if (priorityLevel > 3 || waterQuality > bestWaterQuality) {
                        bestX = nx; bestY = ny;
                        priorityLevel = 3;
                        bestWaterQuality = waterQuality;
                    }
                } else if (priorityLevel > 3 && bestX == -1) {
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
                                                          final int iter) {
        int rows = map.size();
        int cols = map.getFirst().size();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                List<InputParams> cell = map.get(y).get(x);
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

                if (air != null && air.isToxicity() && animal != null) {
                    animal.setStatus("sick");
                }

                if (soil != null && plant != null) {
                    plant.setMass(plant.getMass() + 0.2);
                }

                if (water != null) {
                    if (soil != null && iter % 2 == 1) {
                        soil.setWaterRetention(soil.getWaterRetention() + 0.1);
                    }
                    if (air != null && iter % 2 == 1) {
                        air.setHumidity(air.getHumidity() + 0.1);
                    }
                    if (plant != null) {
                        plant.setMass(plant.getMass() + 0.2);
                    }
                }

                if (air != null && plant != null) {
                    double newOxygenLevel = getOxygenLevel(plant, air);
                    air.setOxygenLevel(newOxygenLevel);
                }

                if (animal != null) {
                    if (animal.isScanned()) {
                        processAnimalFeeding(animal, cell);

                        if (iter % 2 == 1) {
                            if (!animal.hasMovedThisTurn()) {
                                processAnimalMovement(animal, y, x, map);
                                animal.setMovedThisTurn(true);
                            }
                        }
                    }
                }

                if (air != null) {
                    air.calculateAirQuality();
                }
                if (soil != null) {
                    soil.calculateSoilQuality();
                }
            }
        }

        return map;
    }

    private static double getOxygenLevel(PlantInput plant, AirInput air) {
        double oxygenFromPlant = switch (plant.getType()) {
            case "FloweringPlants" -> 6.0;
            case "GymnospermsPlants", "Ferns" -> 0.0;
            case "Mosses" -> 0.8;
            case "Algae" -> 0.5;
            default -> 0.0;
        };

        double maturityOxygenRate = switch (plant.getStatus().toLowerCase()) {
            case "young" -> 0.2;
            case "mature" -> 0.7;
            case "old" -> 0.4;
            default -> 0.0;
        };

        return air.getOxygenLevel() + oxygenFromPlant + maturityOxygenRate;
    }

    public static PairInput pickNextBestCell(List<List<List<InputParams>>> map, PairInput crtCell) {
        PairInput bestCell = null;
        int bestScore = Integer.MAX_VALUE;

        int rows = map.size();
        int cols = map.getFirst().size();

        int currentX = crtCell.getX();
        int currentY = crtCell.getY();

        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};

        for (int i = 0; i < 4; i++) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            if (ny >= 0 && ny < rows && nx >= 0 && nx < cols) {
                List<InputParams> neighborCell = map.get(ny).get(nx);

                int currentScore = (int) getCellQuality(neighborCell);

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

        final int count = cell.size();

        double possibilityToGetStuckInSoil = 0.0;
        double possibilityToGetDamagedByAir = 0.0;
        double possibilityToBeAttackedByAnimal = 0.0;
        double possibilityToGetStuckInPlants = 0.0;

        for (InputParams param : cell) {
            if (param instanceof AirInput a) {
                a.calculateAirQuality();
                a.calculateToxicity();
                possibilityToGetDamagedByAir = a.getToxicityAQ();
            } else if (param instanceof SoilInput s) {
                possibilityToGetStuckInSoil = s.calculateBlockProbability();
            } else if (param instanceof PlantInput p) {
                possibilityToGetStuckInPlants = p.calculateBlockProbability();
            } else if (param instanceof AnimalInput an) {
                possibilityToBeAttackedByAnimal = an.calculateAttackProbability();
            }
        }

        double sum = possibilityToGetStuckInSoil + possibilityToGetDamagedByAir +
                possibilityToBeAttackedByAnimal + possibilityToGetStuckInPlants;

        double mean = Math.abs(sum / count);

        return Math.round(mean);
    }
}