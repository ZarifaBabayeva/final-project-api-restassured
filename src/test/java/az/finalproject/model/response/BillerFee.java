package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** type: NONE | PERCENT | FIXED */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillerFee {
    public String type;
    public BigDecimal value;
    public BigDecimal min;
    public BigDecimal max;
}
