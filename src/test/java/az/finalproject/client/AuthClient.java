package az.finalproject.client;

import az.finalproject.core.ApiSpec;
import az.finalproject.model.request.LoginRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public final class AuthClient {

    private AuthClient() {
    }

    public static Response login(String email, String password) {
        return ApiSpec.anonymous()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, password))
                .post("/auth/login");
    }
}
