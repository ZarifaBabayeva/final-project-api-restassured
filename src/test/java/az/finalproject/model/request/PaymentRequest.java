package az.finalproject.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/** POST /bill-payments body */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentRequest {
    public String cardId;
    public String billerId;
    public String subscriberNo;
    public BigDecimal amount;

    public PaymentRequest(String cardId, String billerId, String subscriberNo, String amount) {
        this.cardId = cardId;
        this.billerId = billerId;
        this.subscriberNo = subscriberNo;
        this.amount = amount == null ? null : new BigDecimal(amount);
    }
}
