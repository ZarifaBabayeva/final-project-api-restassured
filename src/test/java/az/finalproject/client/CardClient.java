package az.finalproject.client;

import az.finalproject.core.ApiSpec;
import az.finalproject.model.request.BlockRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/** GRAD-201 / 202 / 203 / 204 — virtual kartlar */
public final class CardClient {

    private CardClient() {
    }

    public static Response create(String token, Object body) {
        return ApiSpec.authorized(token).contentType(ContentType.JSON).body(body).post("/cards");
    }

    public static Response get(String token, String cardId) {
        return ApiSpec.authorized(token).get("/cards/{id}", cardId);
    }

    public static Response block(String token, String cardId, String reason) {
        return block(token, cardId, (Object) new BlockRequest(reason));
    }

    public static Response block(String token, String cardId, Object body) {
        return ApiSpec.authorized(token).contentType(ContentType.JSON).body(body)
                .post("/cards/{id}/block", cardId);
    }

    public static Response unblock(String token, String cardId) {
        return ApiSpec.authorized(token).post("/cards/{id}/unblock", cardId);
    }
}
