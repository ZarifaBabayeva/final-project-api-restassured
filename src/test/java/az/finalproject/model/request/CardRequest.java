package az.finalproject.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/** POST /cards body. Pul sahəsi BigDecimal — float yuvarlaqlaşdırma xətası olmasın deyə. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardRequest {
    public String accountId;
    public BigDecimal initialAmount;
    public String label;

    public CardRequest(String accountId, String initialAmount) {
        this(accountId, initialAmount, null);
    }

    public CardRequest(String accountId, String initialAmount, String label) {
        this.accountId = accountId;
        this.initialAmount = initialAmount == null ? null : new BigDecimal(initialAmount);
        this.label = label;
    }
}
