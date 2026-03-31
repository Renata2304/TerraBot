package fileio;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class PlantInput extends InputParams {
    private String type;
    private String name;
    private double mass;
    private List<PairInput> sections;

    private String status = "young";
    private boolean scanned = false;
    private final String entity = "plants";
}

