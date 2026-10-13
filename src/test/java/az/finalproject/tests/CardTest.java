package az.finalproject.tests;

import az.finalproject.client.CardClient;
import az.finalproject.core.TestData;
import az.finalproject.model.request.CardRequest;
import az.finalproject.model.response.Card;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static az.finalproject.core.ApiConfig.USER1_ACCOUNT;
import static az.finalproject.core.ApiConfig.USER2_ACCOUNT;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("EPIC-2: Virtual kartlar")
@DisplayName("EPIC-2: Virtual kartlar")
class CardTest extends BaseTest {

    // ===================================================================== GRAD-201
    @Nested
    @Feature("GRAD-201 POST /cards")
    @DisplayName("GRAD-201 POST /cards")
    class Create {

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("TC_API_CARD_001 — Etibarlı sorğu ilə virtual kart buraxılır")
        void issuesCard() {
            Response r = CardClient.create(token, new CardRequest(USER1_ACCOUNT, "100", "Kommunal"));
            r.then().statusCode(201);
            Card c = r.as(Card.class);

            assertThat(c.id).matches("^vcd_[a-z0-9]{8}$");
            assertThat(c.status).isEqualTo("ACTIVE");
            assertThat(c.availableBalance).isEqualByComparingTo("100");
            assertThat(c.currency).isEqualTo("AZN");
            assertThat(c.label).isEqualTo("Kommunal");
            assertThat(c.limits.dailyOnline).isEqualByComparingTo("500");
            assertThat(c.limits.perTransaction).isEqualByComparingTo("300");
            assertThat(c.spentToday).isEqualByComparingTo("0");
            assertThat(c.blockReason).isNull();
            assertThat(c.blockedAt).isNull();
            assertThat(r.header("Location")).endsWith("/cards/" + c.id);
        }

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_CARD_002 / SEC_007 — maskedPan, expiry (bu ay + 3 il), tam PAN/CVV qaytarılmır")
        void maskedPanAndExpiry() {
            Response r = CardClient.create(token, new CardRequest(USER1_ACCOUNT, "50"));
            r.then().statusCode(201);
            Card c = r.as(Card.class);

            assertThat(c.maskedPan).matches("^4169 73\\*\\* \\*\\*\\*\\* \\d{4}$");
            assertThat(c.expiry).isEqualTo(TestData.expectedExpiry());
            assertThat(r.jsonPath().getMap("$")).doesNotContainKeys("pan", "cardNumber", "cvv", "cvc");
        }

        @Test
        @Story("Contract")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_CARD_003 — Kart cavabı JSON Schema-ya uyğundur")
        void responseMatchesSchema() {
            CardClient.create(token, new CardRequest(USER1_ACCOUNT, "20"))
                    .then().statusCode(201)
                    .body(matchesJsonSchemaInClasspath("schema/fp-card-schema.json"));
        }

        @ParameterizedTest(name = "initialAmount {0} → {1}")
        @Story("BVA")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_CARD_005–008 — initialAmount BVA (1.00–1000.00)")
        @CsvSource({"0.99, 400", "1.00, 201", "1000.00, 201", "1000.01, 400"})
        void initialAmountBoundaries(String amount, int expectedStatus) {
            Response r = CardClient.create(token, new CardRequest(USER1_ACCOUNT, amount));
            if (expectedStatus == 201) {
                Card c = r.then().statusCode(201).extract().as(Card.class);
                assertThat(c.availableBalance).isEqualByComparingTo(amount);
            } else {
                assertValidationError(r, "initialAmount");
            }
        }

        @ParameterizedTest(name = "initialAmount {0} → 400")
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_CARD_010/011 — 0, mənfi və 3 onluqlu məbləğ rədd edilir")
        @ValueSource(strings = {"0", "-10", "10.555"})
        void invalidInitialAmountRejected(String amount) {
            assertValidationError(CardClient.create(token, new CardRequest(USER1_ACCOUNT, amount)), "initialAmount");
        }

