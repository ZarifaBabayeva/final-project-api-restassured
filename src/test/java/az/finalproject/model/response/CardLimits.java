package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CardLimits {
    public BigDecimal dailyOnline;
    public BigDecimal perTransaction;
}
