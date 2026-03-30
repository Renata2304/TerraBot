package Params;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.*;

import javax.print.attribute.standard.Finishings;
import javax.xml.stream.events.StartDocument;
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

}
