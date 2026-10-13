package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BillerList {
    public int total;
    public List<Biller> items;

    public Biller byId(String id) {
        return items.stream().filter(b -> b.id.equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Kataloqda təchizatçı yoxdur: " + id));
    }
}
