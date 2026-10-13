package az.finalproject.tests;

import az.finalproject.client.BillerClient;
import az.finalproject.model.response.Biller;
import az.finalproject.model.response.BillerList;
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

import java.util.stream.Collectors;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("EPIC-3: Kommunal ödənişlər")
@Feature("GRAD-301 GET /billers")
@DisplayName("GRAD-301 GET /billers")
class BillerTest extends BaseTest {

    private BillerList catalog() {
        return BillerClient.list(token).then().statusCode(200).extract().as(BillerList.class);
    }

    @Test
    @Story("Kataloq")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_BIL_001/009 — Kataloq 6 təchizatçı qaytarır, JSON Schema-ya uyğundur")
    void catalogHasSixBillers() {
        Response r = BillerClient.list(token);
        r.then().statusCode(200).body(matchesJsonSchemaInClasspath("schema/fp-biller-list-schema.json"));
        BillerList list = r.as(BillerList.class);

        assertThat(list.total).isEqualTo(6);
        assertThat(list.items.stream().map(b -> b.id).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("azerisiq", "azersu", "azercell", "bakcell", "citynet", "aztelekom");
    }

    @ParameterizedTest(name = "{0}: {1} — min {2}, max {3}")
    @Story("Kataloq datası")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_BIL_002 — Kataloq datası sənədlə üst-üstə düşür")
    @CsvSource({
            "azerisiq,  UTILITY,  1,   500",
            "azersu,    UTILITY,  1,   300",
            "azercell,  MOBILE,   0.5, 200",
            "bakcell,   MOBILE,   0.5, 200",
            "citynet,   INTERNET, 5,   150",
            "aztelekom, INTERNET, 2,   100"
    })
    void billerData(String id, String category, String min, String max) {
        Biller b = catalog().byId(id);
        assertThat(b.category).isEqualTo(category);
        assertThat(b.minAmount).isEqualByComparingTo(min);
        assertThat(b.maxAmount).isEqualByComparingTo(max);
    }

    @Test
    @Story("Komissiya")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_BIL_003 — Komissiya qaydaları: CityNet 1% (0.20–2.00), Aztelekom sabit 0.50")
    void feeRules() {
        BillerList list = catalog();

        Biller citynet = list.byId("citynet");
        assertThat(citynet.fee.type).isEqualTo("PERCENT");
        assertThat(citynet.fee.value).isEqualByComparingTo("1");
        assertThat(citynet.fee.min).isEqualByComparingTo("0.2");
        assertThat(citynet.fee.max).isEqualByComparingTo("2");

        Biller aztelekom = list.byId("aztelekom");
        assertThat(aztelekom.fee.type).isEqualTo("FIXED");
        assertThat(aztelekom.fee.value).isEqualByComparingTo("0.5");
    }

    @Test
    @Story("Filtr")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_BIL_004 — category=MOBILE yalnız Azercell və Bakcell qaytarır")
    void filterByCategory() {
        BillerList list = BillerClient.listByCategory(token, "MOBILE")
                .then().statusCode(200).extract().as(BillerList.class);

        assertThat(list.total).isEqualTo(2);
        assertThat(list.items).allSatisfy(b -> assertThat(b.category).isEqualTo("MOBILE"));
    }

    @ParameterizedTest(name = "category={0} → 400")
    @Story("Validasiya")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_BIL_007/008 — Etibarsız və kiçik hərfli category rədd edilir")
    @ValueSource(strings = {"GAS", "mobile"})
    void invalidCategoryRejected(String category) {
        assertValidationError(BillerClient.listByCategory(token, category), "category");
    }
}
