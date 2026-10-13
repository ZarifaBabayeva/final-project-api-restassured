package az.finalproject.core;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Base request specification — bütün sorğuların ortaq hissəsi:
 * base URL, UTF-8 (Azərbaycan hərfləri üçün), Accept: JSON və Allure filter
 * (hər request/response Allure hesabatına avtomatik əlavə olunur).
 */
public final class ApiSpec {

    private ApiSpec() {
    }

    public static RequestSpecification base() {
        return new RequestSpecBuilder()
                .setBaseUri(ApiConfig.BASE_URL)
                .setAccept(ContentType.JSON)
                .setConfig(RestAssuredConfig.config()
                        .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8")))
                .addFilter(new AllureRestAssured())
                .build();
    }

    /** Token-siz sorğu (401 testləri üçün) */
    public static RequestSpecification anonymous() {
        return given().spec(base());
    }

    /** Bearer token ilə sorğu */
    public static RequestSpecification authorized(String token) {
        return given().spec(base()).header("Authorization", "Bearer " + token);
    }
}
