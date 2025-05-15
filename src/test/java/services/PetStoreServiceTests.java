package services;

import com.petstore.AnimalType;
import com.petstore.PetEntity;

import com.petstore.animals.CatEntity;
import com.petstore.animals.DogEntity;
import com.petstore.animals.attributes.Breed;
import com.petstore.animals.attributes.Gender;
import com.petstore.animals.attributes.PetType;
import com.petstore.animals.attributes.Skin;
import com.petstore.exceptions.DuplicatePetStoreRecordException;
import com.petstore.exceptions.PetNotFoundSaleException;

// New exception for unsupported pet types
import com.petstore.exceptions.PetTypeNotSupportedException;

import com.petstoreservices.exceptions.PetDataStoreException;
import com.petstoreservices.exceptions.PetInventoryFileNotCreatedException;
import com.petstoreservices.repository.PetRepository;
import com.petstoreservices.service.PetInventoryService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.Mockito;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.petstore.animals.attributes.Skin.FUR;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Pet Store Service Tests - removing the functionality of the PetRepository to see if the
 * services methods are functioning correctly
 *
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PetStoreServiceTests
{
    @InjectMocks //inject the pet repository into the pet service
    private PetInventoryService petService;

    @Mock //Mock the petRepository
    private PetRepository petRepository;

    private List<PetEntity> myPets;

    private PetEntity newDogItem;
    @Captor
    private ArgumentCaptor petTypeCaptor;

    @Captor
    private ArgumentCaptor petIdCaptor;

    @Captor
    private ArgumentCaptor petEntityCaptor;

    @Captor
    private ArgumentCaptor<List> petEntityListCaptor;
    @BeforeEach
    public void init() throws PetDataStoreException
    {
        //create the datastore to be used for testing
        myPets = new ArrayList<PetEntity>(Arrays.asList(
                new DogEntity(AnimalType.DOMESTIC, FUR, Gender.MALE, Breed.MALTESE,
                        new BigDecimal("750.00"), 3),
                new DogEntity(AnimalType.DOMESTIC, FUR, Gender.MALE, Breed.POODLE,
                        new BigDecimal("650.00"), 1),
                new DogEntity(AnimalType.DOMESTIC, FUR, Gender.FEMALE, Breed.GREY_HOUND,
                        new BigDecimal("750.00"), 4),
                new CatEntity(AnimalType.DOMESTIC, Skin.HAIR, Gender.MALE, Breed.BURMESE,
                        new BigDecimal("65.00"),1)
        ));
        petTypeCaptor  = ArgumentCaptor.forClass(PetType.class);;
        petIdCaptor = ArgumentCaptor.forClass(Integer.class);
        petEntityCaptor = ArgumentCaptor.forClass(PetEntity.class);
        petEntityListCaptor = ArgumentCaptor.forClass(List.class);
    }

    @TestFactory
    @Order(1)
    @DisplayName("Validate Dogs only return test")
    public Stream<DynamicTest> getInventoryTestByDog() throws PetNotFoundSaleException, PetDataStoreException
    {
        //mock the getInventory Repo and provide the data to be returned
        Mockito.lenient().doReturn(myPets).when(petRepository).getPetInventory();

        //execute the service - the list above should be returned filtered
        List<PetEntity> foundPetList = petService.getPetsByPetType(PetType.DOG);

        verify(petRepository, times(1)).getPetInventory(); //how many times inventory was called
        verify(petRepository).getPetInventory(); //verify the repository method was called

        //validate
        List<DynamicTest> inventoryTests = Arrays.asList(
                DynamicTest.dynamicTest("List size test",
                        ()-> assertEquals(3, foundPetList.size())),
                DynamicTest.dynamicTest("Pet item with Dog id 1",
                        ()-> assertTrue(foundPetList.stream()
                                .anyMatch(c -> c.getPetId()==1 && c.getPetType() == PetType.DOG
                                        && c.getGender() ==Gender.MALE && c.getBreed() == Breed.POODLE))),
                DynamicTest.dynamicTest("Pet item with Dog id 3",
                        ()-> assertTrue(foundPetList.stream()
                                .anyMatch(c -> c.getPetId()== 3&& c.getPetType() == PetType.DOG
                                        && c.getGender() ==Gender.MALE && c.getBreed() == Breed.MALTESE))),
                DynamicTest.dynamicTest("Pet item with Cat id 1 Not Found",
                        ()-> assertTrue(foundPetList.contains(myPets.get(3))==false)));

        return inventoryTests.stream();
    }

    @TestFactory
    @Order(3)
    @DisplayName("Add PetEntity DOG POST Test")
    public Stream<DynamicNode> postPetTest() throws
            PetInventoryFileNotCreatedException, PetDataStoreException {
        //Mock the getInventory with resuls
        Mockito.doReturn(myPets).when(petRepository).getPetInventory(); //mock the getInventory Repo
        //create pet entity to be added
        newDogItem = new DogEntity(AnimalType.DOMESTIC, FUR, Gender.FEMALE, Breed.GERMAN_SHEPHERD,
                new BigDecimal("225.00"), 5);
        //sort the pets so the pet entity has the correct filtered pet entity field
        List<PetEntity> sortedPets = myPets.stream()
                .filter(p -> p.getPetType().equals(PetType.DOG))
                .sorted(Comparator.comparingInt(PetEntity::getPetId))
                .collect(Collectors.toList());

        //mock the creation of the pet entity in the repository
        Mockito.doReturn(newDogItem).when(this.petRepository).createPetEntity(newDogItem,sortedPets);

        //execute the service
        PetEntity aEntity = this.petService.addInventory(PetType.DOG, newDogItem);

        verify(petRepository, times(1)).getPetInventory(); //how many times inventory was called
        verify(petRepository).createPetEntity(newDogItem,sortedPets); //verify createPetEntity class was called

        verify(petRepository).createPetEntity((PetEntity) petEntityCaptor.capture(), (List<PetEntity>)
                petEntityListCaptor.capture()); //capture the arguments for createPetEntity
        List<DynamicNode> testResultsList = new ArrayList<DynamicNode>();
        List<DynamicTest> argumentCreatePetAddItem = Arrays.asList(
                DynamicTest.dynamicTest("Pet item match",
                        ()-> assertEquals(newDogItem.toString(),
                                this.petEntityCaptor.getValue().toString())),
                DynamicTest.dynamicTest("Pet item with PetId[" + newDogItem.getPetId() + "]",
                        ()-> assertTrue(this.petEntityCaptor.getValue().toString().
                                contains(String.valueOf(newDogItem.getPetId())))),
                DynamicTest.dynamicTest("Pet item with PetType<Dog>",
                        ()-> assertTrue(this.petEntityCaptor.getValue().toString().
                                contains(newDogItem.getPetType().name))));
        DynamicContainer argumentTestContainer = DynamicContainer.dynamicContainer(
                "Pet Entity createPetEntity Arg[1] Captor Test[" +newDogItem.getPetId() +"]",
                argumentCreatePetAddItem);
        testResultsList.add(argumentTestContainer);
        //generate the sorted list
        List<DynamicTest> argumentCreateListPetItem = Arrays.asList(
                DynamicTest.dynamicTest("Pet item match size[" + sortedPets.size() +"]",
                        ()-> assertThat((List<PetEntity>)  this.petEntityListCaptor.getValue(),
                                hasSize(sortedPets.size()))),
                DynamicTest.dynamicTest("Pet item with PetId[" + sortedPets.get(0).getPetId() +
                                "] PetType[" + sortedPets.get(0).getPetType().name + "]",
                        ()-> assertThat((List<PetEntity>)  this.petEntityListCaptor.getValue(),
                                hasItem(sortedPets.get(0)))),
                DynamicTest.dynamicTest("Pet item with PetId[" + sortedPets.get(1).getPetId() +
                                "] PetType[" + sortedPets.get(1).getPetType().name + "]",
                        ()-> assertThat((List<PetEntity>)  this.petEntityListCaptor.getValue(),
                                hasItem(sortedPets.get(1)))),
                DynamicTest.dynamicTest("Pet item with PetId[" + sortedPets.get(2).getPetId() +
                                "] PetType[" + sortedPets.get(2).getPetType().name + "]",
                        ()-> assertThat((List<PetEntity>)  this.petEntityListCaptor.getValue(),
                                hasItem(sortedPets.get(2)))));
        DynamicContainer argumentTestContainerSortList = DynamicContainer.dynamicContainer(
                "Pet Entity createPetEntity Arg[2] PetEntity List Tests size[" + sortedPets.size() + "]",
                argumentCreateListPetItem);
        testResultsList.add(argumentTestContainerSortList);

        //generate the test results
        List<DynamicTest> inventoryTests = Arrays.asList(
                DynamicTest.dynamicTest("Pet item with Dog id 2",
                        ()-> assertEquals(5, aEntity.getPetId())),
                DynamicTest.dynamicTest("Dog breed",
                        ()-> assertTrue(AnimalType.DOMESTIC == aEntity.getAnimalType())),
                DynamicTest.dynamicTest("Dog Gender",
                        ()-> assertTrue(Gender.FEMALE == aEntity.getGender())));
        DynamicContainer petResponseContainer = DynamicContainer.dynamicContainer(
                "Pet Entity Service Tests[" +aEntity.getPetId() +"]", inventoryTests);
        testResultsList.add(petResponseContainer);


        return testResultsList.stream();
    }

    @TestFactory
    @Order(2)
    @DisplayName("Delete Pet Item<Dog> from inventory test")
    public Stream<DynamicNode> removePetItem() throws  PetInventoryFileNotCreatedException,
            DuplicatePetStoreRecordException, PetNotFoundSaleException,  PetDataStoreException
    {
        PetEntity removedPetItem = myPets.stream()
                .filter(p -> p.getPetType().equals(PetType.DOG) && p.getPetId() == 3)
                .findFirst()
                .orElse(null); //capture the item from the existing list in myPets

        Mockito.doReturn(removedPetItem).when(petRepository).findPetByPetTypeAndPetId(PetType.DOG, 3); //mock the getInventory Repo

        //mock the removeEntity() repo and define the results
        Mockito.doReturn(removedPetItem).when(this.petRepository).removeEntity(removedPetItem);

        //Execute the service and return the removeEntity id
        PetEntity removeEntity = petService.removeInventoryByIDAndPetType(PetType.DOG, removedPetItem.getPetId());

        List<DynamicNode> testResultsList = new ArrayList<DynamicNode>();

        verify(petRepository).removeEntity((PetEntity) this.petEntityCaptor.capture());
        List<DynamicTest> argCaptureRemovePetTests = Arrays.asList(
                DynamicTest.dynamicTest("Pet item match",
                        ()-> assertEquals(removedPetItem.toString(),
                                this.petEntityCaptor.getValue().toString())),
                DynamicTest.dynamicTest("Pet item with PetId<5>",
                        ()-> assertTrue(this.petEntityCaptor.getValue().toString().
                                contains(String.valueOf(removedPetItem.getPetId())))),
                DynamicTest.dynamicTest("Pet item with PetType<Dog>",
                        ()-> assertTrue(this.petEntityCaptor.getValue().toString().
                                contains(removedPetItem.getPetType().name))));

        DynamicContainer argumentRemoveContainer = DynamicContainer.dynamicContainer(
                "Pet Entity removePet Captor Tests[" +removedPetItem.getPetId() +"]",
                argCaptureRemovePetTests);

        List<DynamicTest> removedInventoryTests = Arrays.asList(
                DynamicTest.dynamicTest("Pet item with Dog id [3]",
                        ()-> assertEquals(3, removeEntity.getPetId())),
                DynamicTest.dynamicTest("Dog breed [" + AnimalType.DOMESTIC + "]",
                        ()-> assertSame(AnimalType.DOMESTIC,  removeEntity.getAnimalType())),
                DynamicTest.dynamicTest("Dog Gender[" + Gender.MALE + "]",
                        ()-> assertSame(Gender.MALE, removeEntity.getGender())));
        DynamicContainer petResponseContainer = DynamicContainer.dynamicContainer(
                "Pet Entity Service Tests[" +removedPetItem.getPetId() +"]", removedInventoryTests);

        verify(petRepository).findPetByPetTypeAndPetId((PetType) this.petTypeCaptor.capture(),
                (Integer) this.petIdCaptor.capture());
        //verify the arguments capture are what was provided
        List<DynamicTest> argCaptureFindPetTests = Arrays.asList(
                DynamicTest.dynamicTest("Pet item with Dog id 4",
                        ()-> assertTrue(removedPetItem.getPetId()==
                                Integer.valueOf(this.petIdCaptor.getValue().toString()))),
                DynamicTest.dynamicTest("Pet item with PetType<Dog>",
                        ()-> assertEquals("DOG", petTypeCaptor.getValue().toString())));

        DynamicContainer argumentTestContainer = DynamicContainer.dynamicContainer(
                "Pet Entity findPetByPetTypeAndPetId Captor Tests[" +removedPetItem.getPetId() +"]",
                argCaptureFindPetTests);

        testResultsList.add(argumentTestContainer);
        testResultsList.add(argumentRemoveContainer);
        testResultsList.add(petResponseContainer);
        return testResultsList.stream();
    }

    @TestFactory
    @Order(3)
    @DisplayName("Update Pet Item<Cat> in inventory test")
    public Stream<DynamicNode> updatePetItemTest() throws Exception
    {
        Mockito.doReturn(myPets).when(petRepository).getPetInventory(); //mock the getInventory Repo
        PetEntity updatePetItem = myPets.stream()
                .filter(p -> p.getPetType().equals(PetType.CAT) && p.getPetId() == 1)
                .findFirst()
                .orElse(null); //capture the item from the existing list in myPets

       /* CatEntity oldCat = new CatEntity(AnimalType.DOMESTIC, Skin.HAIR, Gender.MALE, Breed.BURMESE,
                new BigDecimal("65.00"), 1);*/
        CatEntity updatedCat = new CatEntity(AnimalType.DOMESTIC, Skin.HAIR, Gender.FEMALE, Breed.SPHYNX,
                new BigDecimal("65.00"), 1);

        Mockito.lenient().doReturn(updatePetItem).when(this.petRepository).findPetByPetTypeAndPetId(PetType.CAT, 1); //mock the getInventory Repo
        // Mockito.lenient().doReturn(updatePetItem).when(this.petRepository).removeEntity(updatedCat);
        Mockito.lenient().doReturn(updatedCat).when(this.petRepository).updatePetEntity(updatePetItem, updatedCat);

        PetEntity updateEntity = this.petService.updateInventoryByPetIdAndPetType(PetType.CAT, 1, updatedCat);

        List<DynamicNode> testResultsList = new ArrayList<DynamicNode>();
        verify(petRepository, times(1)).getPetInventory(); //how many times inventory was called
        //verify(petRepository).findPetByPetTypeAndPetId(PetType.CAT, 1);
        // verify(petRepository).removeEntity(updatedCat);
        verify(petRepository).updatePetEntity(updatePetItem, updatedCat);
        List<DynamicTest> argCaptureRemovePetTests = Arrays.asList(
                DynamicTest.dynamicTest("Pet item not match",
                        ()-> assertNotEquals(updatePetItem.toString(),
                                updateEntity.toString())),
                DynamicTest.dynamicTest("Pet item with PetId<1>",
                        ()-> assertTrue(updateEntity.toString().
                                contains(String.valueOf(updatePetItem.getPetId())))),
                DynamicTest.dynamicTest("Pet item with PetType<Cat>",
                        ()-> assertTrue(updateEntity.toString().
                                contains(updatePetItem.getPetType().name))));
        // Assertions using captured value
        testResultsList.addAll(argCaptureRemovePetTests);
        return testResultsList.stream();
    }
    @TestFactory
    @Order(3)
    @DisplayName("Validate Cats only return test")
    public Stream<DynamicTest> getInventoryTestByCat() throws PetNotFoundSaleException, PetDataStoreException
    {
        //Mocking the Inventory for the Pet Store
        Mockito.lenient().doReturn(myPets).when(petRepository).getPetInventory();

        //Filtering out the Cats in Inventory
        List<PetEntity> foundCatList = petService.getPetsByPetType(PetType.CAT);

        verify(petRepository, times(1)).getPetInventory(); //how many times inventory was called
        verify(petRepository).getPetInventory(); //verify the repository method was called

        //Providing Results for Tests
        List<DynamicTest> inventoryTests = Arrays.asList(
                DynamicTest.dynamicTest("List size test",
                        ()-> assertEquals(3, foundCatList.size())),
                DynamicTest.dynamicTest("Pet item with Cat id 1",
                        ()-> assertTrue(foundCatList.stream()
                                .anyMatch(c -> c.getPetId()==1 && c.getPetType() == PetType.CAT
                                        && c.getGender() ==Gender.MALE && c.getBreed() == Breed.BURMESE))),
                DynamicTest.dynamicTest("Pet item with Cat id 3",
                        ()-> assertTrue(foundCatList.stream()
                                .anyMatch(c -> c.getPetId()== 3&& c.getPetType() == PetType.CAT
                                        && c.getGender() ==Gender.MALE && c.getBreed() == Breed.RAGDOLL))));


        return inventoryTests.stream();
    }

    // Test for unsupported pet type update
    // Test for handling unsupported pet types (Bird, Snake, Fish, etc.) in updateInventory

    @TestFactory
    @Order(4)
    @DisplayName("Test unsupported pet type update")
    public Stream<DynamicTest> testUnsupportedPetTypeUpdate() throws Exception {
        // Create a bird entity (unsupported type)
        PetEntity birdEntity = new PetEntity();
        birdEntity.setPetType(PetType.BIRD);
        birdEntity.setPetId(1);
        
        // Mock the repository to return a bird entity
        Mockito.doReturn(birdEntity).when(petRepository).findPetByPetTypeAndPetId(PetType.BIRD, 1);
        
        // Mock the repository to throw PetTypeNotSupportedException
        Mockito.doThrow(new PetTypeNotSupportedException("Bird type not supported"))
            .when(petRepository).updatePetEntity(any(), any());
        
        // Execute and verify exception
        List<DynamicTest> tests = Arrays.asList(
            DynamicTest.dynamicTest("Should throw PetTypeNotSupportedException",
                () -> assertThrows(PetTypeNotSupportedException.class, () -> 
                    petService.updateInventoryByPetIdAndPetType(PetType.BIRD, 1, birdEntity))),
            DynamicTest.dynamicTest("Should call findPetByPetTypeAndPetId",
                () -> verify(petRepository, times(1)).findPetByPetTypeAndPetId(PetType.BIRD, 1))
        );
        
        return tests.stream();
    }

    // Test for duplicate pet records
    // Test for handling duplicate pet records when searching for a pet by type and id

    @TestFactory
    @Order(5)
    @DisplayName("Test duplicate pet records")
    public Stream<DynamicTest> testDuplicatePetRecords() throws Exception {
        // Create duplicate pets
        List<PetEntity> duplicatePets = Arrays.asList(
            new DogEntity(AnimalType.DOMESTIC, FUR, Gender.MALE, Breed.MALTESE, new BigDecimal("750.00"), 1),
            new DogEntity(AnimalType.DOMESTIC, FUR, Gender.MALE, Breed.POODLE, new BigDecimal("650.00"), 1)
        );
        
        // Mock repository to throw DuplicatePetStoreRecordException
        Mockito.doThrow(new DuplicatePetStoreRecordException("Duplicate records found"))
            .when(petRepository).findPetByPetTypeAndPetId(PetType.DOG, 1);
        
        // Execute and verify exception
        List<DynamicTest> tests = Arrays.asList(
            DynamicTest.dynamicTest("Should throw DuplicatePetStoreRecordException",
                () -> assertThrows(DuplicatePetStoreRecordException.class, () -> 
                    petService.getPetByIdAndType(PetType.DOG, 1))),
            DynamicTest.dynamicTest("Should call findPetByPetTypeAndPetId",
                () -> verify(petRepository, times(1)).findPetByPetTypeAndPetId(PetType.DOG, 1))
        );
        
        return tests.stream();
    }

    //What other tests could we add here?
}
