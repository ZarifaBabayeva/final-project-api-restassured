package az.finalproject.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;

/** POST /beneficiaries body. null sahələr göndərilmir (nickname opsionaldır). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BeneficiaryRequest {
    public String fullName;
    public String iban;
    public String currency;
    public String nickname;

    public BeneficiaryRequest(String fullName, String iban, String currency) {
        this(fullName, iban, currency, null);
    }

    public BeneficiaryRequest(String fullName, String iban, String currency, String nickname) {
        this.fullName = fullName;
        this.iban = iban;
        this.currency = currency;
        this.nickname = nickname;
    }
}
