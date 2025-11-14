package api;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import base.Basetest;

public class TokenEndpointTest extends Basetest{
    @Test
    void shouldReturnAccessTokenForAdminUser(){
        given()
                .contentType(ContentType.URLENC)
                .formParam("client_id","admin-cli")
                .formParam("username","nithin")
                .formParam("password","password")
                .formParam("grant_type","password").when().post("/realms/master/protocol/openid-connect/token")
                .then().statusCode(200).body("access_token",notNullValue())
                .body("token_type",equalTo("Bearer"));

    }
}
