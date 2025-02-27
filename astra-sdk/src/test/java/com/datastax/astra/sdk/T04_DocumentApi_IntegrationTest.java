/*
 * Copyright DataStax, Inc.
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

package com.datastax.astra.sdk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.datastax.astra.dto.PersonAstra;
import com.datastax.astra.dto.PersonAstra.Address;
import com.datastax.stargate.sdk.StargateClient;
import com.datastax.stargate.sdk.core.DataCenter;
import com.datastax.stargate.sdk.doc.ApiDocument;
import com.datastax.stargate.sdk.doc.ApiDocumentClient;
import com.datastax.stargate.sdk.doc.CollectionClient;
import com.datastax.stargate.sdk.doc.DocumentClient;
import com.datastax.stargate.sdk.doc.NamespaceClient;
import com.datastax.stargate.sdk.doc.domain.DocumentResultPage;
import com.datastax.stargate.sdk.doc.domain.SearchDocumentQuery;

/**
 * Test operations for the Document API operation
 *
 * @author Cedrick LUNVEN (@clunven)
 */
@TestMethodOrder(OrderAnnotation.class)
public class T04_DocumentApi_IntegrationTest extends AbstractAstraIntegrationTest {
    
    private static final String WORKING_NAMESPACE    = "astra_sdk_namespace_test";
    private static final String COLLECTION_PERSON    = "person";
    
    private static ApiDocumentClient clientApiDoc;
    
