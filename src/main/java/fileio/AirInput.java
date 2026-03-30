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

    public List<Map.Entry<String, Double>> getExtraDetails() {
        List<Map.Entry<String,Double>> details = new ArrayList<>();
        details.add(Map.entry("humidity", this.humidity));
        details.add(Map.entry("temperature", this.temperature));
        details.add(Map.entry("oxygenLevel", this.oxygenLevel));
        details.add(Map.entry("altitude", this.altitude));
        details.add(Map.entry("pollenLevel", this.pollenLevel));
        details.add(Map.entry("co2Level", this.co2Level));
        details.add(Map.entry("iceCrystalConcentration", this.iceCrystalConcentration));
        details.add(Map.entry("dustParticles", this.dustParticles));
        return details;
    }
}

