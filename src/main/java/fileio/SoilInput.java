package fileio;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class SoilInput extends InputParams {
    private String type;
    private String name;
    private double mass;
    private double nitrogen;
    private double waterRetention;
    private double soilpH;
    private double organicMatter;
    private double leafLitter;
    private double waterLogging;
    private double permafrostDepth;
    private double rootDensity;
    private double salinity;
    private List<PairInput> sections;

    public List<Map.Entry<String, Double>> getExtraDetails() {
        List<Map.Entry<String,Double>> details = new ArrayList<>();
        details.add(Map.entry("nitrogen", nitrogen));
        details.add(Map.entry("water", waterRetention));
        details.add(Map.entry("soilpH", soilpH));
        details.add(Map.entry("organicMatter", organicMatter));
        details.add(Map.entry("leafLitter", leafLitter));
        details.add(Map.entry("waterLogging", waterLogging));
        return details;
    }
}