    @BeforeAll
    public static void config() {
        System.out.println(ANSI_YELLOW + "[T04_DocumentApi_IntegrationTest]" + ANSI_RESET);
        //initDb("sdk_test_docApi");
        
        /*
        client = AstraClient.builder()
                .databaseId("...")
                .cloudProviderRegion("...")
                .appToken("AstraCS:...")
                .build();
        */
        
        clientApiDoc = client.apiStargateDocument();
        // Not available in the doc API anymore as of now
        if (!client.apiStargateDocument().namespace(WORKING_NAMESPACE).exist()) {
            client.apiDevopsDatabases().database(dbId.get())
                  .createNamespace(WORKING_NAMESPACE);
            System.out.print(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creating namespace ");
            while(!client.apiStargateDocument().namespace(WORKING_NAMESPACE).exist()) {
                System.out.print(ANSI_GREEN + "\u25a0" +ANSI_RESET); 
                waitForSeconds(1);
            }
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Namespace created");
        } else {
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Namespace already exist.");
        }
       
    }
   
    @Test
    @Order(1)
    @DisplayName("Parameter validations should through IllegalArgumentException(s)")
    public void builderParams_should_not_be_empty() {
        System.out.println(ANSI_YELLOW + "\n#01Checking required parameters " + ANSI_RESET);
        Assertions.assertAll("Required parameters",
                () -> Assertions.assertThrows(IllegalArgumentException.class, 
                        () -> { AstraClient.builder().databaseId(null); }),
                () -> Assertions.assertThrows(IllegalArgumentException.class, 
                        () -> { AstraClient.builder().databaseId(""); }),
                () -> Assertions.assertThrows(IllegalArgumentException.class, 
                        () -> { AstraClient.builder().cloudProviderRegion(""); }),
                () -> Assertions.assertThrows(IllegalArgumentException.class, 
                        () -> { AstraClient.builder().cloudProviderRegion(null); }),
                () -> Assertions.assertThrows(IllegalArgumentException.class, 
                        () -> { AstraClient.builder().appToken(""); }),
                () -> Assertions.assertThrows(IllegalArgumentException.class, 
                        () -> { AstraClient.builder().appToken(null); })
        );
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Validation OK");
    }
    
    /*
    @Test
    @Order(2)
    @DisplayName("Create and delete namespace with replicas")
    public void should_create_tmp_namespace()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#02 Working with Namespaces" + ANSI_RESET);
        if (clientApiDoc.namespace("tmp_namespace").exist()) {
            clientApiDoc.namespace("tmp_namespace").delete();
            int wait = 0;
            while (wait++ < 5 && clientApiDoc.namespace("tmp_namespace").exist()) {
                Thread.sleep(1000);
                System.out.println("+ ");
            }
        }
        clientApiDoc.namespace("tmp_namespace").createSimple(1);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
        int wait = 0;
        while (wait++ < 5 && !clientApiDoc.namespace("tmp_namespace").exist()) {
            Thread.sleep(1000);
        }
        clientApiDoc.namespace("tmp_namespace").delete();
        wait = 0;
        while (wait++ < 5 && clientApiDoc.namespace("tmp_namespace").exist()) {
            Thread.sleep(1000);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Namespace craeted");
    }
    
    @Test
    @Order(3)
    @DisplayName("Create and delete namespace with datacenter")
    public void should_create_tmp_namespace2() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#03 Working with Namespaces 2" + ANSI_RESET);
        // TMP KEYSPACE
        if (clientApiDoc.namespace("tmp_namespace2").exist()) {
            clientApiDoc.namespace("tmp_namespace2").delete();
            int wait = 0;
            while (wait++ < 5 && clientApiDoc.namespace("tmp_namespace2").exist()) {
                Thread.sleep(1000);
            }
        }
        clientApiDoc.namespace("tmp_namespace2").create(new DataCenter(cloudRegion.get(), 1));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
        int wait = 0;
        while (wait++ < 5 && !clientApiDoc.namespace("tmp_namespace2").exist()) {
            Thread.sleep(1000);
            System.out.println("+ ");
        }

        clientApiDoc.namespace("tmp_namespace2").delete();
        wait = 0;
        while (wait++ < 5 && clientApiDoc.namespace("tmp_namespace2").exist()) {
            Thread.sleep(1000);
            System.out.println("+ ");
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Namespaces created");
    }
   
    @Test
    @Order(4)
    @DisplayName("Create working namespace and check list")
    public void should_create_working_namespace() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#04 Create working namespaces" + ANSI_RESET);
        
        if (!clientApiDoc.namespace(WORKING_NAMESPACE).exist()) {
            clientApiDoc.namespace(WORKING_NAMESPACE).createSimple(1);
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
            int wait = 0;
            while (wait++ < 5 && !clientApiDoc.namespace(WORKING_NAMESPACE).exist()) {
                Thread.sleep(1000);
                System.out.println("+ ");
            }
        }
        Assertions.assertTrue(clientApiDoc.namespace(WORKING_NAMESPACE).exist());
        Assertions.assertFalse(clientApiDoc.namespace("invalid").exist());
        // When
        Set<String> namespaces = clientApiDoc.namespaceNames().collect(Collectors.toSet());
        // Then
        Assertions.assertTrue(namespaces.contains(WORKING_NAMESPACE));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Namespaces created");
    }
    
    */
    
    @Test
    @Order(5)
    @DisplayName("Create working namespace and check list")
    public void should_fail_on_invalid_namespace_params() {
        System.out.println(ANSI_YELLOW + "\n#05 Fail on invalid namespaces" + ANSI_RESET);
        NamespaceClient dc = StargateClient.builder().disableCQL().build().apiDocument().namespace("???df.??");
        Assertions.assertAll("Required parameters", () -> Assertions.assertThrows(RuntimeException.class, () -> {
            StargateClient.builder().disableCQL().build().apiDocument().namespace("?AA???").collectionNames();
        }));

        Assertions.assertAll("Required parameters", () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.exist();
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.collectionNames();
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.delete();
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.find();
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.createSimple(1);
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.create(new DataCenter(cloudRegion.get(), 1));
        }));
        
        Assertions.assertThrows(InvocationTargetException.class, () -> {
            Method method = NamespaceClient.class.getDeclaredMethod("marshallApiResponseNamespace", String.class);
            method.setAccessible(true);
            method.invoke(dc, (String)null);
        });
        
