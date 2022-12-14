package io.quarkiverse.featureflags.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FeatureFlagsResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/featureflags")
                .then()
                .statusCode(200)
                .body(is("Hello world"));
    }
}
