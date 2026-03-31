package Params;

import fileio.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static java.util.Collections.addAll;

public class Commands extends InputParams{

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
        if  (waters != null) {
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
        double fertilizerBonus = 0.0;

        // Extragem entitățile de pe celulă
        AnimalInput prey = null;
        PlantInput plant = null;
        WaterInput water = null;
        SoilInput soil = null;

        for (InputParams param : cell) {
            if (param instanceof AnimalInput a && a != animal) prey = a;
            else if (param instanceof PlantInput p) plant = p;
            else if (param instanceof WaterInput w) water = w;
            else if (param instanceof SoilInput s) soil = s;
        }

        // 1. Carnivore / Parasite mănâncă alt animal (chiar și nescanat)
        if (isCarnivore && prey != null) {
            animal.setMass(animal.getMass() + prey.getMass());
            cell.remove(prey);
            hasEaten = true;
            fertilizerBonus = 0.5;
        }
        // 2. Ierbivore / Fallback pentru carnivore fără pradă
        else {
            boolean plantUnlocked = plant != null && plant.isScanned();
            boolean waterUnlocked = water != null && water.isScanned();

            if (plantUnlocked && waterUnlocked) {
                double waterToDrink = Math.min(animal.getMass() * 0.08, water.getMass());
                water.setMass(water.getMass() - waterToDrink);
                animal.setMass(animal.getMass() + waterToDrink + plant.getMass());
                cell.remove(plant);
                hasEaten = true;
                fertilizerBonus = 0.8;
            } else if (plantUnlocked) {
                animal.setMass(animal.getMass() + plant.getMass());
                cell.remove(plant);
                hasEaten = true;
                fertilizerBonus = 0.5;
            } else if (waterUnlocked) {
                double waterToDrink = Math.min(animal.getMass() * 0.08, water.getMass());
                water.setMass(water.getMass() - waterToDrink);
                animal.setMass(animal.getMass() + waterToDrink);
                hasEaten = true;
                fertilizerBonus = 0.5;
            }
        }

        // 3. Actualizare status și îngrășământ
        if (hasEaten) {
            animal.setStatus("well-fed");
            if (soil != null) {
                soil.setOrganicMatter(soil.getOrganicMatter() + fertilizerBonus);
            }
        } else {
            animal.setStatus("hungry");
        }
    }

    public static void processAnimalMovement(AnimalInput animal, int currentX, int currentY,
                                             List<List<List<InputParams>>> map) {
        int rows = map.size();
        int cols = map.get(0).size();

        // Direcțiile: Sus, Dreapta, Jos, Stânga
        int[] dx = {-1, 0, 1, 0};
        int[] dy = {0, 1, 0, -1};

        int bestX = -1, bestY = -1;
        int priorityLevel = 4; // 1 = Plant+Water, 2 = Plant, 3 = Water, 4 = Any valid
        double bestWaterQuality = -1.0;

        for (int i = 0; i < 4; i++) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            // Verificăm limitele hărții
            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols) {
                List<InputParams> neighborCell = map.get(nx).get(ny);

                boolean hasPlant = false;
                boolean hasWater = false;
                double waterQuality = 0.0;

                for (InputParams param : neighborCell) {
                    if (param instanceof PlantInput) hasPlant = true;
                    if (param instanceof WaterInput w) {
                        hasWater = true;
                        waterQuality = w.getPurity();
                    }
                }

                // Prio 1: Plant + Water (tie-breaker: waterQuality)
                if (hasPlant && hasWater) {
                    if (priorityLevel > 1 || waterQuality > bestWaterQuality) {
                        bestX = nx; bestY = ny;
                        priorityLevel = 1;
                        bestWaterQuality = waterQuality;
                    }
                }
                // Prio 2: Doar Plant (luăm prima găsită confrom ordinii parcurgerii)
                else if (hasPlant && priorityLevel > 2) {
                    bestX = nx; bestY = ny;
                    priorityLevel = 2;
                }
                // Prio 3: Doar Water (tie-breaker: waterQuality)
                else if (hasWater && priorityLevel >= 3) {
                    if (priorityLevel > 3 || waterQuality > bestWaterQuality) {
                        bestX = nx; bestY = ny;
                        priorityLevel = 3;
                        bestWaterQuality = waterQuality;
                    }
                }
                // Prio 4: Orice pătrățică validă (prima găsită)
                else if (priorityLevel > 3 && bestX == -1) {
                    bestX = nx; bestY = ny;
                }
            }
        }

        // Efectuăm mișcarea dacă am găsit o celulă validă
        if (bestX != -1 && bestY != -1) {
            map.get(currentX).get(currentY).remove(animal);
            map.get(bestX).get(bestY).add(animal);

            // Actualizăm coordonatele interne ale animalului (dacă folosești o astfel de reținere)
            // animal.setX(bestX);
            // animal.setY(bestY);
        }
    }

    public static List<List<List<InputParams>>> updateMap(List<List<List<InputParams>>> map,
                                                          final int iter) {
        int rows =  map.size();
        int cols =  map.get(0).size();

        for  (int y = 0; y < rows; y++) {
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

                // Soil -> Plant (+0.2 mass)
                if (soil != null && plant != null) {
                    plant.setMass(plant.getMass() + 0.2);
                }

                // Water -> Air, Soil, Plant
                if (water != null) {

                    // Water -> Soil (+0.1 waterRetention la fiecare 2 iterații)
                    if (soil != null && iter % 2 == 1) {
                        soil.setWaterRetention(soil.getWaterRetention() + 0.1);
                    }

                    // Water -> Air (+0.1 humidity la fiecare 2 iterații)
                    if (air != null && iter % 2 == 1) {
                        air.setHumidity(air.getHumidity() + 0.1);
                    }

                    // Water -> Plant (+0.2 mass la FIECARE iterație)
                    if (plant != null) {
                        plant.setMass(plant.getMass() + 0.2);
                    }
                }

                // Plant -> Air (+ oxigen)
                if (air != null && plant != null) {

                    double newOxygenLevel = getOxygenLevel(plant, air);
                    air.setOxygenLevel(newOxygenLevel);
                }

                // Animal -> Soil, Water, Plant
                if (animal != null) {
                    if (animal.isScanned()) {
                        processAnimalFeeding(animal, cell);

                        if (iter % 2 == 1) {
                            if (!animal.hasMovedThisTurn()) {
                                processAnimalMovement(animal, x, y, map);
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
}