        Assertions.assertThrows(InvocationTargetException.class, () -> {
            Method method = NamespaceClient.class.getDeclaredMethod("marshallApiResponseCollections", String.class);
            method.setAccessible(true);
            method.invoke(dc,  (String)null);
        });
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Validation OK");
    }
    
    @Test
    @Order(6)
    @DisplayName("Create collection")
    public void should_create_collection() throws InterruptedException {
        // Operations on collections
        System.out.println(ANSI_YELLOW + "\n#06 Create Collection" + ANSI_RESET);
        // Create working collection is not present
        if (clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON).exist()) {
            clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON).delete();
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Delete collection request sent");
            Thread.sleep(500);
        }
        // Given
        Assertions.assertFalse(clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON).exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Collection does not exist");
        // When
        clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON).create();
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
        Thread.sleep(1000);
        // Then
        Assertions.assertTrue(clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON).exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Collection now exist");
    }

    @Test
    @Order(7)
    @DisplayName("Find Collection")
    public void shoudl_find_collection() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#07 Find Collection" + ANSI_RESET);
        Assertions.assertTrue(clientApiDoc
                .namespace(WORKING_NAMESPACE)
                .collectionNames().anyMatch(s -> COLLECTION_PERSON.equals(s)));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Collection is available in list");
    }
    
    @Test
    @Order(8)
    @DisplayName("Delete collection")
    public void should_delete_collection() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#08 Delete Collection" + ANSI_RESET);
        
        // Given
        String randomCollection = UUID.randomUUID().toString().replaceAll("-", "");
        Assertions.assertFalse(clientApiDoc.namespace(WORKING_NAMESPACE).collection(randomCollection).exist());
        // When
        clientApiDoc.namespace(WORKING_NAMESPACE).collection(randomCollection).create();
        Thread.sleep(1000);
        // Then
        Assertions.assertTrue(clientApiDoc.namespace(WORKING_NAMESPACE).collection(randomCollection).exist());
        // When
        clientApiDoc.namespace(WORKING_NAMESPACE).collection(randomCollection).delete();
        Thread.sleep(1000);
        // Then
        Assertions.assertFalse(clientApiDoc.namespace(WORKING_NAMESPACE).collection(randomCollection).exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Collection deleted");
    }

    @Test
    @Order(9)
    @DisplayName("Create document")
    public void should_create_newDocument() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#09 Create document" + ANSI_RESET);
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(collectionPersonAstra.exist());
        // When
        String docId = collectionPersonAstra.create(new PersonAstra("loulou", "looulou", 20, new Address("Paris", 75000)));
        // Then
        Assertions.assertNotNull(docId);
        Thread.sleep(1000);
        Assertions.assertTrue(collectionPersonAstra.document(docId).exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document created");
    }
    
    @Test
    @Order(10)
    @DisplayName("Order document")
    public void should_upsert_document_create() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#10 Upsert document with" + ANSI_RESET);
        // Given
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(collectionPersonAstra.exist());
        // When
        collectionPersonAstra.document("myId").upsert(new PersonAstra("loulou", "looulou", 20, new Address("Paris", 75000)));

        Thread.sleep(500);
        // Then
        Assertions.assertTrue(collectionPersonAstra.document("myId").exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document created");
    }

    @Test
    @Order(11)
    @DisplayName("Update document")
    public void should_upsert_document_update() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#11 Upsert document update" + ANSI_RESET);
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(collectionPersonAstra.exist());
        String uid = UUID.randomUUID().toString();
        Assertions.assertFalse(collectionPersonAstra.document(uid).exist());
        // When
        collectionPersonAstra.document(uid).upsert(new PersonAstra("loulou", "looulou", 20, new Address("Paris", 75000)));
        collectionPersonAstra.document(uid).upsert(new PersonAstra("loulou", "looulou", 20, new Address("Paris", 75015)));
        // Then
        Optional<PersonAstra> loulou = collectionPersonAstra.document(uid).find(PersonAstra.class);
        Assertions.assertTrue(loulou.isPresent());
        Assertions.assertEquals(75015, loulou.get().getAddress().getZipCode());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document updated");
    }

    @Test
    @Order(12)
    @DisplayName("Delete document")
    public void should_delete_document() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#12 delete a document" + ANSI_RESET);
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        String uid = UUID.randomUUID().toString();
        Assertions.assertFalse(collectionPersonAstra.document(uid).exist());
        // When
        collectionPersonAstra.document(uid).upsert(new PersonAstra("loulou", "looulou", 20, new Address("Paris", 75000)));
        // Then
        Assertions.assertTrue(collectionPersonAstra.document(uid).exist());
        collectionPersonAstra.document(uid).delete();
        Thread.sleep(1000);
        Assertions.assertFalse(collectionPersonAstra.document(uid).exist());
        Assertions.assertTrue(collectionPersonAstra.document(uid).find(String.class).isEmpty());
        Assertions.assertThrows(RuntimeException.class, () -> {
            collectionPersonAstra.document(uid).delete();
        });
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document deleted");
    }

    @Test
    @Order(13)
    @DisplayName("Update document")
    public void should_update_document() {
        System.out.println(ANSI_YELLOW + "\n#13 Update a document" + ANSI_RESET);
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(collectionPersonAstra.exist());
        // When
        collectionPersonAstra.document("AAA").upsert(new PersonAstra("loulou", "looulou", 20, new Address("Paris", 75000)));
        collectionPersonAstra.document("AAA").update(new PersonAstra("a", "b"));
        // Then
        Optional<PersonAstra> loulou = collectionPersonAstra.document("AAA").find(PersonAstra.class);
        Assertions.assertTrue(loulou.isPresent());
        // Then sub fields are still there
        Assertions.assertEquals(75000, loulou.get().getAddress().getZipCode());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document updated");
    }

    @Test
    @Order(14)
    @DisplayName("Find All PersonAstra")
    public void should_find_all_PersonAstra() {
        System.out.println(ANSI_YELLOW + "\n#14 FindAll" + ANSI_RESET);
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc
                .namespace(WORKING_NAMESPACE)
                .collection(COLLECTION_PERSON);
        Assertions.assertTrue(collectionPersonAstra.exist());
        // When
        DocumentResultPage<PersonAstra> results = collectionPersonAstra.findAllPageable(PersonAstra.class);
        // Then
        Assert.assertNotNull(results);
        for (ApiDocument<PersonAstra> PersonAstra : results.getResults()) {
            Assert.assertNotNull(PersonAstra);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document list found");
    }

    @Test
    @Order(15)
    @DisplayName("Search Query")
    public void should_search_withQuery() {
        System.out.println(ANSI_YELLOW + "\n#15 Find with where clause" + ANSI_RESET);
        // Given
        CollectionClient collectionPersonAstra = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(collectionPersonAstra.exist());

        collectionPersonAstra.document("PersonAstra1").upsert(new PersonAstra("PersonAstra1", "PersonAstra1", 20, new Address("Paris", 75000)));
        collectionPersonAstra.document("PersonAstra2").upsert(new PersonAstra("PersonAstra2", "PersonAstra2", 30, new Address("Paris", 75000)));
        collectionPersonAstra.document("PersonAstra3").upsert(new PersonAstra("PersonAstra3", "PersonAstra3", 40, new Address("Melun", 75000)));
        Assertions.assertTrue(collectionPersonAstra.document("PersonAstra1").exist());
        Assertions.assertTrue(collectionPersonAstra.document("PersonAstra2").exist());
        Assertions.assertTrue(collectionPersonAstra.document("PersonAstra3").exist());

        // Create a query
        SearchDocumentQuery query = SearchDocumentQuery.builder().where("age").isGreaterOrEqualsThan(21).build();

        // Execute q query
        DocumentResultPage<PersonAstra> results = collectionPersonAstra.searchPageable(query, PersonAstra.class);
        Assert.assertNotNull(results);
        for (ApiDocument<PersonAstra> PersonAstra : results.getResults()) {
            Assert.assertNotNull(PersonAstra);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Document list found");
    }
    

    @Test
    @Order(16)
    @DisplayName("Invalid parameters")
    public void testInvalidDoc() {
        System.out.println(ANSI_YELLOW + "\n#16 Parameter validation when working with documents" + ANSI_RESET);
        DocumentClient dc = StargateClient.builder().disableCQL().build().apiDocument().namespace("n").collection("c")
                .document("??a=&invalid??");

        Assertions.assertAll("Required parameters", () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.exist();
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.delete();
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.upsert("X");
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.update("X");
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.find(String.class);
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.findSubDocument("a", String.class);
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.replaceSubDocument("a", String.class);
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.updateSubDocument("a", String.class);
        }), () -> Assertions.assertThrows(RuntimeException.class, () -> {
            dc.deleteSubDocument("a");
        }));

        Assertions.assertThrows(InvocationTargetException.class, () -> {
            Method method = DocumentClient.class.getDeclaredMethod("marshallDocumentId", String.class);
            method.setAccessible(true);
            method.invoke(dc, "invalid_body");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Method method = DocumentClient.class.getDeclaredMethod("marshallDocument", String.class, Class.class);
            method.setAccessible(true);
            method.invoke(dc, "invalid_body");
        });
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Validation OK");
    }

    @Test
    @Order(17)
    @DisplayName("Find sub doc")
    public void should_find_subdocument() {
        System.out.println(ANSI_YELLOW + "\n#17 Find a sub document" + ANSI_RESET);
        // Given, Collection exist, Document Exist
        CollectionClient cc = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(cc.exist());

        // Create doc
        DocumentClient p1 = cc.document("PersonAstra1");
        p1.upsert(new PersonAstra("PersonAstra1", "PersonAstra1", 20, new Address("Paris", 75000)));
        Assertions.assertTrue(p1.find(PersonAstra.class).isPresent());

        // When
        Optional<String> os = p1.findSubDocument("firstname", String.class);
        Assertions.assertTrue(os.isPresent());
        Assertions.assertTrue(os.get().length() > 0);

        // When
        Optional<Integer> oi = p1.findSubDocument("age", Integer.class);
        Assertions.assertTrue(oi.isPresent());
        Assertions.assertEquals(20, oi.get());

        // When
        Optional<Address> oa = p1.findSubDocument("address", Address.class);
        Assertions.assertTrue(oa.isPresent());
        Assertions.assertEquals(75000, oa.get().getZipCode());

        // When
        Optional<Integer> oz = p1.findSubDocument("address/zipCode", Integer.class);
        Assertions.assertTrue(oz.isPresent());
        Assertions.assertEquals(75000, oz.get());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Sub document retrieved");
    }

    @Test
    @Order(18)
    @DisplayName("Update sub doc")
    public void should_update_subdocument() {
        System.out.println(ANSI_YELLOW + "\n#18 Update a sub document" + ANSI_RESET);
        // Given
        CollectionClient cc = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(cc.exist());

        DocumentClient p1 = cc.document("PersonAstra1");
        p1.upsert(new PersonAstra("PersonAstra1", "PersonAstra1", 20, new Address("Paris", 75000)));
        Assertions.assertTrue(p1.find(PersonAstra.class).isPresent());

        // When
        p1.replaceSubDocument("address", new Address("city2", 8000));
        // Then
        Address updated = (Address) p1.findSubDocument("address", Address.class).get();
        Assertions.assertEquals(8000, updated.getZipCode());
        Assertions.assertEquals("city2", updated.getCity());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Sub document updated");
    }

    @Test
    @Order(19)
    @DisplayName("Delete sub doc")
    public void should_delete_subdocument() {
        System.out.println(ANSI_YELLOW + "\n#19 Delete a sub document" + ANSI_RESET);
        // Given
        CollectionClient cc = clientApiDoc.namespace(WORKING_NAMESPACE).collection(COLLECTION_PERSON);
        Assertions.assertTrue(cc.exist());
        DocumentClient p1 = cc.document("PersonAstra1");
        p1.upsert(new PersonAstra("PersonAstra1", "PersonAstra1", 20, new Address("Paris", 75000)));
        Assertions.assertTrue(p1.find(PersonAstra.class).isPresent());
        Assertions.assertFalse(p1.findSubDocument("address", Address.class).isEmpty());
        // When
        p1.deleteSubDocument("address");
        // Then
        Assertions.assertTrue(p1.findSubDocument("address", Address.class).isEmpty());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Sub document deleted");
    }
}
