package fileio;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class AnimalInput extends InputParams {
    private String type;
    private String name;
    private double mass;
    private List<PairInput> sections;
    private String status = "healthy";
    private boolean scanned = false;
    private boolean movedThisTurn = false;
    private final String entity = "animals";

    public boolean hasMovedThisTurn() {
        return movedThisTurn;
    }
    public double calculateAttackProbability() {
        int possibility = switch (this.getType()) {
            case "Herbivores" -> 85;
            case "Carnivores" -> 30;
            case "Omnivores" -> 60;
            case "Detritivores" -> 90;
            case "Parasites" -> 10;
            default -> 100;
        };
        return (100.0 - possibility) / 10.0;
    }
}

