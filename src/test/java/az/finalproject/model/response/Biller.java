package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Biller {
    public String id;
    public String name;
    public String category;
    public String subscriberFormat;
    public BigDecimal minAmount;
    public BigDecimal maxAmount;
    public BillerFee fee;
    public String subscriberPattern;
}
