package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeneficiaryList {
    public int total;
    public List<Beneficiary> items;

    public List<String> fullNames() {
        return items.stream().map(b -> b.fullName).collect(Collectors.toList());
    }

    public List<String> ids() {
        return items.stream().map(b -> b.id).collect(Collectors.toList());
    }
}
