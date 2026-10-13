package az.finalproject.tests;

import az.finalproject.client.AuthClient;
import az.finalproject.client.BeneficiaryClient;
import az.finalproject.core.ApiSpec;
import az.finalproject.core.TestData;
import az.finalproject.model.response.BeneficiaryList;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static az.finalproject.core.ApiConfig.PASSWORD;
import static az.finalproject.core.ApiConfig.USER1_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Ümumi qaydalar")
@Feature("Auth və təhlükəsizlik")
@DisplayName("Auth və ümumi qaydalar")
class AuthSecurityTest extends BaseTest {

    @Test
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_AUTH_001 — Etibarlı login token qaytarır, şifrə cavabda yoxdur")
    void validLoginReturnsToken() {
        Response r = AuthClient.login(USER1_EMAIL, PASSWORD);
        r.then().statusCode(200);
        assertThat(r.jsonPath().getString("token")).isNotBlank();
        assertThat(r.jsonPath().getString("user.email")).isEqualTo(USER1_EMAIL);
        assertThat(r.asString()).doesNotContain(PASSWORD);
    }

    @Test
    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_AUTH_002 — Yanlış şifrə ilə login 401, token verilmir")
    void wrongPasswordReturns401() {
        Response r = AuthClient.login(USER1_EMAIL, "Wrong999");
        r.then().statusCode(401);
        assertThat(r.jsonPath().getString("token")).isNull();
    }

    @ParameterizedTest(name = "{0} {1} → 401")
    @Story("Token tələbi")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC_API_SEC_001 — Token olmadan qorunan endpoint-lər 401 qaytarır")
    @CsvSource({
            "GET,    /beneficiaries",
            "POST,   /beneficiaries",
            "DELETE, /beneficiaries/ben_00000000",
            "POST,   /cards",
            "GET,    /cards/vcd_00000000",
            "POST,   /cards/vcd_00000000/block",
            "POST,   /cards/vcd_00000000/unblock",
            "GET,    /billers",
            "POST,   /bill-payments",
            "GET,    /bill-payments"
    })
    void protectedEndpointsRequireToken(String method, String path) {
        RequestSpecification request = ApiSpec.anonymous();
        if (method.equals("POST")) request.contentType(ContentType.JSON).body("{}");
        request.request(method, path).then().statusCode(401);
    }

    @ParameterizedTest(name = "Authorization: \"{0}\" → 401")
    @Story("Token tələbi")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC_API_SEC_002/003 — Saxta token və Bearer prefiksi olmayan token rədd edilir")
    @CsvSource({"Bearer abc.invalid.token", "RAW_TOKEN"})
    void invalidAuthorizationHeaderRejected(String header) {
        String value = header.equals("RAW_TOKEN") ? token : header;
        ApiSpec.anonymous().header("Authorization", value)
                .get("/beneficiaries")
                .then().statusCode(401);
    }

    @Test
    @Story("Sandbox izolyasiyası")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC_API_SEC_005 — Yeni login yeni, boş sandbox açır")
    void newLoginOpensEmptySandbox() {
        createBeneficiary("İzolyasiya Test", TestData.iban(900), "AZN", null);

        String newToken = login(USER1_EMAIL);
        BeneficiaryList list = BeneficiaryClient.list(newToken)
                .then().statusCode(200).extract().as(BeneficiaryList.class);

        assertThat(list.total).isZero();
        assertThat(list.items).isEmpty();
    }
}
