package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Beneficiary {
    public String id;
    public String fullName;
    public String iban;
    public String ibanMasked;
    public String currency;
    public String nickname;
    public String createdAt;
}
