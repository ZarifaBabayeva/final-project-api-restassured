package az.finalproject.client;

import az.finalproject.core.ApiSpec;
import io.restassured.response.Response;

/** GRAD-301 — xidmət təchizatçıları kataloqu */
public final class BillerClient {

    private BillerClient() {
    }

    public static Response list(String token) {
        return ApiSpec.authorized(token).get("/billers");
    }

    public static Response listByCategory(String token, String category) {
        return ApiSpec.authorized(token).queryParam("category", category).get("/billers");
    }
}
