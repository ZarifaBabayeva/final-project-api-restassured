package az.finalproject.client;

import az.finalproject.core.ApiSpec;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.UUID;

/** GRAD-302 / 303 — kommunal ödənişlər və tarixçə */
public final class PaymentClient {

    private PaymentClient() {
    }

    /** Hər çağırışda yeni, unikal Idempotency-Key ilə ödəniş */
    public static Response pay(String token, Object body) {
        return pay(token, body, UUID.randomUUID().toString());
    }

    /** Verilmiş açarla ödəniş; key = null olarsa header göndərilmir */
    public static Response pay(String token, Object body, String idempotencyKey) {
        RequestSpecification request = ApiSpec.authorized(token).contentType(ContentType.JSON).body(body);
        if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
        return request.post("/bill-payments");
    }

    public static Response history(String token, Map<String, ?> query) {
        return ApiSpec.authorized(token).queryParams(query).get("/bill-payments");
    }
}