        @Test
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_CARD_009 — Məbləğ mətn kimi (\"50\") rədd edilir")
        void amountAsStringRejected() {
            assertValidationError(CardClient.create(token, Map.of("accountId", USER1_ACCOUNT, "initialAmount", "50")),
                    "initialAmount");
        }

        @Test
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_CARD_013 — Məcburi accountId olmadan rədd edilir")
        void missingAccountRejected() {
            assertValidationError(CardClient.create(token, Map.of("initialAmount", 100)), "accountId");
        }

        @Test
        @Story("Təhlükəsizlik")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("TC_API_CARD_014 — Başqa istifadəçinin hesabı ilə kart açıla bilmir (403, AC-5)")
        void foreignAccountForbidden() {
            assertErrorCode(CardClient.create(token, new CardRequest(USER2_ACCOUNT, "100")), 403, "FORBIDDEN");
        }

        @ParameterizedTest(name = "user2 ({2} hesab): {0} → {1}")
        @Story("BVA")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_CARD_016/017 — Hesab balansı BVA (user2 balansı 890.50 USD)")
        @CsvSource({"891, 422, USD", "890.50, 201, USD"})
        void accountBalanceBoundary(String amount, int expectedStatus, String currency) {
            Response r = CardClient.create(user2Token(), new CardRequest(USER2_ACCOUNT, amount));
            if (expectedStatus == 201) {
                Card c = r.then().statusCode(201).extract().as(Card.class);
                assertThat(c.currency).isEqualTo(currency);
                assertThat(c.availableBalance).isEqualByComparingTo(amount);
            } else {
                assertErrorCode(r, 422, "INSUFFICIENT_FUNDS");
            }
        }

        @ParameterizedTest(name = "3 kart, biri {0} → 4-cü kart {1}")
        @Story("Kart limiti")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_CARD_018–020 — Maks. 3 ACTIVE/FROZEN kart; BLOCKED sayılmır")
        @CsvSource({"ACTIVE, 422", "TEMPORARY, 422", "LOST, 201"})
        void cardLimit(String thirdCardAction, int expectedStatus) {
            createCard("10");
            createCard("10");
            Card third = createCard("10");
            if (!thirdCardAction.equals("ACTIVE")) CardClient.block(token, third.id, thirdCardAction).then().statusCode(200);

            Response r = CardClient.create(token, new CardRequest(USER1_ACCOUNT, "10"));
            if (expectedStatus == 201) r.then().statusCode(201);
            else assertErrorCode(r, 422, "CARD_LIMIT_REACHED");
        }
    }

    // ===================================================================== GRAD-202
    @Nested
    @Feature("GRAD-202 GET /cards/{id}")
    @DisplayName("GRAD-202 GET /cards/{id}")
    class Get {

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_CARDG_001 — GET cavabı POST cavabı ilə eynidir")
        void getReturnsSameCard() {
            Card created = createCard("100");
            Card fetched = CardClient.get(token, created.id).then().statusCode(200).extract().as(Card.class);

            assertThat(fetched)
                    .usingRecursiveComparison()
                    .withComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                    .isEqualTo(created);
        }

        @Test
        @Story("Balans")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_CARDG_002 — Ödənişdən sonra availableBalance azalır, spentToday artır")
        void balanceAndSpentTodayAfterPayment() {
            Card card = createCard("100");
            pay(card.id, "azercell", "501234567", "50.50");

            Card after = CardClient.get(token, card.id).then().statusCode(200).extract().as(Card.class);
            assertThat(after.availableBalance).isEqualByComparingTo("49.5");
            assertThat(after.spentToday).isEqualByComparingTo("50.5");
        }

        @ParameterizedTest(name = "{0} → 404")
        @Story("Təhlükəsizlik (IDOR)")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("TC_API_CARDG_003/004 — Başqasının və mövcud olmayan kart 404")
        @ValueSource(strings = {"başqasının kartı", "mövcud olmayan kart"})
        void cardNotVisible(String caseName) {
            Card card = createCard("100");
            Response r = caseName.startsWith("başqa")
                    ? CardClient.get(user2Token(), card.id)
                    : CardClient.get(token, "vcd_00000000");
            assertErrorCode(r, 404, "NOT_FOUND");
        }
    }
}
