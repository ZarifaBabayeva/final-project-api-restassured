package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentBiller {
    public String id;
    public String name;
    public String category;
}
