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
    private double growthLevel = 0.0;
    private String status = "young";
    private boolean scanned =  false;

    private final String entity = "plants";

    public double getOxygenContribution() {
        double baseOxygen = switch (this.getType()) {
            case "FloweringPlants" -> 6.0;
            case "Mosses" -> 0.8;
            case "Algae" -> 0.5;
            default -> 0.0;
        };

        double maturityBonus = switch (this.status.toLowerCase()) {
            case "young" -> 0.2;
            case "mature" -> 0.7;
            case "old" -> 0.4;
            default -> 0.0;
        };

        return baseOxygen + maturityBonus;
    }

    public double calculateBlockProbability() {
        int possibility = switch (this.getType()) {
            case "FloweringPlants", "Flowering Plants (Angiosperms)" -> 90;
            case "GymnospermsPlants", "Gymnosperms" -> 60;
            case "Ferns" -> 30;
            case "Mosses" -> 40;
            case "Algae" -> 20;
            default -> 0;
        };
        return possibility / 100.0;
    }

    public void addGrowth(double amount) {
        this.growthLevel += amount;

        if (this.growthLevel > 1.0) {
            this.growthLevel = 0.0;

            switch (this.status) {
                case "young" -> this.status = "mature";
                case "mature" -> this.status = "old";
                case "old" -> this.status = "dead";
            }
        }
    }
}

