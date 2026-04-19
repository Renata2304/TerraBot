package params;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.CommandInput;

public class Exceptions extends RuntimeException {
    public Exceptions(String message) {
        super(message);
    }

    public static void printError(final ObjectMapper objectMapper, final ArrayNode output,
                                  final CommandInput commandInput, final String messageToPrint) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("command", commandInput.getCommand());
        message.put("message", messageToPrint);
        message.put("timestamp", commandInput.getTimestamp());

        output.add(message);
    }
}
