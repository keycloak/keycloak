package base;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public abstract class Basetest {
protected static String baseUrl = "http://localhost:8080";
@BeforeAll
    static void setup(){
    RestAssured.baseURI=baseUrl;
}

}
