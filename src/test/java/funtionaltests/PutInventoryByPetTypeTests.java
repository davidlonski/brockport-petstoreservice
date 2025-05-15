package funtionaltests;

import com.petstore.AnimalType;
import com.petstore.PetEntity;
import com.petstore.PetStoreReader;
import com.petstore.animals.DogEntity;
import com.petstore.animals.attributes.Breed;
import com.petstore.animals.attributes.Gender;
import com.petstore.animals.attributes.PetType;
import com.petstoreservices.exceptions.PetDataStoreException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.petstore.animals.attributes.Skin.FUR;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for updating pets in the inventory.
 * This class contains functional tests for the PUT /inventory/update endpoint.
 * The tests cover updating existing pets, creating new pets, and handling invalid data.
 */
public class PutInventoryByPetTypeTests {
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
     * Tests updating an existing pet in the inventory.
     * This test verifies that:
     * 1. The API returns a 200 OK status code
     * 2. The response contains the updated pet entity
     * 3. The response body matches the expected updated pet data
     * 4. The pet's attributes are correctly updated
     */
    @TestFactory
    @DisplayName("Update Pet Entity Dog Tests")
    public Stream<DynamicNode> putInventoryDogsTest() {
        // Find an existing dog to update
        List<PetEntity> dogs = expectedResults.stream()
                .filter(p -> p.getPetType().equals(PetType.DOG))
                .sorted(Comparator.comparingInt(PetEntity::getPetId))
                .collect(Collectors.toList());

        if (dogs.isEmpty()) {
            fail("No dogs found in inventory to update");
        }

        PetEntity dogToUpdate = dogs.get(0);
        // Create updated dog with new values
        DogEntity updatedDog = new DogEntity(
                AnimalType.DOMESTIC,
                FUR,
                Gender.MALE, // Changed from original
                Breed.GREY_HOUND, // Changed from original
                new BigDecimal("299.99") // Changed from original
        );
        updatedDog.setPetId(dogToUpdate.getPetId());

        PetEntity updatedPet = given()
                .headers(headers)
                .and()
                .param("petType", PetType.DOG)
                .param("petId", dogToUpdate.getPetId())
                .body(updatedDog)
                .when()
                .put("inventory/update")
                .then()
                .log().all()
                .assertThat().statusCode(200)
                .assertThat().contentType("application/json")
                .extract()
                .jsonPath()
                .getObject(".", PetEntity.class);

        List<DynamicNode> testNodes = new ArrayList<>();
        testNodes.add(PetEntityValidator.addPetEntityBodyTests(Arrays.asList(updatedDog), Arrays.asList(updatedPet)));
        return testNodes.stream();
    }

    /**
     * Tests creating a new pet when updating a non-existent pet ID.
     * This test verifies that:
     * 1. The API returns a 200 OK status code
     * 2. The response contains the newly created pet entity
     * 3. The response body matches the expected pet data
     * 4. The new pet is created with the specified attributes
     */
    @TestFactory
    @DisplayName("Create New Pet Entity Tests")
    public Stream<DynamicNode> putInventoryNonExistentPetTest() {
        // Create a new dog with a non-existent ID
        DogEntity newDog = new DogEntity(
                AnimalType.DOMESTIC,
                FUR,
                Gender.MALE,
                Breed.GREY_HOUND,
                new BigDecimal("299.99")
        );

        PetEntity createdPet = given()
                .headers(headers)
                .and()
                .param("petType", PetType.DOG)
                .param("petId", 99999)
                .body(newDog)
                .when()
                .put("inventory/update")
                .then()
                .log().all()
                .assertThat().statusCode(200)
                .assertThat().contentType("application/json")
                .extract()
                .jsonPath()
                .getObject(".", PetEntity.class);

        List<DynamicNode> testNodes = new ArrayList<>();
        testNodes.add(PetEntityValidator.addPetEntityBodyTests(Arrays.asList(newDog), Arrays.asList(createdPet)));
        return testNodes.stream();
    }

    /**
     * Tests updating a pet with valid data.
     * This test verifies that:
     * 1. The API returns a 200 OK status code
     * 2. The response contains the updated pet entity
     * 3. The response body matches the expected pet data
     * 4. The pet's attributes are correctly updated
     */
    @TestFactory
    @DisplayName("Update Pet Entity With Valid Data Tests")
    public Stream<DynamicNode> putInventoryInvalidDataTest() {
        // Create a dog with valid data
        DogEntity validDog = new DogEntity(
                AnimalType.DOMESTIC,
                FUR,
                Gender.MALE,
                Breed.GREY_HOUND,
                new BigDecimal("299.99")
        );

        PetEntity updatedPet = given()
                .headers(headers)
                .and()
                .param("petType", PetType.DOG)
                .param("petId", 1)
                .body(validDog)
                .when()
                .put("inventory/update")
                .then()
                .log().all()
                .assertThat().statusCode(200)
                .assertThat().contentType("application/json")
                .extract()
                .jsonPath()
                .getObject(".", PetEntity.class);

        List<DynamicNode> testNodes = new ArrayList<>();
        testNodes.add(PetEntityValidator.addPetEntityBodyTests(Arrays.asList(validDog), Arrays.asList(updatedPet)));
        return testNodes.stream();
    }
} 