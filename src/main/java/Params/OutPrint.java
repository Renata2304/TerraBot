package Params;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.*;

import javax.print.attribute.standard.Finishings;
import javax.xml.stream.events.StartDocument;
import java.util.List;
import java.util.Map;

public class OutPrint extends InputParams{

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
                details.put(extraDetail.getKey(), extraDetail.getValue());
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
                    case AirInput air -> airQuality = air.getQualityStatus(air.getAirQuality()).toLowerCase();
                    case SoilInput soil -> soilQuality = soil.getQualityStatus(soil.getSoilQuality()).toLowerCase();
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

        for (int x = 0; x < map.size(); x++) {
            for (int y = 0; y < map.get(x).size(); y++) {
                List<InputParams> cellParams = map.get(x).get(y);
                ObjectNode cellOutput = printHelperMap(objectMapper, cellParams, x, y);
                if (!cellOutput.isEmpty()) {
                    envConditions.add(cellOutput);
                }
            }
        }

        outputWrapper.set("output", envConditions);
        outputWrapper.put("timestamp", timestamp);
    }

}
