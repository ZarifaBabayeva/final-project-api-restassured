package az.finalproject.tests;

import az.finalproject.client.CardClient;
import az.finalproject.client.PaymentClient;
import az.finalproject.model.request.PaymentRequest;
import az.finalproject.model.response.Card;
import az.finalproject.model.response.ErrorResponse;
import az.finalproject.model.response.Payment;
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

import java.util.Map;
import java.util.UUID;

import static az.finalproject.core.ApiConfig.USER2_ACCOUNT;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRAD-302 POST /bill-payments.
 * Bakı vaxtı 23:30–00:30 arasında işlətməyin: spentToday gecə yarısı sıfırlanır.
 */
@Epic("EPIC-3: Kommunal ödənişlər")
@Feature("GRAD-302 POST /bill-payments")
@DisplayName("GRAD-302 POST /bill-payments")
class BillPaymentTest extends BaseTest {

    private Response payWith(String cardId, String billerId, String subscriberNo, String amount) {
        return PaymentClient.pay(token, new PaymentRequest(cardId, billerId, subscriberNo, amount));
    }

    @Test
    @Story("Pozitiv")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_PAY_001/002 — CityNet-ə ödəniş: komissiya, total, balanceAfter, Location, schema")
    void successfulPayment() {
        Card card = createCard("1000");
        Response r = payWith(card.id, "citynet", "CN123456", "50");
        r.then().statusCode(201).body(matchesJsonSchemaInClasspath("schema/fp-payment-schema.json"));
        Payment p = r.as(Payment.class);

        assertThat(p.status).isEqualTo("SUCCESS");
        assertThat(p.amount).isEqualByComparingTo("50");
        assertThat(p.fee).isEqualByComparingTo("0.5");
        assertThat(p.total).isEqualByComparingTo("50.5");
        assertThat(p.balanceAfter).isEqualByComparingTo("949.5");
        assertThat(p.currency).isEqualTo("AZN");
        assertThat(p.biller.id).isEqualTo("citynet");
        assertThat(p.biller.category).isEqualTo("INTERNET");
        assertThat(r.header("Location")).endsWith("/bill-payments/" + p.id);
    }

    @Test
    @Story("Balans")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_003 — Kart balansı və spentToday total qədər dəyişir")
    void cardBalanceChangesByTotal() {
        Card card = createCard("1000");
        pay(card.id, "citynet", "CN123456", "20");

        Card after = CardClient.get(token, card.id).then().statusCode(200).extract().as(Card.class);
        assertThat(after.availableBalance).isEqualByComparingTo("979.8");
        assertThat(after.spentToday).isEqualByComparingTo("20.2");
    }

    @Test
    @Story("Qəbz nömrəsi")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_004 — receiptNo formatı RCP-YYYYMMDD-NNNNNN və ardıcıl artım")
    void receiptNumbersAreSequential() {
        Card card = createCard("1000");
        assertThat(pay(card.id, "azercell", "501234567", "1").receiptNo).matches("^RCP-\\d{8}-000001$");
        assertThat(pay(card.id, "azercell", "501234567", "1").receiptNo).matches("^RCP-\\d{8}-000002$");
    }

    // ---------------------------------------------------------------- Idempotency

    @Test
    @Story("Idempotency")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_PAY_005 — Eyni açar + eyni body: 200 replay, eyni id, pul bir dəfə çıxır")
    void replayDoesNotChargeTwice() {
        Card card = createCard("1000");
        String key = UUID.randomUUID().toString();
        PaymentRequest body = new PaymentRequest(card.id, "citynet", "CN123456", "50");

        Payment first = PaymentClient.pay(token, body, key).then().statusCode(201).extract().as(Payment.class);
        Response replay = PaymentClient.pay(token, body, key);
        replay.then().statusCode(200);

        assertThat(replay.as(Payment.class).id).isEqualTo(first.id);
        assertThat(replay.header("Idempotent-Replay")).isEqualTo("true");
        Card after = CardClient.get(token, card.id).then().statusCode(200).extract().as(Card.class);
        assertThat(after.availableBalance).isEqualByComparingTo("949.5");
    }

    @Test
    @Story("Idempotency")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_006 — Eyni açar fərqli məbləğlə → 409 IDEMPOTENCY_CONFLICT")
    void sameKeyDifferentBodyConflicts() {
        Card card = createCard("1000");
        String key = UUID.randomUUID().toString();
        PaymentClient.pay(token, new PaymentRequest(card.id, "citynet", "CN123456", "50"), key).then().statusCode(201);

        assertErrorCode(PaymentClient.pay(token, new PaymentRequest(card.id, "citynet", "CN123456", "60"), key),
                409, "IDEMPOTENCY_CONFLICT");
    }

