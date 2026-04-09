package main;

import Params.Commands;
import Params.Exceptions;
import Params.OutPrint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

        PairInput robotPosition = new PairInput(0, 0);
        boolean hasSimulationStarted = false;
        List<List<List<InputParams>>> map = new ArrayList<>();
        int crtSimulation = 0;
        long energyLvl = -1;
        boolean startedRecharge = false;
        int nextAction = 0;

        for (CommandInput commandInput : inputLoader.getCommands()) {
            if (!hasSimulationStarted && !commandInput.getCommand().equals("startSimulation")) {
                Exceptions.printError(objectMapper, output, commandInput,
                        "ERROR: Simulation not started. Cannot perform action");
                continue;
            }
            if (hasSimulationStarted && commandInput.getCommand().equals("startSimulation")) {
                Exceptions.printError(objectMapper, output, commandInput,
                        "ERROR: Simulation already started. Cannot perform action");
                continue;
            }
            if (nextAction > commandInput.getTimestamp()) {
                Exceptions.printError(objectMapper, output, commandInput,
                        "ERROR: Robot still charging. Cannot perform action");
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
                    map = Commands.updateMap(map, commandInput.getTimestamp());

                    OutPrint.printEnvironment(objectMapper, output,
                            map.get(robotPosition.getY()).get(robotPosition.getX()),
                            commandInput.getTimestamp());
                    break;
                case "printMap" :
                    map = Commands.updateMap(map, commandInput.getTimestamp());
                    OutPrint.printMap(objectMapper, output, map, commandInput.getTimestamp());
                    break;
                case "getEnergyStatus" :
                    OutPrint.printGetEnergyStatus(objectMapper, output, commandInput, energyLvl);
                    break;
                case "moveRobot" :
                    map = Commands.updateMap(map, commandInput.getTimestamp());
                    final PairInput nextPos = Commands.pickNextBestCell(map, robotPosition);

                    if (nextPos != null) {
                        final List<InputParams> nextCell = map.get(nextPos.getY()).get(nextPos.getX());
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
                    OutPrint.printRechargeBattery(objectMapper, output, commandInput);
            }
        }

        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        WRITER.writeValue(outputFile, output);
    }
}
