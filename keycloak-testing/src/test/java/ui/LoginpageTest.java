package ui;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.containsString;
import static io.restassured.RestAssured.given;

public class LoginpageTest {

    @Test
   void shouldrejectInvalidCredentials(){
       given()
               .contentType(ContentType.URLENC)
               .formParam("client_id","admin-cli")
               .formParam("username","wrongusername")
               .formParam("password","password")
               .formParam("grant_type","password")
               .when().post("/realms/master/protocol/openid-connect/token")
               .then().statusCode(401).body("error", equalTo("invalid_grant"));
   }

   @Test
    void failLoginForEmptyFields(){
        given()
                .contentType(ContentType.URLENC)
                .formParam("client_id","admin-cli")
                .formParam("username","nithin")
                .formParam("grant_type","password")
                .when().post("/realms/master/protocol/openid-connect/token")
                .then().statusCode(401)
                .body("error_description",containsString("Invalid user credentials"));
   }
   @Test
    void shouldRefreshAccessToken(){
        var response= given()
                .contentType(ContentType.URLENC)
                .formParam("client_id","admin-cli")
                .formParam("username","nithin")
                .formParam("password","password")
                .formParam("grant_type","password")
                .when().post("/realms/master/protocol/openid-connect/token")
                .then().statusCode(200).extract().response();
        String refreshToken= response.path("refresh_token");
        given()
                .contentType(ContentType.URLENC)
                .formParam("client_id","admin-cli")
                .formParam("refresh_token",refreshToken)
                .formParam("grant_type","refresh_token")
                .when().post("/realms/master/protocol/openid-connect/token")
                .then().statusCode(200).body("access_token",notNullValue());
   }
}
