/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.it.cli.dist.util.CopyTLSKeystore;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

@DistributionTest(keepAlive = true, reInstall = DistributionTest.ReInstall.BEFORE_TEST)
@BeforeStartDistribution(CopyTLSKeystore.class)
@RawDistOnly(reason = "Containers are immutable")
public class TestKubeRealmTest {

    static String url="http://mykeycloak.127.0.0.1.nip.io:8080/";
    static String tokenUrl="realms/master/protocol/openid-connect/token";

    String realm="master";
    String realmPath="admin/realms/"+realm;
    String userPath=realmPath+"/users";
    String clientsPath=realmPath+"/clients";
    private static String accessToken;

    @BeforeAll
    @Launch({ "start-dev", "--hostname=mykeycloak.127.0.0.1.nip.io" })
    public static void validatetestTokenStatusCodeRceived(){
        RestAssured.useRelaxedHTTPSValidation();
        System.out.println("--------Access Token Started --------- validatetestTokenStatusCodeRceived");
        Response response=given().baseUri(url).basePath(tokenUrl)
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type","password")
                .param("client_id","admin-cli")
                .param("username","admin")
                .param("password","admin").when()
                .post();
        System.out.println("--------Response--------- validatetestTokenStatusCodeRceived");
        accessToken = response.jsonPath().getString("access_token");
        System.out.println("---------AccessToken:-------- "+accessToken);
        Assert.assertEquals(200, response.getStatusCode());
        System.out.println("--------Access Token Ended --------- validatetestTokenStatusCodeRceived");
    }
    @Test
    public void validateAdminUserExists()
    {
        System.out.println("--------Actual Test Started validateAdminUserExists ---------");
        Response response=given().relaxedHTTPSValidation().baseUri(url)
                .basePath(userPath).header("Authorization","Bearer "+accessToken)
                .get();
        System.out.println("Response: "+response.getBody().asString());
        Assert.assertTrue(response.getBody().asString().contains("\"username\":\"admin\""));
    }
    @Test
    public void validateAleastOneClientExists()
    {
        Response response = given().relaxedHTTPSValidation().baseUri(url)
                .basePath(clientsPath).header("Authorization","Bearer "+accessToken)
                .when()
                .get()
                .then()
                .log().ifValidationFails()
                .assertThat().
                statusCode(200).extract().response();
        JsonPath jsonPathEvaluator= response.jsonPath();
        List allClients =  jsonPathEvaluator.getList("$");
        Assert.assertNotEquals(0,allClients.size());
        System.out.println("Number of clients:" + allClients.size());
    }
    @Test
    public void validateRealmDisplayNameIsKeycloak()
    {
        Response response = given().relaxedHTTPSValidation().baseUri(url)
                .basePath(realmPath).header("Authorization","Bearer "+accessToken)
                .when()
                .get()
                .then()
                .log().ifValidationFails()
                .assertThat().
                statusCode(200).extract().response();
        Assert.assertEquals("Keycloak",response.jsonPath().getString("displayName"));
    }

}