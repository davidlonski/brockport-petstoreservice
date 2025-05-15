package funtionaltests;

import com.petstore.PetEntity;
import com.petstore.PetStoreReader;
import com.petstore.animals.attributes.PetType;
import com.petstoreservices.exceptions.PetDataStoreException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for retrieving pets by their ID from the inventory.
 * This class contains functional tests for the GET /inventory/search endpoint.
 */
public class GetInventoryByPetIdTests {
    private static Headers headers;
    private List<PetEntity> expectedResults;

    @BeforeEach
    public void retrieveDataStore() throws PetDataStoreException {
        RestAssured.baseURI = "http://localhost:8080/";
        PetStoreReader psReader = new PetStoreReader();
        expectedResults = psReader.readJsonFromFile();

        Header contentType = new Header("Content-Type", ContentType.JSON.toString());
        Header accept = new Header("Accept", ContentType.JSON.toString());
        headers = new Headers(contentType, accept);
    }

    /**
     * Tests retrieving a pet by its ID.
     * This test verifies that:
     * 1. The API returns a 200 OK status code
     * 2. The response contains the correct pet entity
     * 3. The response body matches the expected pet data
     */
    @TestFactory
    @DisplayName("Get Pet Entity By ID Tests")
    public Stream<DynamicNode> getInventoryByPetIdTest() {
        // Find an existing pet to retrieve
        List<PetEntity> pets = expectedResults.stream()
                .sorted(Comparator.comparingInt(PetEntity::getPetId))
                .collect(Collectors.toList());

        PetEntity petToRetrieve = pets.get(0);

        PetEntity retrievedPet = given()
                .headers(headers)
                .and()
                .param("petType", petToRetrieve.getPetType())
                .param("petId", petToRetrieve.getPetId())
                .when()
                .get("inventory/search")
                .then()
                .log().all()
                .assertThat().statusCode(200)
                .assertThat().contentType("application/json")
                .extract()
                .jsonPath()
                .getObject(".", PetEntity.class);

        List<DynamicNode> testNodes = new ArrayList<>();
        testNodes.add(PetEntityValidator.addPetEntityBodyTests(Arrays.asList(petToRetrieve), Arrays.asList(retrievedPet)));
        return testNodes.stream();
    }

    /**
     * Tests retrieving a non-existent pet by ID.
     * This test verifies that:
     * 1. The API returns a 400 Bad Request status code
     * 2. The response contains an appropriate error message
     * 3. The error message indicates that no pet was found with the given ID
     */
    @Test
    @DisplayName("Get Non-Existent Pet Entity Tests")
    public void getInventoryByNonExistentPetIdTest() {
        String response = given()
                .headers(headers)
                .and()
                .param("petType", PetType.DOG)
                .param("petId", 99999)
                .when()
                .get("inventory/search")
                .then()
                .log().all()
                .assertThat().statusCode(400)
                .assertThat().contentType("application/json")
                .extract()
                .asString();

        Assertions.assertEquals("0 results found for search criteria for pet id[99999] petType[DOG] Please try again!!", response);
    }

    /**
     * Tests retrieving a pet with an invalid ID format.
     * This test verifies that:
     * 1. The API returns a 400 Bad Request status code
     * 2. The response contains an appropriate error message
     * 3. The error message indicates that the ID format is invalid
     */
    @TestFactory
    @DisplayName("Get Pet Entity With Invalid ID Tests")
    public Stream<DynamicTest> getInventoryByInvalidPetIdTest() {
        RestAssured.registerParser("application/json", Parser.JSON);
        BadRequestResponseBody body = given()
                .headers(headers)
                .and()
                .param("petType", PetType.DOG)
                .param("petId", "invalid")
                .when()
                .get("inventory/search")
                .then()
                .log().all()
                .assertThat().statusCode(400)
                .assertThat().contentType("application/json")
                .extract()
                .jsonPath()
                .getObject(".", BadRequestResponseBody.class);

        return body.executeTests("Bad Request", "Failed to convert value of type 'java.lang.String' to required type 'int'; For input string: \"invalid\"",
                "/inventory/search", 400).stream();
    }

    /**
     * Tests retrieving a pet by ID with a specific pet type filter.
     * This test verifies that:
     * 1. The API returns a 200 OK status code
     * 2. The response contains the correct pet entity
     * 3. The response body matches the expected pet data
     * 4. The returned pet matches both the ID and type criteria
     */
    @TestFactory
    @DisplayName("Get Pet Entity By ID With Type Filter Tests")
    public Stream<DynamicNode> getInventoryByPetIdWithTypeTest() {
        // Find an existing dog to retrieve
        List<PetEntity> dogs = expectedResults.stream()
                .filter(p -> p.getPetType().equals(PetType.DOG))
                .sorted(Comparator.comparingInt(PetEntity::getPetId))
                .collect(Collectors.toList());

        PetEntity dogToRetrieve = dogs.get(0);

        PetEntity retrievedDog = given()
                .headers(headers)
                .and()
                .param("petType", PetType.DOG)
                .param("petId", dogToRetrieve.getPetId())
                .when()
                .get("inventory/search")
                .then()
                .log().all()
                .assertThat().statusCode(200)
                .assertThat().contentType("application/json")
                .extract()
                .jsonPath()
                .getObject(".", PetEntity.class);

        List<DynamicNode> testNodes = new ArrayList<>();
        testNodes.add(PetEntityValidator.addPetEntityBodyTests(Arrays.asList(dogToRetrieve), Arrays.asList(retrievedDog)));
        return testNodes.stream();
    }
} 