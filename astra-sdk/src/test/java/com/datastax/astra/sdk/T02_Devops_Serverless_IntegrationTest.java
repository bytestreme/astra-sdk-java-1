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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.datastax.astra.sdk.devops.ApiDevopsClient;
import com.datastax.astra.sdk.devops.CloudProviderType;
import com.datastax.astra.sdk.devops.DatabaseStatusType;
import com.datastax.astra.sdk.devops.DatabaseTierType;
import com.datastax.astra.sdk.devops.req.DatabaseCreationRequest;
import com.datastax.astra.sdk.devops.res.Database;
import com.datastax.astra.sdk.devops.res.DatabaseAvailableRegion;

@TestMethodOrder(OrderAnnotation.class)
public class T02_Devops_Serverless_IntegrationTest extends AbstractAstraIntegrationTest {
    
    private static final String SERVERLESS_DB_NAME   = "sdk_test_devops";
    private static final String SERVERLESS_KEYSPACE  = "sdk_ks1";
    private static final String SECOND_KEYSPACE      = "sdk_ks2";
    private static final String SERVERLESS_NAMESPACE = "sdk_ns1";
    
    @BeforeAll
    public static void config() {
        System.out.println(ANSI_YELLOW + "[Astra DEVOPS Api Test Suite]" + ANSI_RESET);
    }
    
    @Test
    @Order(1)
    public void should_fail_on_invalid_params() {
        System.out.println(ANSI_YELLOW + "- Parameter validation" + ANSI_RESET);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ApiDevopsClient(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ApiDevopsClient(null));
    }
    
    @Test
    @Order(2)
    public void should_connect_to_astra_withAstraClient() {
        System.out.println(ANSI_YELLOW + "- Connection with AstraClient" + ANSI_RESET);
        
        // Given
        Assertions.assertTrue(appToken.isPresent());
        // When
        try(AstraClient cli = AstraClient.builder()
                .appToken(appToken.get())
                .build()) {
            // Then
            Assertions.assertNotNull(cli
                    .apiDevops()
                    .findAllAvailableRegions().collect(Collectors.toList()).size() > 1);
         }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Can connect to ASTRA with token");
    }
    
