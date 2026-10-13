package az.finalproject.tests;

import az.finalproject.client.BeneficiaryClient;
import az.finalproject.core.TestData;
import az.finalproject.model.request.BeneficiaryRequest;
import az.finalproject.model.response.Beneficiary;
import az.finalproject.model.response.BeneficiaryList;
import az.finalproject.model.response.ErrorResponse;
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

import static az.finalproject.core.ApiConfig.USER1_OWN_IBAN;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("EPIC-1: Saxlanmış alıcılar")
@DisplayName("EPIC-1: Saxlanmış alıcılar")
class BeneficiaryTest extends BaseTest {

    private static final String IBAN = "AZ77AIIB40060019440123456789";

    // ===================================================================== GRAD-101
    @Nested
    @Feature("GRAD-101 POST /beneficiaries")
    @DisplayName("GRAD-101 POST /beneficiaries")
    class Create {

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("TC_API_BEN_001 — Bütün sahələrlə alıcı yaradılır (201, Location)")
        void createsBeneficiary() {
            Response r = BeneficiaryClient.create(token, new BeneficiaryRequest("Nigar Əliyeva", IBAN, "AZN", "Anam"));
            r.then().statusCode(201);
            Beneficiary b = r.as(Beneficiary.class);

            assertThat(b.id).matches("^ben_[a-z0-9]{8}$");
            assertThat(b.fullName).isEqualTo("Nigar Əliyeva");
            assertThat(b.iban).isEqualTo(IBAN);
            assertThat(b.currency).isEqualTo("AZN");
            assertThat(b.nickname).isEqualTo("Anam");
            assertThat(b.createdAt).isNotBlank();
            assertThat(r.header("Location")).endsWith("/beneficiaries/" + b.id);
        }

        @Test
        @Story("Contract")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_002 — Alıcı cavabı JSON Schema-ya uyğundur")
        void responseMatchesSchema() {
            BeneficiaryClient.create(token, new BeneficiaryRequest("Rauf Məmmədov", TestData.iban(1), "USD"))
                    .then().statusCode(201)
                    .body(matchesJsonSchemaInClasspath("schema/fp-beneficiary-schema.json"));
        }

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_003/004 — IBAN normallaşdırılır, nickname yoxdursa null")
        void ibanIsNormalizedAndNicknameNull() {
            Beneficiary b = BeneficiaryClient.create(token,
                            new BeneficiaryRequest("Nigar Əliyeva", "az77 aiib 4006 0019 4401 2345 6789", "AZN"))
                    .then().statusCode(201).extract().as(Beneficiary.class);

            assertThat(b.iban).isEqualTo(IBAN);
            assertThat(b.ibanMasked).isEqualTo("AZ77 **** **** **** **** 6789");
            assertThat(b.nickname).isNull();
        }

        @Test
        @Story("Validasiya")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEN_005 — Bir neçə səhv sahə details-də ayrıca göstərilir")
        void allInvalidFieldsReported() {
            ErrorResponse e = error(BeneficiaryClient.create(token, new BeneficiaryRequest("A", "123", "GBP")), 400);
            assertThat(e.code).isEqualTo("VALIDATION_ERROR");
            assertThat(e.fields()).containsExactlyInAnyOrder("fullName", "iban", "currency");
        }

        @ParameterizedTest(name = "fullName uzunluğu {0} → {1}")
        @Story("BVA")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEN_006–009 — fullName uzunluğu BVA (2–50)")
        @CsvSource({"1, 400", "2, 201", "50, 201", "51, 400"})
        void fullNameLengthBoundaries(int length, int expectedStatus) {
            Response r = BeneficiaryClient.create(token,
                    new BeneficiaryRequest(TestData.repeat('A', length), TestData.iban(length), "AZN"));
            if (expectedStatus == 201) r.then().statusCode(201);
            else assertValidationError(r, "fullName");
        }

        @ParameterizedTest(name = "fullName \"{0}\" → 400")
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_011/012 — fullName yalnız hərf, boşluq, defis, apostrof ola bilər")
        @ValueSource(strings = {"---", "Ali2"})
        void invalidFullNameRejected(String fullName) {
            assertValidationError(BeneficiaryClient.create(token,
                    new BeneficiaryRequest(fullName, TestData.iban(77), "AZN")), "fullName");
        }

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_013 — Azərbaycan hərfləri, defis və apostrof qəbul edilir")
        void azerbaijaniLettersAccepted() {
            Beneficiary b = BeneficiaryClient.create(token,
                            new BeneficiaryRequest("Şəhla Güləliyeva-O'Neil", TestData.iban(13), "AZN"))
                    .then().statusCode(201).extract().as(Beneficiary.class);
            assertThat(b.fullName).isEqualTo("Şəhla Güləliyeva-O'Neil");
        }

