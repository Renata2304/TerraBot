package params;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.*;

import java.util.List;
import java.util.Map;

public class OutPrint extends InputParams{

    public static void printMessage(final ObjectMapper objectMapper, final ArrayNode output,
                                  final CommandInput commandInput, final String messageToPrint) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("command", commandInput.getCommand());
        message.put("message", messageToPrint);
        message.put("timestamp", commandInput.getTimestamp());

        output.add(message);
    }

    public static void printStartFinish(final ObjectMapper objectMapper, final ArrayNode output,
                                  final CommandInput commandInput, final int startFinish) {
        //   Start = 0, Finish = 1
        ObjectNode message = objectMapper.createObjectNode();
        message.put("command", commandInput.getCommand());
        message.put("message", startFinish == 1 ? "Simulation has ended." : "Simulation has started.");

        message.put("timestamp", commandInput.getTimestamp());

        output.add(message);
    }

    public static ObjectNode createNodeFromParam(ObjectMapper objectMapper, InputParams param) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("type", param.getType());
        details.put("name", param.getName());
        details.put("mass", param.getMass());

        if (param.getExtraDetails() != null) {
            for (Map.Entry<String, Double> extraDetail : param.getExtraDetails()) {
                if ("dustParticles".equals(extraDetail.getKey())) {
                    continue;
                }
                details.put(extraDetail.getKey(), extraDetail.getValue());
            }
        }

        if (param instanceof AirInput a) {
            if ("DesertAir".equals(a.getType())) {
                boolean isStormActive = (a.getWeatherEventTimestamp() != -1);
                details.put("desertStorm", isStormActive);
            } else if ("PolarAir".equals(a.getType())) {
                boolean isStormActive = (a.getWeatherEventTimestamp() != -1);
                details.put("polarStorm", isStormActive);
            }
        }

        return details;
    }

    public static void printEnvironment(ObjectMapper objectMapper, ArrayNode output,
                                        List<InputParams> crtPos, final int timestamp) {
        ObjectNode outputWrapper = objectMapper.createObjectNode();
        outputWrapper.put("command", "printEnvConditions");

        ObjectNode envConditions = objectMapper.createObjectNode();

        for (InputParams param : crtPos) {
            envConditions.set(param.getEntity(),
                    OutPrint.createNodeFromParam(objectMapper, param));
        }

        outputWrapper.set("output", envConditions);
        outputWrapper.put("timestamp", timestamp);

        output.add(outputWrapper);
    }

    public static ObjectNode printHelperMap(ObjectMapper objectMapper, List<InputParams> params, int x, int y) {
        ObjectNode details = objectMapper.createObjectNode();

        String airQuality = null;
        String soilQuality = null;
        int totalObjects = 0;

        if (params != null) {
            totalObjects = params.size();

            for (InputParams param : params) {
                switch (param) {
                    case AirInput air -> {
                        airQuality = air.getQualityStatus(air.getAirQuality()).toLowerCase();
                        totalObjects = totalObjects - 1;
                    }
                    case SoilInput soil -> {
                        soilQuality = soil.getQualityStatus(soil.getSoilQuality()).toLowerCase();
                        totalObjects = totalObjects - 1;
                    }
                    default -> {
                    }
                }
            }
        }

        details.put("airQuality", airQuality);

        ArrayNode sectionArray = objectMapper.createArrayNode();
        sectionArray.add(x);
        sectionArray.add(y);
        details.set("section", sectionArray);

        details.put("soilQuality", soilQuality);
        details.put("totalNrOfObjects", totalObjects);

        return details;
    }

    public static void printMap(ObjectMapper objectMapper, ArrayNode output,
                                List<List<List<InputParams>>> map, final int timestamp) {
        ObjectNode outputWrapper = output.addObject();
        outputWrapper.put("command", "printMap");

        ArrayNode envConditions = objectMapper.createArrayNode();

        if (!map.isEmpty()) {
            int nrOfRows = map.size();
            int nrOfCols = map.getFirst().size();

            for (int x = 0; x < nrOfRows; x++) {
                for (int y = 0; y < nrOfCols; y++) {
                    List<InputParams> cellParams = map.get(y).get(x);
                    ObjectNode cellOutput = printHelperMap(objectMapper, cellParams, y, x);

                    if (!cellOutput.isEmpty()) {
                        envConditions.add(cellOutput);
                    }
                }
            }
        }

        outputWrapper.set("output", envConditions);
        outputWrapper.put("timestamp", timestamp);
    }

    public static void printGetEnergyStatus (final ObjectMapper objectMapper, final ArrayNode output,
                                             final CommandInput commandInput, final long energyLvl) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("command", commandInput.getCommand());
        message.put("message", "TerraBot has " + energyLvl + " energy points left.");
        message.put("timestamp", commandInput.getTimestamp());

        output.add(message);
    }

    private static boolean verifyMoveRobot(final ObjectMapper objectMapper, final ArrayNode output,
                                           final CommandInput commandInput, final long energyLvl,
                                           final long quality) {
        if (energyLvl - quality < 0) {
            printMessage(objectMapper, output, commandInput,
                    "ERROR: Not enough battery left. Cannot perform action");
            return false;
        }

        return true;
    }

    public static long printMoveRobot(final ObjectMapper objectMapper, final ArrayNode output,
                                      final CommandInput commandInput, final PairInput posToMove,
                                      final long quality, long energyLvl) {
        boolean result = verifyMoveRobot(objectMapper, output, commandInput, energyLvl, quality);

        if (result) {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("command", commandInput.getCommand());
            message.put("message", "The robot has successfully moved to position (" +
                    posToMove.getX() + ", " + posToMove.getY() + ").");
            message.put("timestamp", commandInput.getTimestamp());

            output.add(message);

            energyLvl = energyLvl - quality;
        }

        return energyLvl;
    }
}
