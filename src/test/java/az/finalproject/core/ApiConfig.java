package az.finalproject.core;

/**
 * Mühit konfiqurasiyası. Base URL komanda sətrindən dəyişdirilə bilər:
 *   mvn test -DbaseUrl=https://api.anarabbas.com
 */
public final class ApiConfig {

    public static final String BASE_URL = System.getProperty("baseUrl", "https://api.anarabbas.com");

    public static final String USER1_EMAIL = "user1@test.com";
    public static final String USER2_EMAIL = "user2@test.com";
    public static final String PASSWORD = "User1234";

    /** user1-in AZN hesabı (balans 12500) */
    public static final String USER1_ACCOUNT = "acc_002";
    /** user2-nin USD hesabı (balans 890.50) */
    public static final String USER2_ACCOUNT = "acc_003";
    /** user1-in öz hesabının IBAN-ı (OWN_ACCOUNT testi üçün) */
    public static final String USER1_OWN_IBAN = "AZ29NABZ00000000137010002855";

    private ApiConfig() {
    }
}
