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
            for (WaterInput water : waters) {
                if (water.getSections() != null) {
                    for (PairInput pair : water.getSections()) {
                        map.get(pair.getX()).get(pair.getY()).add(water);
                    }
                }
            }
        }
        if (airs != null) {
            for (AirInput air : airs) {
                if (air.getSections() != null) {
                    for (PairInput pair : air.getSections()) {
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
}
