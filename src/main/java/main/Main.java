package main;

import Params.OutPrint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



/**
 * The entry point to this homework. It runs the checker that tests your implementation.
 */
public final class Main {

    private Main() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final ObjectWriter WRITER = MAPPER.writer().withDefaultPrettyPrinter();

    /**
     * @param inputPath input file path
     * @param outputPath output file path
     * @throws IOException when files cannot be loaded.
     */
    public static void action(final String inputPath,
                              final String outputPath) throws IOException {

        InputLoader inputLoader = new InputLoader(inputPath);
        ArrayNode output = MAPPER.createArrayNode();
        ObjectMapper objectMapper = new ObjectMapper();

        /*
         * TODO Implement your function here
         *
         * How to add output to the output array?
         * There are multiple ways to do this, here is one example:
         *
         *
         * ObjectNode objectNode = MAPPER.createObjectNode();
         * objectNode.put("field_name", "field_value");
         *
         * ArrayNode arrayNode = MAPPER.createArrayNode();
         * arrayNode.add(objectNode);
         *
         * output.add(arrayNode);
         * output.add(objectNode);
         *
         */

        List<SoilInput> soils = new ArrayList<>();
        List<AnimalInput> animals =  new ArrayList<>();
        List<PlantInput> plants = new ArrayList<>();
        List<AirInput> airs = new ArrayList<>();
        List<WaterInput> waters = new ArrayList<>();
        PairInput robotPosition = new PairInput(0, 0);


        for (SimulationInput simulationInput : inputLoader.getSimulations()) {
            String territory = simulationInput.getTerritoryDim();
            int enegryPoints = simulationInput.getEnergyPoints();
            System.out.println(enegryPoints);
            TerritorySectionParamsInput params = simulationInput.getTerritorySectionParams();

            if (params.getSoil() != null) soils.addAll(params.getSoil());
            if (params.getAnimals() != null) animals.addAll(params.getAnimals());
            if (params.getPlants() != null) plants.addAll(params.getPlants());
            if (params.getAir() != null) airs.addAll(params.getAir());
            if (params.getWater() != null) waters.addAll(params.getWater());
        }

        for (CommandInput commandInput : inputLoader.getCommands()) {
            switch (commandInput.getCommand()) {
                case "startSimulation":
                    OutPrint.printStartFinish(objectMapper, output, commandInput, 0);
                    break;
                case "stopSimulation":
                    OutPrint.printStartFinish(objectMapper, output, commandInput, 1);
                    break;
                case "printEnvironment":
                    ObjectNode outputWrapper = objectMapper.createObjectNode();
                    outputWrapper.put("command", commandInput.getCommand());

                    ObjectNode envConditions = objectMapper.createObjectNode();

                    if (!soils.isEmpty()) {
                        envConditions.set("soil", OutPrint.createNodeFromParam(objectMapper, soils.get(0)));
                    }
                    if (!plants.isEmpty()) {
                        envConditions.set("plants", OutPrint.createNodeFromParam(objectMapper, plants.get(0)));
                    }
                    if (!animals.isEmpty()) {
                        envConditions.set("animals", OutPrint.createNodeFromParam(objectMapper, animals.get(0)));
                    }
                    if (!waters.isEmpty()) {
                        envConditions.set("water", OutPrint.createNodeFromParam(objectMapper, waters.get(0)));
                    }
                    if (!airs.isEmpty()) {
                        envConditions.set("air", OutPrint.createNodeFromParam(objectMapper, airs.get(0)));
                    }

                    outputWrapper.set("output", envConditions);
                    outputWrapper.put("timestamp", commandInput.getTimestamp());

                    output.add(outputWrapper);
                    break;
            }
        }

        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        WRITER.writeValue(outputFile, output);
    }
}
