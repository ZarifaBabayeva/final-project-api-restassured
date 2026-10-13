package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentHistoryItem {
    public String id;
    public String receiptNo;
    public String billerId;
    public String subscriberNo;
    public BigDecimal amount;
    public BigDecimal fee;
    public BigDecimal total;
    public String status;
    public String createdAt;
}
