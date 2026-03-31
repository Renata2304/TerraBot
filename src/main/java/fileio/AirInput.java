package fileio;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class AirInput extends InputParams {
    private String type;
    private String name;
    private double mass;

    private double humidity;
    private double temperature;
    private double oxygenLevel;
    private double altitude;
    private double pollenLevel;
    private double co2Level;
    private double iceCrystalConcentration;
    private double dustParticles;
    private List<PairInput> sections;

    private final String entity = "air";
    private double airQuality;

    private double calculateAirQuality() {
        double calculatedQuality = switch (this.getType()) {
            case "TropicalAir" -> (this.oxygenLevel * 2) +
                    (this.humidity * 0.5) -
                    (this.co2Level * 0.01);
            case "PolarAir" -> (this.oxygenLevel * 2) +
                    (100 - Math.abs(this.temperature)) -
                    (this.iceCrystalConcentration * 0.05);
            case "TemperateAir" -> (this.oxygenLevel * 2) +
                    (this.humidity * 0.7) -
                    (this.pollenLevel * 0.1);
            case "DesertAir" -> (this.oxygenLevel * 2) -
                    (this.dustParticles * 0.2) -
                    (this.temperature * 0.3);
            case "MountainAir" -> {
                double oxygenFactor = this.oxygenLevel -
                        (this.altitude / 1000 * 0.5);
                yield (oxygenFactor * 2) + (this.humidity * 0.6);
            }
            default -> 0.0;
        };
        this.airQuality = calculatedQuality;
        return calculatedQuality;
    }

    public String getQualityStatus(double airQuality) {
        if (airQuality < 40.00) {
            return "Poor";
        } else if (airQuality > 69.00) {
            return "Good";
        } else {
            return "Moderate";
        }
    }

    public List<Map.Entry<String, Double>> getExtraDetails() {
        List<Map.Entry<String,Double>> details = new ArrayList<>();
        details.add(Map.entry("airQuality", this.calculateAirQuality()));
        details.add(Map.entry("altitude", this.altitude));
        details.add(Map.entry("humidity", this.humidity));
        details.add(Map.entry("temperature", this.temperature));
        details.add(Map.entry("oxygenLevel", this.oxygenLevel));
        return details;
    }
}

