package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentHistory {
    public int page;
    public int limit;
    public int total;
    public int totalPages;
    public List<PaymentHistoryItem> items;

    public List<String> createdAts() {
        return items.stream().map(i -> i.createdAt).collect(Collectors.toList());
    }

    public List<String> billerIds() {
        return items.stream().map(i -> i.billerId).collect(Collectors.toList());
    }
}
