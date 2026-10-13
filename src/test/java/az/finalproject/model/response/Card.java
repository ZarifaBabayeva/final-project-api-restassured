package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Kart. Block/unblock cavabı da bu class-a map olunur (id, status, blockReason, blockedAt). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    public String id;
    public String accountId;
    public String label;
    public String maskedPan;
    public String expiry;
    public String currency;
    public String status;
    public BigDecimal availableBalance;
    public CardLimits limits;
    public String blockReason;
    public String blockedAt;
    public String createdAt;
    public BigDecimal spentToday;
}
