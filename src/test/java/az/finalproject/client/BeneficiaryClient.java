package az.finalproject.client;

import az.finalproject.core.ApiSpec;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Collections;
import java.util.Map;

/** GRAD-101 / 102 / 103 — saxlanmış alıcılar */
public final class BeneficiaryClient {

    private BeneficiaryClient() {
    }

    /** body: POJO, Map və ya xam JSON mətni ola bilər (neqativ testlər üçün) */
    public static Response create(String token, Object body) {
        return ApiSpec.authorized(token).contentType(ContentType.JSON).body(body).post("/beneficiaries");
    }

    public static Response list(String token) {
        return list(token, Collections.emptyMap());
    }

    public static Response list(String token, Map<String, ?> query) {
        return ApiSpec.authorized(token).queryParams(query).get("/beneficiaries");
    }

    public static Response delete(String token, String id) {
        return ApiSpec.authorized(token).delete("/beneficiaries/{id}", id);
    }
}
