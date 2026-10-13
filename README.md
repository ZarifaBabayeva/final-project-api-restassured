# Final Project — API Test Avtomatlaşdırması (RestAssured + JUnit 5 + Allure)

https://api.anarabbas.com — Final Project backlog (3 epic, 10 task) üçün avtomat regression testləri.

**Stack:** Java · RestAssured 5.4 · JUnit 5 (parameterized) · Jackson (POJO) · AssertJ · JSON Schema Validator · Allure 2

## İşə salma

```bash
# Bütün testlər
mvn clean test

# Allure hesabatını brauzerdə aç (Allure CLI-ni plugin özü yükləyir)
mvn allure:serve

# Statik HTML hesabat: target/site/allure-maven-plugin/index.html
mvn allure:report

# Tək test class-ı
mvn clean test -Dtest=BillPaymentTest
```

> Ödəniş və tarixçə testlərini **Bakı vaxtı 23:30–00:30 arasında işlətməyin** — kartın gündəlik xərci (`spentToday`) gecə yarısı sıfırlanır.

## Struktur

```
src/test/java/az/finalproject/
├── core/
│   ├── ApiConfig.java        # base URL, test istifadəçiləri, hesablar
│   ├── ApiSpec.java          # BASE REQUEST SPEC: base URL, UTF-8, Accept JSON, Allure filter
│   ├── TestData.java         # IBAN generatoru, gözlənilən expiry
│   └── CardState.java        # state transition üçün başlanğıc vəziyyətlər
├── client/                   # Hər resource üçün API client (testlər URL yazmır)
│   ├── AuthClient, BeneficiaryClient, CardClient, BillerClient, PaymentClient
├── model/
│   ├── request/              # POJO: LoginRequest, BeneficiaryRequest, CardRequest, BlockRequest, PaymentRequest
│   └── response/             # POJO: Beneficiary(List), Card, Biller(List), Payment, PaymentHistory, ErrorResponse ...
└── tests/
    ├── BaseTest.java                  # TOKEN SETUP + ortaq setup/assert metodları
    ├── AuthSecurityTest.java          # login, 401 qaydaları, sandbox izolyasiyası
    ├── BeneficiaryTest.java           # GRAD-101 / 102 / 103
    ├── CardTest.java                  # GRAD-201 / 202
    ├── CardStateTransitionTest.java   # GRAD-203 / 204 — keçid cədvəli
    ├── BillerTest.java                # GRAD-301
    ├── BillPaymentTest.java           # GRAD-302 — komissiya, idempotency, limitlər
    └── PaymentHistoryTest.java        # GRAD-303 — səhifələmə, filtr
src/test/resources/schema/             # 6 JSON Schema (contract testləri)
```

## Dizayn qərarları

- **Token setup.** Backlog-a görə hər login yeni, boş sandbox açır. `BaseTest`-də `@BeforeEach` hər testdən (parameterized testin hər sətrindən) əvvəl yeni login edir — testlər bir-birindən asılı deyil, istənilən ardıcıllıqla işləyir və deterministikdir.
- **POJO.** Request və response-lar class-lardır; **pul sahələri `BigDecimal`** — `0.1 + 0.2` kimi float xətaları olmasın və `50.50` olduğu kimi göndərilsin. Response POJO-ları `@JsonIgnoreProperties(ignoreUnknown = true)` ilə yeni sahələrə davamlıdır.
- **Data-driven BVA.** Sərhəd dəyərləri `@ParameterizedTest` + `@CsvSource` ilə: fullName 1/2/50/51, initialAmount 0.99/1.00/1000.00/1000.01, Azercell 0.49/0.50/200.00/200.01, tək əməliyyat limiti 300.00/300.01, Idempotency-Key 7/8/64/65, limit 0/1/50/51 və s.
- **State transition.** GRAD-203/204-ün bütün keçid cədvəli bir parametrləşdirilmiş testdir — hər sətir Allure-da ayrıca test kimi görünür.
- **JSON Schema.** 6 endpoint üçün contract yoxlaması (`matchesJsonSchemaInClasspath`).
- **Assert `code`-a yazılır**, xəta mətninə yox (backlog qaydası).
- **Allure.** `@Epic` (3 epic) → `@Feature` (task) → `@Story`; `@Severity`; `@DisplayName`-də TC ID-ləri (`docs/Birmarket_Test_Documentation.xlsx` ilə traceability). `AllureRestAssured` filter hər request/response-u hesabata əlavə edir.

Test case-lər, coverage və traceability: əsas repodakı `docs/Birmarket_Test_Documentation.xlsx` (API_* sheet-ləri).