    @Test
    @Order(3)
    public void should_connect_to_astra_directDevops() {
        System.out.println(ANSI_YELLOW + "- Connection with DevopsClient" + ANSI_RESET);
        
        // Given
        Assertions.assertTrue(appToken.isPresent());
        // When
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // Then
        Assert.assertTrue(cli.findAllAvailableRegions().collect(Collectors.toList()).size() > 1);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Can connect to ASTRA with token");
    }
    
    
    @Test
    @Order(4)
    public void should_aws_us_east_be_available() {
        System.out.println(ANSI_YELLOW + "- AWS Region available" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // When
        Stream<DatabaseAvailableRegion> streamDb = cli.findAllAvailableRegions();
        Map <DatabaseTierType, Map<CloudProviderType,List<DatabaseAvailableRegion>>> available = 
                cli.mapAvailableRegions(streamDb);
        // Then
        Assert.assertTrue(available
                .containsKey(DatabaseTierType.serverless));
        Assert.assertTrue(available
                .get(DatabaseTierType.serverless)
                .containsKey(CloudProviderType.AWS));
        Assert.assertTrue(available
                .get(DatabaseTierType.serverless)
                .get(CloudProviderType.AWS).stream()
                .anyMatch(db -> "us-east-1".equalsIgnoreCase(db.getRegion())));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Tier `serverless` for region 'aws/us-east-1' is available");
    }
    
    public static String serverlessDbId;
    
    @Test
    @Order(5)
    public void should_createServerlessDb() {
        System.out.println(ANSI_YELLOW + "- DB Creation" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // When
        Assert.assertFalse(cli.findDatabasesNonTerminatedByName(SERVERLESS_DB_NAME).collect(Collectors.toSet()).size()>0);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Instance with name'" + SERVERLESS_DB_NAME + "' does not exist.");
        // When
        DatabaseCreationRequest dcr = DatabaseCreationRequest
                .builder()
                .name(SERVERLESS_DB_NAME)
                .tier(DatabaseTierType.serverless)
                .cloudProvider(CloudProviderType.AWS)
                .cloudRegion("us-east-1")
                .keyspace(SERVERLESS_KEYSPACE)
                .username("...")
                .password("...")
                .build();
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createDatabase(null));
        
        serverlessDbId = cli.createDatabase(dcr);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - DB Creation request successful");
        
        // Then
        Assert.assertTrue(cli.databaseExist(serverlessDbId));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Database id=" + serverlessDbId);
        System.out.print(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Initializing ");
        while(!DatabaseStatusType.ACTIVE.equals(cli.findDatabaseById(serverlessDbId).get().getStatus())) {
            System.out.print(ANSI_GREEN + "\u25a0" +ANSI_RESET); 
            waitForSeconds(5);
        }
        Assert.assertEquals(DatabaseStatusType.ACTIVE, cli.findDatabaseById(serverlessDbId).get().getStatus());
        System.out.println(ANSI_GREEN + "\n[OK]" + ANSI_RESET + " - DB is active");
    }
    
    
    @Test
    @Order(6)
    public void should_find_databases() {
        System.out.println(ANSI_YELLOW + "- [GET] Returns a list of databases" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // When
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.findDatabaseById(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.findDatabaseById(null));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Validated parameters are required");
       
        Assert.assertFalse(cli.findDatabaseById("i-like-cheese").isPresent());
        
        Assert.assertTrue(cli.findAllDatabasesNonTerminated().anyMatch(
                db -> SERVERLESS_DB_NAME.equals(db.getInfo().getName())));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - List retrieved and the new created DB is present");
    }
        
    @Test
    @Order(7)
    public void should_find_databases_by_id() {
        System.out.println(ANSI_YELLOW + "- [GET] Finds database by ID" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        Optional<Database> odb = cli.findDatabaseById(serverlessDbId);
        Assert.assertTrue(odb.isPresent());
        Assert.assertEquals(DatabaseStatusType.ACTIVE, odb.get().getStatus());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - DB can be loaded from its id");
        Assert.assertNotNull(odb.get().getInfo());
        Assert.assertEquals(DatabaseTierType.serverless, odb.get().getInfo().getTier());
        Assert.assertNotNull(odb.get().getMetrics());
        Assert.assertNotNull(odb.get().getStorage());
        Assert.assertNotNull(SERVERLESS_KEYSPACE, odb.get().getInfo().getKeyspace());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Fields metrics,storage,info are populated");
    }
    
    @Test
    @Order(8)
    public void should_crud_keyspaces() {
        System.out.println(ANSI_YELLOW + "- [POST] Adds keyspace into database" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // Check Parameters
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createNamespace(serverlessDbId, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createNamespace("", SERVERLESS_NAMESPACE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createKeyspace(serverlessDbId, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createKeyspace(null, SECOND_KEYSPACE));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Parameters validation is working");
        
        // Given
        Assert.assertFalse(cli.findDatabaseById(serverlessDbId).get().getInfo().getKeyspaces().contains(SECOND_KEYSPACE));
        // When
        cli.createKeyspace(serverlessDbId, SECOND_KEYSPACE);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace creation request successful for '" + SECOND_KEYSPACE + "'");
        Assert.assertEquals(DatabaseStatusType.MAINTENANCE, cli.findDatabaseById(serverlessDbId).get().getStatus());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - DB Switch in [MAINTENANCE] mode");
        while(DatabaseStatusType.ACTIVE != cli.findDatabaseById(serverlessDbId).get().getStatus() ) {
            waitForSeconds(1);
        }
        // When
        Assert.assertEquals(DatabaseStatusType.ACTIVE, cli.findDatabaseById(serverlessDbId).get().getStatus());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - DB Switch in [ACTIVE] mode");
        
        cli.createNamespace(serverlessDbId, SERVERLESS_NAMESPACE);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Namespace creation request successful for '" + SERVERLESS_NAMESPACE + "'");
        Assert.assertEquals(DatabaseStatusType.MAINTENANCE, cli.findDatabaseById(serverlessDbId).get().getStatus());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - DB Switch in [MAINTENANCE] mode");
        while(DatabaseStatusType.ACTIVE != cli.findDatabaseById(serverlessDbId).get().getStatus() ) {
            waitForSeconds(1);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - DB Switch in [ACTIVE] mode");
        // Then
        Database db = cli.findDatabaseById(serverlessDbId).get();
        Assert.assertTrue(db.getInfo().getKeyspaces().contains(SECOND_KEYSPACE));
        Assert.assertTrue(db.getInfo().getKeyspaces().contains(SERVERLESS_NAMESPACE));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected keyspaces and namespaces are now present");
        
        // Cann create keyspace that already exist
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createNamespace(serverlessDbId, SERVERLESS_NAMESPACE));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - You cannot create an existing keyspace");
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.createKeyspace(serverlessDbId, SECOND_KEYSPACE));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - You cannot create an existing namespace");
    }
    
    @Test
    @Order(9)
    public void should_download_secureBundle() {
        System.out.println(ANSI_YELLOW + "- [POST] Obtain zip for connecting to the database" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        String randomFile = "/tmp/" + UUID.randomUUID().toString().replaceAll("-", "") + ".zip";
        Assert.assertFalse(new File(randomFile).exists());
        // When
        cli.downloadSecureConnectBundle(serverlessDbId, randomFile);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Downloading file call");
        // Then
        Assert.assertTrue(new File(randomFile).exists());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - File as been download in " + randomFile);
    }
    
    @Test
    @Order(10)
    public void should_not_parkserverless() {
        System.out.println(ANSI_YELLOW + "- Cannot park serverless" + ANSI_RESET);
        
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // (6) - Check that we cannot park a serverlessDB
        System.out.println(ANSI_YELLOW + "\n#10 [POST] Parks a database serverless is not possible)" + ANSI_RESET);
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.parkDatabase(serverlessDbId));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected exception retrieved");
    }
    
    @Test
    @Order(11)
    public void should_not_unpark_serverless() {
        System.out.println(ANSI_YELLOW + "- Cannot unpark serverless" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // (7)
        System.out.println(ANSI_YELLOW + "\n#11 [POST] Unparks a database serverless is not possible)" + ANSI_RESET);
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.unparkDatabase(serverlessDbId));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected exception retrieved");
    }
    
    @Test
    @Order(12)
    public void should_not_resize_erverless() {
        System.out.println(ANSI_YELLOW + "- Cannot resize serverless" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        
        // (8)
        System.out.println(ANSI_YELLOW + "\n#12 [POST] Resize a database serverless is not possible)" + ANSI_RESET);
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.resizeDatase(serverlessDbId, 2));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected exception retrieved");
    }
    
    @Test
    @Order(13)
    public void should_not_resetpassword() {
        System.out.println(ANSI_YELLOW + "- Cannot resect password" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        // (9)
        System.out.println(ANSI_YELLOW + "\n#13 - [POST] Reset a Password in a database serverless is not possible)" + ANSI_RESET);
        Assertions.assertThrows(IllegalArgumentException.class, () -> cli.resetPassword(serverlessDbId, "token", "cedrick1"));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected exception retrieved");
    }
    
    @Test
    @Order(14)
    public void should_terminate_db() {
        System.out.println(ANSI_YELLOW + "- [POST] Terminating an instance" + ANSI_RESET);
        // Given
        ApiDevopsClient cli = new ApiDevopsClient(appToken.get());
        cli.terminateDatabase(serverlessDbId);
        while(DatabaseStatusType.TERMINATING != cli.findDatabaseById(serverlessDbId).get().getStatus() ) {
            waitForSeconds(1);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Status changed to TERMINATING");
        while(DatabaseStatusType.TERMINATED != cli.findDatabaseById(serverlessDbId).get().getStatus() ) {
            waitForSeconds(1);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Status changed to TERMINATED");
    }
   
    
}
