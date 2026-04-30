package main;

import params.Commands;
import params.OutPrint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fileio.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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

        PairInput robotPosition = new PairInput(0, 0);
        boolean hasSimulationStarted = false;
        List<List<List<InputParams>>> map = new ArrayList<>();
        int crtSimulation = 0;
        long energyLvl = -1;
        int nextAction = 0;
        List<Map.Entry<String,List<String>>> inventory =  new ArrayList<>();

        for (CommandInput commandInput : inputLoader.getCommands()) {
            if (!hasSimulationStarted && !commandInput.getCommand().equals("startSimulation")) {
                OutPrint.printMessage(objectMapper, output, commandInput,
                        "ERROR: Simulation not started. Cannot perform action");
                continue;
            }
            if (hasSimulationStarted && commandInput.getCommand().equals("startSimulation")) {
                OutPrint.printMessage(objectMapper, output, commandInput,
                        "ERROR: Simulation already started. Cannot perform action");
                continue;
            }
            if (nextAction > commandInput.getTimestamp()) {
                OutPrint.printMessage(objectMapper, output, commandInput,
                        "ERROR: Robot still charging. Cannot perform action");
                continue;
            }
            if (!commandInput.getCommand().equals("endSimulation") &&
                !commandInput.getCommand().equals("startSimulation")) {
                map = Commands.updateMap(map, commandInput.getTimestamp());
            }
            switch (commandInput.getCommand()) {
                case "startSimulation":
                    OutPrint.printStartFinish(objectMapper, output, commandInput, 0);
                    hasSimulationStarted = true;
                    map = Commands.buildMap(inputLoader.getSimulations().get(crtSimulation));
                    energyLvl = inputLoader.getSimulations().get(crtSimulation).getEnergyPoints();
                    break;
                case "endSimulation":
                    OutPrint.printStartFinish(objectMapper, output, commandInput, 1);
                    hasSimulationStarted = false;
                    crtSimulation++;
                    map = new ArrayList<>();
                    robotPosition = new PairInput(0, 0);
                    energyLvl = -1;
                    break;
                case "printEnvConditions":
                    OutPrint.printEnvironment(objectMapper, output,
                            map.get(robotPosition.getX()).get(robotPosition.getY()),
                            commandInput.getTimestamp());
                    break;
                case "printMap" :
                    OutPrint.printMap(objectMapper, output, map, commandInput.getTimestamp());
                    break;
                case "getEnergyStatus" :
                    OutPrint.printGetEnergyStatus(objectMapper, output, commandInput, energyLvl);
                    break;
                case "moveRobot" :
                    final PairInput nextPos = Commands.pickNextBestCell(map, robotPosition);

                    if (nextPos != null) {
                        final List<InputParams> nextCell = map.get(nextPos.getX()).get(nextPos.getY());
                        final long quality = Commands.getCellQuality(nextCell);
                        long oldEnergy = energyLvl;
                        energyLvl = OutPrint.printMoveRobot(objectMapper, output, commandInput, nextPos, quality, energyLvl);
                        if (energyLvl != oldEnergy) {
                            robotPosition.setX(nextPos.getX());
                            robotPosition.setY(nextPos.getY());
                        }
                    }
                    break;
                case "rechargeBattery" :
                    energyLvl = energyLvl + commandInput.getTimeToCharge();
                    nextAction = commandInput.getTimestamp() + commandInput.getTimeToCharge();
                    OutPrint.printMessage(objectMapper, output, commandInput,
                            "Robot battery is charging.");
                    break;
                case "scanObject":
                    if (energyLvl < 7) {
                        OutPrint.printMessage(objectMapper, output, commandInput,
                                "ERROR: Not enough energy to perform action");
                        continue;
                    }
                    String type = Commands.scanObject(commandInput, map, robotPosition);

                    if (type != null) {
                        String message = "The scanned object is " + type + ".";
                        OutPrint.printMessage(objectMapper, output, commandInput,
                                message);
                        type = type.substring(type.lastIndexOf(" ") + 1);
                        for (InputParams params: map.get(robotPosition.getX()).get(robotPosition.getY())) {
                            boolean alreadyInInventory = inventory.stream()
                                    .anyMatch(entry -> entry.getKey().equals(params.getName()));

                            if ((params instanceof PlantInput && type.equals("plant") ||
                                params instanceof WaterInput && type.equals("water")) &&
                                !alreadyInInventory) {
                                inventory.add(Map.entry(params.getName(), new ArrayList<>()));
                                break;
                            }
                        }
                        energyLvl -= 7;
                    } else {
                        OutPrint.printMessage(objectMapper, output, commandInput,
                                "ERROR: Object not found. Cannot perform action");
                        continue;
                    }
                    break;
                case "changeWeatherConditions":
                    Commands.changeWeatherConditions(map, commandInput.getType(),
                            objectMapper, output, commandInput);
                    break;
                case "learnFact":
                    if (energyLvl < 2) {
                        OutPrint.printMessage(objectMapper, output, commandInput,
                                "ERROR: Not enough battery left. Cannot perform action");
                        continue;
                    }
                    boolean found = false;
                    for (Map.Entry<String, List<String>> stringListEntry : inventory) {
                        if (stringListEntry.getKey().equals(commandInput.getComponents())) {
                            stringListEntry.getValue().add(commandInput.getSubject());
                            found = true;
                            OutPrint.printMessage(objectMapper, output, commandInput,
                                    "The fact has been successfully saved in the database.");
                            energyLvl -= 2;
                            break;
                        }
                    }
                    if (!found) {
                        OutPrint.printMessage(objectMapper, output, commandInput,
                                "ERROR: Subject not yet saved. Cannot perform action");
                        continue;
                    }
                    break;
                case "printKnowledgeBase":
                    OutPrint.printKnowledgeBase(objectMapper, output, commandInput, inventory);
                    break;
            }
        }

        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        WRITER.writeValue(outputFile, output);
    }
}
