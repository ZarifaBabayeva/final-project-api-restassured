package az.finalproject.tests;

import az.finalproject.client.CardClient;
import az.finalproject.client.PaymentClient;
import az.finalproject.core.CardState;
import az.finalproject.model.request.PaymentRequest;
import az.finalproject.model.response.Card;
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
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRAD-203 / GRAD-204 — kart vəziyyətlərinin keçid cədvəli (state transition).
 * Cədvəlin hər sətri ayrıca test kimi icra olunur və Allure-da ayrıca görünür.
 */
@Epic("EPIC-2: Virtual kartlar")
@Feature("GRAD-203/204 Kartı bloklamaq / açmaq")
@DisplayName("GRAD-203/204 Kart vəziyyəti keçidləri")
class CardStateTransitionTest extends BaseTest {

    @ParameterizedTest(name = "{0} + {1}({2}) → {3} {4}")
    @Story("State transition cədvəli")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_BLK_001–009, UNB_001–004 — Bütün keçidlər")
    @CsvSource({
            // başlanğıc,     əməliyyat, reason,    status, gözlənilən (status və ya code)
            "ACTIVE,          block,     TEMPORARY, 200,    FROZEN",
            "ACTIVE,          block,     LOST,      200,    BLOCKED",
            "ACTIVE,          block,     STOLEN,    200,    BLOCKED",
            "FROZEN,          block,     TEMPORARY, 409,    ALREADY_FROZEN",
            "FROZEN,          block,     LOST,      200,    BLOCKED",
            "FROZEN,          block,     STOLEN,    200,    BLOCKED",
            "BLOCKED_LOST,    block,     TEMPORARY, 409,    CARD_BLOCKED",
            "BLOCKED_LOST,    block,     LOST,      409,    -",
            "BLOCKED_STOLEN,  block,     STOLEN,    409,    -",
            "FROZEN,          unblock,   -,         200,    ACTIVE",
            "ACTIVE,          unblock,   -,         409,    NOT_FROZEN",
            "BLOCKED_LOST,    unblock,   -,         409,    PERMANENTLY_BLOCKED",
            "BLOCKED_STOLEN,  unblock,   -,         409,    PERMANENTLY_BLOCKED"
    })
    void transition(CardState from, String action, String reason, int expectedStatus, String expected) {
        Card card = createCard("10");
        moveCardTo(card.id, from);

        Response r = action.equals("block")
                ? CardClient.block(token, card.id, reason)
                : CardClient.unblock(token, card.id);

        r.then().statusCode(expectedStatus);
        if (expectedStatus == 200) {
            Card result = r.as(Card.class);
            assertThat(result.status).isEqualTo(expected);
            if (expected.equals("ACTIVE")) {
                assertThat(result.blockReason).isNull();
                assertThat(result.blockedAt).isNull();
            } else {
                assertThat(result.blockReason).isEqualTo(reason);
                assertThat(result.blockedAt).isNotBlank();
            }
        } else if (!expected.equals("-")) {
            assertThat(r.jsonPath().getString("code")).isEqualTo(expected);
        }
    }

    @Test
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_BLK_010 — Etibarsız reason rədd edilir, kart ACTIVE qalır")
    void invalidReasonKeepsCardActive() {
        Card card = createCard("10");
        assertValidationError(CardClient.block(token, card.id, "BROKEN"), "reason");

        Card after = CardClient.get(token, card.id).then().statusCode(200).extract().as(Card.class);
        assertThat(after.status).isEqualTo("ACTIVE");
    }

    @Test
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_BLK_011 — reason sahəsi olmadan rədd edilir")
    void missingReasonRejected() {
        Card card = createCard("10");
        assertValidationError(CardClient.block(token, card.id, Map.of()), "reason");
    }

    @Test
    @Story("Təhlükəsizlik (IDOR)")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_BLK_016 — Başqa istifadəçinin kartı bloklana bilmir (404), kart ACTIVE qalır")
    void otherUserCannotBlock() {
        Card card = createCard("10");
        assertErrorCode(CardClient.block(user2Token(), card.id, "LOST"), 404, "NOT_FOUND");

        Card after = CardClient.get(token, card.id).then().statusCode(200).extract().as(Card.class);
        assertThat(after.status).isEqualTo("ACTIVE");
    }

    @ParameterizedTest(name = "{0} kartla ödəniş → 422 CARD_NOT_ACTIVE")
    @Story("Bloklanmış kartla ödəniş")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_BLK_017/018 — FROZEN və BLOCKED kartla ödəniş rədd edilir, balans dəyişmir")
    @EnumSource(value = CardState.class, names = {"FROZEN", "BLOCKED_LOST"})
    void notActiveCardCannotPay(CardState state) {
        Card card = createCard("100");
        moveCardTo(card.id, state);

        assertErrorCode(PaymentClient.pay(token, new PaymentRequest(card.id, "azercell", "501234567", "10")),
                422, "CARD_NOT_ACTIVE");

        Card after = CardClient.get(token, card.id).then().statusCode(200).extract().as(Card.class);
        assertThat(after.availableBalance).isEqualByComparingTo("100");
    }

    @Test
    @Story("Bloklanmış kartla ödəniş")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_UNB_005 — Açılmış kartla ödəniş uğurla keçir")
    void unblockedCardCanPay() {
        Card card = createCard("100");
        moveCardTo(card.id, CardState.FROZEN);
        CardClient.unblock(token, card.id).then().statusCode(200);

        assertThat(pay(card.id, "azercell", "501234567", "10").balanceAfter).isEqualByComparingTo("90");
    }
}
