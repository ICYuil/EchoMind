package interview.guide.modules.interview.model;

import lombok.Data;

import java.io.Serializable;
@Data
public class TtsDTO implements Serializable {
    public String text;
    public String voice;
    public Double rate;
}
