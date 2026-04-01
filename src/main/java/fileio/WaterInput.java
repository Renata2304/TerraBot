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
public class WaterInput extends InputParams {
    private String type;
    private String name;
    private double mass;
    private double purity;
    private double salinity;
    private double turbidity;
    private double contaminantIndex;
    private double pH;
    private boolean isFrozen;
    private List<PairInput> sections;

    private final String entity = "water";
    private boolean scanned = false;
    private double waterQuality;

    public double calculateWaterQuality() {
        double purityScore = this.purity / 100.0;
        double phScore = 1.0 - (Math.abs(this.pH - 7.5) / 7.5);
        double salinityScore = 1.0 - (this.salinity / 350.0);
        double turbidityScore = 1.0 - (this.turbidity / 100.0);
        double contaminantScore = 1.0 - (this.contaminantIndex / 100.0);
        double frozenScore = this.isFrozen ? 0.0 : 1.0;

        double calculatedQuality = (0.3 * purityScore
                + 0.2 * phScore
                + 0.15 * salinityScore
                + 0.1 * turbidityScore
                + 0.15 * contaminantScore
                + 0.2 * frozenScore) * 100.0;

        double normalizeScore = Math.max(0.0, Math.min(100.0, calculatedQuality));
        this.waterQuality = Math.round(normalizeScore * 100.0) / 100.0;

        return this.waterQuality;
    }

    public String getQualityStatus() {
        if (this.waterQuality < 40.00) {
            return "Poor";
        } else if (this.waterQuality > 69.00) {
            return "Good";
        } else {
            return "Moderate";
        }
    }
}