    @Test
    @Story("Idempotency")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_007 — Idempotency-Key olmadan ödəniş rədd edilir")
    void keyIsRequired() {
        Card card = createCard("1000");
        assertErrorCode(PaymentClient.pay(token, new PaymentRequest(card.id, "azercell", "501234567", "5"), null),
                400, "IDEMPOTENCY_KEY_REQUIRED");
    }

    @ParameterizedTest(name = "açar uzunluğu {0} → {1}")
    @Story("BVA")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_008 — Idempotency-Key uzunluğu BVA (8–64)")
    @CsvSource({"7, 400", "8, 201", "64, 201", "65, 400"})
    void keyLengthBoundaries(int length, int expectedStatus) {
        Card card = createCard("1000");
        String key = "k".repeat(length);
        Response r = PaymentClient.pay(token, new PaymentRequest(card.id, "azercell", "501234567", "1"), key);
        if (expectedStatus == 201) r.then().statusCode(201);
        else assertErrorCode(r, 400, "IDEMPOTENCY_KEY_INVALID");
    }

    @Test
    @Story("Idempotency")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_009 — Uğursuz sorğunun açarı yenidən istifadə oluna bilər")
    void failedRequestKeyCanBeReused() {
        Card card = createCard("1000");
        String key = UUID.randomUUID().toString();
        assertErrorCode(PaymentClient.pay(token, new PaymentRequest(card.id, "azercell", "123", "5"), key),
                422, "INVALID_SUBSCRIBER");
        PaymentClient.pay(token, new PaymentRequest(card.id, "azercell", "501234567", "5"), key).then().statusCode(201);
    }

    // ---------------------------------------------------------------- Abunəçi formatı

    @ParameterizedTest(name = "{0} {1} → 201")
    @Story("Abunəçi formatı")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_011 — Hər təchizatçının etibarlı abunəçi formatı qəbul edilir")
    @CsvSource({
            "azerisiq,  1234567890, 1",
            "azersu,    12345678,   1",
            "azercell,  501234567,  0.50",
            "azercell,  511234567,  0.50",
            "azercell,  101234567,  0.50",
            "bakcell,   551234567,  0.50",
            "bakcell,   991234567,  0.50",
            "citynet,   CN123456,   5",
            "aztelekom, 1234567,    2"
    })
    void validSubscriberFormats(String billerId, String subscriberNo, String amount) {
        Card card = createCard("1000");
        assertThat(pay(card.id, billerId, subscriberNo, amount).status).isEqualTo("SUCCESS");
    }

    @ParameterizedTest(name = "{0} {1} → 422")
    @Story("Abunəçi formatı")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_010/012 — Səhv abunəçi formatı və başqa operatorun nömrəsi rədd edilir")
    @CsvSource({
            "azerisiq,  123456789",
            "azersu,    123456789",
            "azercell,  701234567",
            "azercell,  551234567",
            "bakcell,   501234567",
            "citynet,   CN12345",
            "citynet,   cn123456",
            "aztelekom, 123456"
    })
    void invalidSubscriberFormats(String billerId, String subscriberNo) {
        Card card = createCard("1000");
        ErrorResponse e = error(payWith(card.id, billerId, subscriberNo, "5"), 422);
        assertThat(e.code).isEqualTo("INVALID_SUBSCRIBER");
        assertThat(e.expected).isNotBlank();
    }

    // ---------------------------------------------------------------- Məbləğ və komissiya

    @ParameterizedTest(name = "Azercell {0} → {1}")
    @Story("BVA")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_013 — Azercell məbləğ aralığı BVA (0.50–200.00)")
    @CsvSource({"0.49, 422", "0.50, 201", "200.00, 201", "200.01, 422"})
    void azercellAmountBoundaries(String amount, int expectedStatus) {
        Card card = createCard("1000");
        Response r = payWith(card.id, "azercell", "501234567", amount);
        if (expectedStatus == 201) r.then().statusCode(201);
        else assertErrorCode(r, 422, "AMOUNT_OUT_OF_RANGE");
    }

    @ParameterizedTest(name = "{0} {1} → fee {2}, total {3}")
    @Story("Komissiya")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_014–017 — Komissiya hesablanması (faiz, min/max, yuvarlaqlaşdırma, sabit, komissiyasız)")
    @CsvSource({
            "citynet,   CN123456,   5,     0.2,  5.2",
            "citynet,   CN123456,   20,    0.2,  20.2",
            "citynet,   CN123456,   21,    0.21, 21.21",
            "citynet,   CN123456,   150,   1.5,  151.5",
            "citynet,   CN123456,   33.33, 0.33, 33.66",
            "aztelekom, 1234567,    2,     0.5,  2.5",
            "aztelekom, 1234567,    100,   0.5,  100.5",
            "azercell,  501234567,  10,    0,    10"
    })
    void feeCalculation(String billerId, String subscriberNo, String amount, String fee, String total) {
        Card card = createCard("1000");
        Payment p = pay(card.id, billerId, subscriberNo, amount);
        assertThat(p.fee).isEqualByComparingTo(fee);
        assertThat(p.total).isEqualByComparingTo(total);
    }

