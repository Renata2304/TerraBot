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

    private final String entity = "soil";

    private double soilQuality;

    public double calculateSoilQuality() {
        double qualityRaw = switch (this.getType()) {
            case "ForestSoil" -> (nitrogen * 1.2) + (organicMatter * 2) +
                    (waterRetention * 1.5) + (leafLitter * 0.3);
            case "SwampSoil" -> (nitrogen * 1.1) + (organicMatter * 2.2) - (waterLogging * 5);
            case "DesertSoil" -> (nitrogen * 0.5) + (waterRetention * 0.3) - (salinity * 2);
            case "GrasslandSoil" -> (nitrogen * 1.3) + (organicMatter * 1.5) + (rootDensity * 0.8);
            case "TundraSoil" -> (nitrogen * 0.7) + (organicMatter * 0.5) - (permafrostDepth * 1.5);
            default -> 0.0;
        };

        double normalizeScore = Math.clamp(qualityRaw, 0.0, 100.0);
        this.soilQuality = Math.round(normalizeScore * 100.0) / 100.0;
        return this.soilQuality;
    }

    public String getQualityStatus(double soilQuality) {
        if (soilQuality < 40.00) {
            return "Poor";
        } else if (soilQuality > 69.00) {
            return "Good";
        } else {
            return "Moderate";
        }
    }

    public Map.Entry<String, Double> getSpecificSoilField() {
        return switch (this.getType()) {
            case "ForestSoil" -> Map.entry("leafLitter", leafLitter);
            case "SwampSoil" -> Map.entry("waterLogging", waterLogging);
            case "DesertSoil" -> Map.entry("salinity", salinity);
            case "GrasslandSoil" -> Map.entry("rootDensity", rootDensity);
            case "TundraSoil" -> Map.entry("permafrostDepth", permafrostDepth);

            default -> Map.entry("unknown", 0.0);
        };
    }

    public double calculateBlockProbability() {
        double probability = switch (this.getType()) {
            case "ForestSoil" -> (this.waterRetention * 0.6 + this.leafLitter * 0.4) / 80.0 * 100.0;
            case "SwampSoil" -> this.waterLogging * 10.0;
            case "DesertSoil" -> (100.0 - this.waterRetention + this.salinity) / 100.0 * 100.0;
            case "GrasslandSoil" -> ((50.0 - this.rootDensity) + this.waterRetention * 0.5) / 75.0 * 100.0;
            case "TundraSoil" -> (50.0 - this.permafrostDepth) / 50.0 * 100.0;
            default -> 0.0;
        };

        return Math.clamp(probability, 0.0, 100.0);
    }

    @Override
    public List<Map.Entry<String, Double>> getExtraDetails() {
        List<Map.Entry<String,Double>> details = new ArrayList<>();
        details.add(Map.entry("soilQuality", calculateSoilQuality()));
        details.add(Map.entry("nitrogen", nitrogen));
        details.add(Map.entry("waterRetention", waterRetention));
        details.add(Map.entry("soilpH", soilpH));
        details.add(Map.entry("organicMatter", organicMatter));
        details.add(Map.entry(this.getSpecificSoilField().getKey(),
                              this.getSpecificSoilField().getValue()));
        return details;
    }
}

