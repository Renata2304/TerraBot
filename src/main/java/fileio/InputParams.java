package fileio;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public abstract class InputParams {
    protected String entity;

    protected String type;
    protected String name;
    protected double mass;
    protected List<Map.Entry<String,Double>> extraDetails;


}