        @ParameterizedTest(name = "IBAN {0} → 400")
        @Story("BVA")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEN_014–016 — IBAN uzunluğu (27/29) və ölkə prefiksi")
        @ValueSource(strings = {"AZ77AIIB4006001944012345678", "AZ77AIIB400600194401234567890", "TR77AIIB40060019440123456789"})
        void invalidIbanRejected(String iban) {
            assertValidationError(BeneficiaryClient.create(token, new BeneficiaryRequest("Test Bir", iban, "AZN")), "iban");
        }

        @Test
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_018 — Siyahıda olmayan valyuta (GBP) rədd edilir")
        void unsupportedCurrencyRejected() {
            assertValidationError(BeneficiaryClient.create(token,
                    new BeneficiaryRequest("Test Bir", TestData.iban(18), "GBP")), "currency");
        }

        @Test
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_021 — Məcburi iban sahəsi olmadan rədd edilir")
        void missingIbanRejected() {
            assertValidationError(BeneficiaryClient.create(token, Map.of("fullName", "Test Bir", "currency", "AZN")), "iban");
        }

        @Test
        @Story("Validasiya")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_023 — Etibarsız JSON body INVALID_JSON qaytarır")
        void invalidJsonRejected() {
            assertErrorCode(BeneficiaryClient.create(token, "{\"fullName\": \"Test\","), 400, "INVALID_JSON");
        }

        @ParameterizedTest(name = "dublikat IBAN \"{0}\" → 409")
        @Story("Biznes qaydası")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEN_024/025 — Eyni IBAN (fərqli formatda da) təkrar əlavə edilə bilmir")
        @ValueSource(strings = {IBAN, "az77 aiib 4006 0019 4401 2345 6789"})
        void duplicateIbanRejected(String duplicateIban) {
            createBeneficiary("Nigar Əliyeva", IBAN, "AZN", null);
            assertErrorCode(BeneficiaryClient.create(token, new BeneficiaryRequest("Başqa Ad", duplicateIban, "AZN")),
                    409, "BENEFICIARY_EXISTS");
        }

        @Test
        @Story("Biznes qaydası")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEN_026 — Ləqəb böyük-kiçik hərf fərqi ilə təkrarlana bilmir")
        void nicknameIsCaseInsensitiveUnique() {
            createBeneficiary("Nigar Əliyeva", IBAN, "AZN", "Anam");
            assertErrorCode(BeneficiaryClient.create(token,
                    new BeneficiaryRequest("Başqa Ad", TestData.iban(26), "AZN", "ANAM")), 409, "NICKNAME_TAKEN");
        }

        @Test
        @Story("Biznes qaydası")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEN_027 — İstifadəçi öz hesabının IBAN-ını əlavə edə bilmir")
        void ownAccountRejected() {
            assertErrorCode(BeneficiaryClient.create(token, new BeneficiaryRequest("Özüm Test", USER1_OWN_IBAN, "AZN")),
                    422, "OWN_ACCOUNT");
        }

        @Test
        @Story("BVA")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEN_029/030 — 10-cu alıcı əlavə olunur, 11-ci LIMIT_REACHED")
        void beneficiaryLimitIsTen() {
            for (int i = 1; i <= 9; i++) createBeneficiary("Alıcı Test " + (char) ('A' + i - 1), TestData.iban(100 + i), "AZN", null);

            BeneficiaryClient.create(token, new BeneficiaryRequest("Onuncu Alıcı", TestData.iban(210), "AZN"))
                    .then().statusCode(201);

            ErrorResponse e = error(BeneficiaryClient.create(token,
                    new BeneficiaryRequest("On Birinci", TestData.iban(211), "AZN")), 422);
            assertThat(e.code).isEqualTo("LIMIT_REACHED");
            assertThat(e.limit).isEqualTo(10);
        }
    }

    // ===================================================================== GRAD-102
    @Nested
    @Feature("GRAD-102 GET /beneficiaries")
    @DisplayName("GRAD-102 GET /beneficiaries")
    class ListBeneficiaries {

        /** 5 alıcı — ardıcıllıqla: Nigar, Cavid, Çingiz, Əli, Elvin (sonuncu ən yenidir) */
        private void createFive() {
            createBeneficiary("Nigar Əliyeva", TestData.iban(101), "AZN", "Anam");
            createBeneficiary("Cavid Həsənov", TestData.iban(102), "USD", null);
            createBeneficiary("Çingiz Abbasov", TestData.iban(103), "AZN", null);
            createBeneficiary("Əli Rzayev", TestData.iban(104), "USD", null);
            createBeneficiary("Elvin Quliyev", TestData.iban(105), "EUR", null);
        }

