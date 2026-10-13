package az.finalproject.tests;

import az.finalproject.client.PaymentClient;
import az.finalproject.model.response.Card;
import az.finalproject.model.response.PaymentHistory;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRAD-303 GET /bill-payments.
 * Bakı vaxtı 23:30–00:30 arasında işlətməyin: "bu gün" filtri gecə yarısı dəyişir.
 */
@Epic("EPIC-3: Kommunal ödənişlər")
@Feature("GRAD-303 GET /bill-payments")
@DisplayName("GRAD-303 GET /bill-payments")
class PaymentHistoryTest extends BaseTest {

    /** 18 ödəniş: 12 Azercell (0.50) + 6 CityNet (5.00) */
    private void createEighteenPayments() {
        Allure.step("Setup: 18 ödəniş (12 Azercell + 6 CityNet)");
        Card card = createCard("1000");
        for (int i = 0; i < 12; i++) pay(card.id, "azercell", "501234567", "0.50");
        for (int i = 0; i < 6; i++) pay(card.id, "citynet", "CN123456", "5");
    }

    private PaymentHistory history(Map<String, ?> query) {
        return PaymentClient.history(token, query).then().statusCode(200).extract().as(PaymentHistory.class);
    }

    @Test
    @Story("Səhifələmə")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_HIS_001/002/003/014 — Səhifələmə: 10 + 8 + boş, ən yeni birinci, schema")
    void pagination() {
        createEighteenPayments();

        Response r = PaymentClient.history(token, Map.of());
        r.then().statusCode(200).body(matchesJsonSchemaInClasspath("schema/fp-payment-history-schema.json"));
        PaymentHistory page1 = r.as(PaymentHistory.class);
        assertThat(page1.page).isEqualTo(1);
        assertThat(page1.limit).isEqualTo(10);
        assertThat(page1.total).isEqualTo(18);
        assertThat(page1.totalPages).isEqualTo(2);
        assertThat(page1.items).hasSize(10);
        assertThat(page1.createdAts()).isSortedAccordingTo(Comparator.reverseOrder());

        assertThat(history(Map.of("page", 2)).items).hasSize(8);

        PaymentHistory page3 = history(Map.of("page", 3));
        assertThat(page3.items).isEmpty();
        assertThat(page3.total).isEqualTo(18);
    }

    @ParameterizedTest(name = "limit={0} → {1}")
    @Story("BVA")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_HIS_004 — limit BVA (1–50)")
    @CsvSource({"0, 400", "1, 200", "50, 200", "51, 400"})
    void limitBoundaries(int limit, int expectedStatus) {
        Card card = createCard("100");
        pay(card.id, "azercell", "501234567", "0.50");
        pay(card.id, "azercell", "501234567", "0.50");

        Response r = PaymentClient.history(token, Map.of("limit", limit));
        if (expectedStatus == 200) {
            PaymentHistory h = r.then().statusCode(200).extract().as(PaymentHistory.class);
            assertThat(h.items).hasSize(Math.min(limit, 2));
        } else {
            assertValidationError(r, "limit");
        }
    }

    @ParameterizedTest(name = "page={0} → 400")
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_HIS_005 — Etibarsız page dəyərləri rədd edilir")
    @ValueSource(strings = {"0", "-1", "abc"})
    void invalidPage(String page) {
        assertValidationError(PaymentClient.history(token, Map.of("page", page)), "page");
    }

    @ParameterizedTest(name = "from={0} → 400")
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_HIS_006/007 — Mövcud olmayan tarix və yanlış format rədd edilir")
    @ValueSource(strings = {"2026-13-01", "2026-02-30", "28-09-2026"})
    void invalidDate(String from) {
        assertValidationError(PaymentClient.history(token, Map.of("from", from)), "from");
    }

    @Test
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_HIS_008 — from > to rədd edilir")
    void fromAfterTo() {
        assertValidationError(PaymentClient.history(token, Map.of("from", "2026-09-30", "to", "2026-09-01")), "from");
    }

    @Test
    @Story("Filtr")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_HIS_009/010/012 — Tarix (inclusive), billerId filtri və filtr + səhifələmə")
    void filters() {
        createEighteenPayments();
        String today = LocalDate.now(ZoneId.of("Asia/Baku")).toString();

        assertThat(history(Map.of("from", today, "to", today, "limit", 50)).total).isEqualTo(18);

        PaymentHistory citynet = history(Map.of("billerId", "citynet"));
        assertThat(citynet.total).isEqualTo(6);
        assertThat(citynet.billerIds()).containsOnly("citynet");

        PaymentHistory azercellPage3 = history(Map.of("billerId", "azercell", "limit", 5, "page", 3));
        assertThat(azercellPage3.total).isEqualTo(12);
        assertThat(azercellPage3.totalPages).isEqualTo(3);
        assertThat(azercellPage3.items).hasSize(2);
    }

    @Test
    @Story("İzolyasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_HIS_013 — Ödənişi olmayan sandbox-da boş siyahı")
    void emptySandbox() {
        PaymentHistory h = history(Map.of());
        assertThat(h.total).isZero();
        assertThat(h.items).isEmpty();
    }
}