    @ParameterizedTest(name = "məbləğ {0} → 400")
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_025 — Məbləğ 0, mənfi və 3 onluqlu ola bilməz")
    @ValueSource(strings = {"0", "-5", "10.555"})
    void invalidAmountRejected(String amount) {
        Card card = createCard("1000");
        assertValidationError(payWith(card.id, "azercell", "501234567", amount), "amount");
    }

    @Test
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_024 — Məbləğ mətn kimi (\"10\") rədd edilir")
    void amountAsStringRejected() {
        Card card = createCard("1000");
        assertValidationError(PaymentClient.pay(token, Map.of(
                "cardId", card.id, "billerId", "azercell", "subscriberNo", "501234567", "amount", "10")), "amount");
    }

    @Test
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_028 — Boş body: hər məcburi sahə details-də")
    void emptyBodyReportsAllFields() {
        ErrorResponse e = error(PaymentClient.pay(token, Map.of()), 400);
        assertThat(e.code).isEqualTo("VALIDATION_ERROR");
        assertThat(e.fields()).contains("cardId", "billerId", "subscriberNo", "amount");
    }

    // ---------------------------------------------------------------- Limitlər və balans

    @ParameterizedTest(name = "total {0} → {1}")
    @Story("Limitlər")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_018 — Tək əməliyyat limiti BVA (300.00)")
    @CsvSource({"300.00, 201", "300.01, 422"})
    void perTransactionLimit(String amount, int expectedStatus) {
        Card card = createCard("1000");
        Response r = payWith(card.id, "azerisiq", "1234567890", amount);
        if (expectedStatus == 201) r.then().statusCode(201);
        else assertErrorCode(r, 422, "PER_TRANSACTION_LIMIT");
    }

    @ParameterizedTest(name = "bu gün {0} xərclənib, {1} ödəniş → remaining {2}")
    @Story("Limitlər")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_019/020 — Gündəlik limit (500) aşıldıqda remaining düzgün qaytarılır")
    @CsvSource({"200, 1, 0", "150, 60, 50"})
    void dailyLimit(String secondPayment, String attempt, String expectedRemaining) {
        Card card = createCard("1000");
        pay(card.id, "azerisiq", "1234567890", "300");
        pay(card.id, "azerisiq", "1234567890", secondPayment);

        ErrorResponse e = error(payWith(card.id, "azersu", "12345678", attempt), 422);
        assertThat(e.code).isEqualTo("DAILY_LIMIT_EXCEEDED");
        assertThat(e.remaining).isEqualByComparingTo(expectedRemaining);
    }

    @ParameterizedTest(name = "balans 49.50, ödəniş {0} → {1}")
    @Story("Balans")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_021 — Kart balansı BVA (49.50)")
    @CsvSource({"49.51, 422", "49.50, 201"})
    void cardBalanceBoundary(String amount, int expectedStatus) {
        Card card = createCard("49.50");
        Response r = payWith(card.id, "azerisiq", "1234567890", amount);
        if (expectedStatus == 201) {
            assertThat(r.then().statusCode(201).extract().as(Payment.class).balanceAfter).isEqualByComparingTo("0");
        } else {
            assertErrorCode(r, 422, "INSUFFICIENT_FUNDS");
        }
    }

    @Test
    @Story("Balans")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_PAY_022 — Balans total (məbləğ + komissiya) üzrə yoxlanılır: 49.60 + 0.50 > 50")
    void balanceCheckUsesTotal() {
        Card card = createCard("50");
        assertErrorCode(payWith(card.id, "citynet", "CN123456", "49.6"), 422, "INSUFFICIENT_FUNDS");
    }

    @Test
    @Story("Valyuta")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_PAY_023 — USD kartla kommunal ödəniş rədd edilir")
    void usdCardNotSupported() {
        String user2 = user2Token();
        Card usdCard = createCard(user2, USER2_ACCOUNT, "10");
        Response r = PaymentClient.pay(user2, new PaymentRequest(usdCard.id, "azercell", "501234567", "5"));
        assertErrorCode(r, 422, "CURRENCY_NOT_SUPPORTED");
    }

    @Test
    @Story("Təhlükəsizlik (IDOR)")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_PAY_026/027 — Mövcud olmayan təchizatçı, mövcud olmayan və başqasının kartı")
    void unknownBillerAndForeignCard() {
        Card own = createCard("100");
        assertThat(error(payWith(own.id, "unknown", "123", "5"), 404).code).isNotBlank();
        assertErrorCode(payWith("vcd_00000000", "azercell", "501234567", "5"), 404, "CARD_NOT_FOUND");

        Card foreign = createCard(user2Token(), USER2_ACCOUNT, "10");
        assertErrorCode(payWith(foreign.id, "azercell", "501234567", "5"), 404, "CARD_NOT_FOUND");
    }
}