        @Test
        @Story("Default sıralama")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BENL_001/002 — Parametrsiz: total = items, ən yeni birinci, schema")
        void defaultListNewestFirst() {
            createFive();
            Response r = BeneficiaryClient.list(token);
            r.then().statusCode(200).body(matchesJsonSchemaInClasspath("schema/fp-beneficiary-list-schema.json"));
            BeneficiaryList list = r.as(BeneficiaryList.class);

            assertThat(list.total).isEqualTo(5);
            assertThat(list.items).hasSize(5);
            assertThat(list.items.get(0).fullName).isEqualTo("Elvin Quliyev");
        }

        @Test
        @Story("Sıralama")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BENL_003 — sort=fullName asc Azərbaycan əlifbası ilə (C < Ç < E < Ə < N)")
        void sortByNameUsesAzerbaijaniAlphabet() {
            createFive();
            BeneficiaryList list = BeneficiaryClient.list(token, Map.of("sort", "fullName", "order", "asc"))
                    .then().statusCode(200).extract().as(BeneficiaryList.class);

            assertThat(list.fullNames()).containsExactly(
                    "Cavid Həsənov", "Çingiz Abbasov", "Elvin Quliyev", "Əli Rzayev", "Nigar Əliyeva");
        }

        @Test
        @Story("Filtr")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BENL_006 — currency=USD yalnız USD alıcıları qaytarır")
        void filterByCurrency() {
            createFive();
            BeneficiaryList list = BeneficiaryClient.list(token, Map.of("currency", "USD"))
                    .then().statusCode(200).extract().as(BeneficiaryList.class);

            assertThat(list.total).isEqualTo(2);
            assertThat(list.items).allSatisfy(b -> assertThat(b.currency).isEqualTo("USD"));
        }

        @ParameterizedTest(name = "q={0} → Nigar Əliyeva")
        @Story("Axtarış")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BENL_008/009 — Axtarış nöqtəli (İ) və nöqtəsiz (I) hərflə işləyir")
        @ValueSource(strings = {"NİGAR", "NIGAR"})
        void searchIsCaseAndLetterInsensitive(String q) {
            createFive();
            BeneficiaryList list = BeneficiaryClient.list(token, Map.of("q", q))
                    .then().statusCode(200).extract().as(BeneficiaryList.class);

            assertThat(list.total).isEqualTo(1);
            assertThat(list.items.get(0).fullName).isEqualTo("Nigar Əliyeva");
        }

        @Test
        @Story("Axtarış")
        @Severity(SeverityLevel.MINOR)
        @DisplayName("TC_API_BENL_013 — q=1 simvol rədd edilir")
        void searchMinLength() {
            assertValidationError(BeneficiaryClient.list(token, Map.of("q", "a")), "q");
        }
    }

    // ===================================================================== GRAD-103
    @Nested
    @Feature("GRAD-103 DELETE /beneficiaries/{id}")
    @DisplayName("GRAD-103 DELETE /beneficiaries/{id}")
    class Delete {

        @Test
        @Story("Pozitiv")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("TC_API_BEND_001/002 — Silinir (204, body boş), siyahıda görünmür")
        void deleteRemovesBeneficiary() {
            Beneficiary a = createBeneficiary("Nigar Əliyeva", IBAN, "AZN", null);
            Beneficiary b = createBeneficiary("Cavid Həsənov", TestData.iban(102), "USD", null);

            Response r = BeneficiaryClient.delete(token, a.id);
            r.then().statusCode(204);
            assertThat(r.asString()).isEmpty();

            BeneficiaryList list = BeneficiaryClient.list(token).then().statusCode(200).extract().as(BeneficiaryList.class);
            assertThat(list.total).isEqualTo(1);
            assertThat(list.ids()).containsExactly(b.id);
        }

        @Test
        @Story("Neqativ")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("TC_API_BEND_003 — Eyni alıcı ikinci dəfə silindikdə 404")
        void deleteTwiceReturns404() {
            Beneficiary a = createBeneficiary("Nigar Əliyeva", IBAN, "AZN", null);
            BeneficiaryClient.delete(token, a.id).then().statusCode(204);
            assertErrorCode(BeneficiaryClient.delete(token, a.id), 404, "NOT_FOUND");
        }

        @Test
        @Story("Təhlükəsizlik (IDOR)")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("TC_API_BEND_004 — Başqa istifadəçinin alıcısı silinə bilmir (404)")
        void otherUserCannotDelete() {
            Beneficiary a = createBeneficiary("Nigar Əliyeva", IBAN, "AZN", null);

            assertErrorCode(BeneficiaryClient.delete(user2Token(), a.id), 404, "NOT_FOUND");

            BeneficiaryList list = BeneficiaryClient.list(token).then().statusCode(200).extract().as(BeneficiaryList.class);
            assertThat(list.ids()).containsExactly(a.id);
        }
    }
}
