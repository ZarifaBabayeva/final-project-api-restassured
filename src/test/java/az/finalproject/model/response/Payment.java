package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {
    public String id;
    public String receiptNo;
    public String status;
    public PaymentBiller biller;
    public String subscriberNo;
    public BigDecimal amount;
    public BigDecimal fee;
    public BigDecimal total;
    public String currency;
    public String cardId;
    public BigDecimal balanceAfter;
    public String createdAt;
}
