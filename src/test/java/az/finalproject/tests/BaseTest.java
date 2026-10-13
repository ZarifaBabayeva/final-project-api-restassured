package az.finalproject.tests;

import az.finalproject.client.AuthClient;
import az.finalproject.client.BeneficiaryClient;
import az.finalproject.client.CardClient;
import az.finalproject.client.PaymentClient;
import az.finalproject.core.CardState;
import az.finalproject.model.request.BeneficiaryRequest;
import az.finalproject.model.request.CardRequest;
import az.finalproject.model.request.PaymentRequest;
import az.finalproject.model.response.Beneficiary;
import az.finalproject.model.response.Card;
import az.finalproject.model.response.ErrorResponse;
import az.finalproject.model.response.LoginResponse;
import az.finalproject.model.response.Payment;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static az.finalproject.core.ApiConfig.PASSWORD;
import static az.finalproject.core.ApiConfig.USER1_ACCOUNT;
import static az.finalproject.core.ApiConfig.USER1_EMAIL;
import static az.finalproject.core.ApiConfig.USER2_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bütün testlərin bazası.
 *
 * Token setup: backlog-a görə hər yeni login yeni, BOŞ sandbox açır.
 * Ona görə hər testdən (və parametrləşdirilmiş testin hər sətrindən) əvvəl yeni login edilir —
 * testlər bir-birinin datasını görmür, istənilən ardıcıllıqla və təkrar-təkrar eyni nəticəni verir.
 */
public abstract class BaseTest {

    /** user1 token-i (hər test üçün yeni sandbox) */
    protected String token;

    private static boolean reportMetadataWritten;

    /**
     * Allure hesabatı üçün Environment (mühit məlumatı) və Categories (düşmə növləri) faylları.
     * Bütün run üçün bir dəfə yazılır.
     */
    @BeforeAll
    static synchronized void writeAllureMetadata() throws IOException {
        if (reportMetadataWritten) return;
        Path dir = Paths.get(System.getProperty("allure.results.directory", "target/allure-results"));
        Files.createDirectories(dir);
        Files.write(dir.resolve("environment.properties"), String.join("\n",
                "API.URL=" + az.finalproject.core.ApiConfig.BASE_URL,
                "Test.user=" + USER1_EMAIL + " (IDOR: " + USER2_EMAIL + ")",
                "Java=" + System.getProperty("java.version"),
                "OS=" + System.getProperty("os.name"),
                "Stack=RestAssured 5.4 + JUnit 5 + Allure 2.27",
                "Sandbox=her test ucun yeni login (temiz muhit)").getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("categories.json"), ("[\n"
                + "  {\"name\": \"Server xətası (5xx) — mühit problemi\", \"matchedStatuses\": [\"failed\", \"broken\"],"
                + " \"messageRegex\": \"(?s).*(but was <5\\\\d\\\\d>|Network connection lost).*\"},\n"
                + "  {\"name\": \"Assertion uyğunsuzluğu — mümkün bug\", \"matchedStatuses\": [\"failed\"]},\n"
                + "  {\"name\": \"Test kodu / mühit xətası\", \"matchedStatuses\": [\"broken\"]}\n"
                + "]").getBytes(StandardCharsets.UTF_8));
        reportMetadataWritten = true;
    }

    @BeforeEach
    void openFreshSandbox() {
        token = login(USER1_EMAIL);
    }

    protected static String login(String email) {
        return AuthClient.login(email, PASSWORD)
                .then().statusCode(200)
                .extract().as(LoginResponse.class).token;
    }

    /** IDOR testləri üçün ikinci istifadəçi */
    protected String user2Token() {
        return login(USER2_EMAIL);
    }

    // ---------- Test datası yaradan köməkçi addımlar (setup) ----------

    protected Beneficiary createBeneficiary(String fullName, String iban, String currency, String nickname) {
        Allure.step("Setup: alıcı yaradılır — " + fullName);
        return BeneficiaryClient.create(token, new BeneficiaryRequest(fullName, iban, currency, nickname))
                .then().statusCode(201)
                .extract().as(Beneficiary.class);
    }

    protected Card createCard(String amount) {
        return createCard(token, USER1_ACCOUNT, amount);
    }

    protected Card createCard(String userToken, String accountId, String amount) {
        Allure.step("Setup: " + amount + " ilə kart buraxılır");
        return CardClient.create(userToken, new CardRequest(accountId, amount))
                .then().statusCode(201)
                .extract().as(Card.class);
    }

    /** Kartı state transition testi üçün lazım olan başlanğıc vəziyyətə gətirir */
    protected void moveCardTo(String cardId, CardState state) {
        switch (state) {
            case ACTIVE:
                return;
            case FROZEN:
                CardClient.block(token, cardId, "TEMPORARY").then().statusCode(200);
                return;
            case BLOCKED_LOST:
                CardClient.block(token, cardId, "LOST").then().statusCode(200);
                return;
            case BLOCKED_STOLEN:
                CardClient.block(token, cardId, "STOLEN").then().statusCode(200);
                return;
            default:
                throw new IllegalArgumentException(state.name());
        }
    }

    protected Payment pay(String cardId, String billerId, String subscriberNo, String amount) {
        Allure.step("Setup: " + billerId + " ödənişi " + amount);
        return PaymentClient.pay(token, new PaymentRequest(cardId, billerId, subscriberNo, amount))
                .then().statusCode(201)
                .extract().as(Payment.class);
    }

    // ---------- Ortaq yoxlamalar ----------

    /** Status-u yoxlayır və xəta cavabını POJO kimi qaytarır */
    protected ErrorResponse error(Response response, int expectedStatus) {
        return response.then().statusCode(expectedStatus).extract().as(ErrorResponse.class);
    }

    /** 400 VALIDATION_ERROR və details-də göstərilən sahə */
    protected void assertValidationError(Response response, String field) {
        ErrorResponse e = error(response, 400);
        assertThat(e.code).isEqualTo("VALIDATION_ERROR");
        assertThat(e.fields()).contains(field);
    }

    protected void assertErrorCode(Response response, int status, String code) {
        assertThat(error(response, status).code).isEqualTo(code);
    }
}
