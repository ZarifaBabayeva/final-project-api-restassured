package az.finalproject.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Xəta cavabı: {"error", "code", "details"[...], ...}.
 * Backlog qaydası: assert mətnə (error) yox, code-a yazılır.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
    public String error;
    public String code;
    public List<ErrorDetail> details;
    public Integer limit;
    public BigDecimal remaining;
    public String expected;

    public List<String> fields() {
        if (details == null) return Collections.emptyList();
        return details.stream().map(d -> d.field).collect(Collectors.toList());
    }
}
