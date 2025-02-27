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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.datastax.astra.dto.Video;
import com.datastax.astra.dto.VideoRowMapper;
import com.datastax.stargate.sdk.rest.ApiRestClient;
import com.datastax.stargate.sdk.rest.KeyClient;
import com.datastax.stargate.sdk.rest.TableClient;
import com.datastax.stargate.sdk.rest.domain.ClusteringExpression;
import com.datastax.stargate.sdk.rest.domain.ColumnDefinition;
import com.datastax.stargate.sdk.rest.domain.CreateIndex;
import com.datastax.stargate.sdk.rest.domain.CreateTable;
import com.datastax.stargate.sdk.rest.domain.IndexDefinition;
import com.datastax.stargate.sdk.rest.domain.Ordering;
import com.datastax.stargate.sdk.rest.domain.QueryWithKey;
import com.datastax.stargate.sdk.rest.domain.RowResultPage;
import com.datastax.stargate.sdk.rest.domain.SearchTableQuery;
import com.datastax.stargate.sdk.rest.domain.SortField;
import com.datastax.stargate.sdk.rest.domain.TableDefinition;
import com.datastax.stargate.sdk.rest.domain.TableOptions;

/**
 * DATASET
 * @author Cedrick LUNVEN (@clunven)
 */
@TestMethodOrder(OrderAnnotation.class)
public class T05_RestApi_IntegrationTest extends AbstractAstraIntegrationTest {
  
    private static final String WORKING_KEYSPACE = "sdk_test_ks";
    private static final String WORKING_TABLE    = "videos";
    
    private static ApiRestClient clientApiRest;
    
    @BeforeAll
    public static void config() {
        System.out.println(ANSI_YELLOW + "[T05_RestApi_IntegrationTest]" + ANSI_RESET);
        
        //initDb("sdk_test_restApi");
        
        client = AstraClient.builder()
                .databaseId("9bd1d0b7-c841-46a0-ae5e-aa15fbebbf23")
                .cloudProviderRegion("us-east-1")
                .appToken("AstraCS:TWRvjlcrgfZYfhcxGZhUlAZH:2174fb7dacfd706a2d14d168706022010e99a7bb7cd133050f46ee0d523b386d")
                .build();
        
        clientApiRest = client.apiStargateData();
        
        // Not available in the doc API anymore as of now
        if (!client.apiStargateData().keyspace(WORKING_KEYSPACE).exist()) {
            client.apiDevopsDatabases()
                  .database(dbId.get())
                  .createKeyspace(WORKING_KEYSPACE);
            System.out.print(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creating keyspace ");
            while(!client.apiStargateData().keyspace(WORKING_KEYSPACE).exist()) {
                System.out.print(ANSI_GREEN + "\u25a0" +ANSI_RESET); 
                waitForSeconds(1);
            }
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace created");
        } else {
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace already exist.");
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Parameter validations should through IllegalArgumentException(s)")
    public void builderParams_should_not_be_empty() {
        System.out.println(ANSI_YELLOW + "\n#01 Checking required parameters " + ANSI_RESET);
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
    @DisplayName("Create and delete keyspace with replicas")
    public void should_create_tmp_keyspace()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#02 Working with Keyspace" + ANSI_RESET);
        if (clientApiRest.keyspace("tmp_keyspace").exist()) {
            clientApiRest.keyspace("tmp_keyspace").delete();
            int wait = 0;
            while (wait++ < 5 && clientApiRest.keyspace("tmp_keyspace").exist()) {
                Thread.sleep(1000);
                System.out.println("+ ");
            }
        }
        clientApiRest.keyspace("tmp_keyspace").createSimple(1);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
        int wait = 0;
        while (wait++ < 5 && !clientApiRest.keyspace("tmp_keyspace").exist()) {
            Thread.sleep(1000);
        }
        clientApiRest.keyspace("tmp_keyspace").delete();
        wait = 0;
        while (wait++ < 5 && clientApiRest.keyspace("tmp_keyspace").exist()) {
            Thread.sleep(1000);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace craeted");
    }
    
    @Test
    @Order(3)
    @DisplayName("Create and delete keyspace with datacenter")
    public void should_create_tmp_keyspace2() throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#03 Working with Keyspaces 2" + ANSI_RESET);
        // TMP KEYSPACE
        if (clientApiRest.keyspace("tmp_keyspace2").exist()) {
            clientApiRest.keyspace("tmp_keyspace2").delete();
            int wait = 0;
            while (wait++ < 5 && clientApiRest.keyspace("tmp_keyspace2").exist()) {
                Thread.sleep(1000);
            }
        }
        clientApiRest.keyspace("tmp_keyspace2").create(new DataCenter(cloudRegion.get(), 1));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
        int wait = 0;
        while (wait++ < 5 && !clientApiRest.keyspace("tmp_keyspace2").exist()) {
            Thread.sleep(1000);
            System.out.println("+ ");
        }

        clientApiRest.keyspace("tmp_keyspace2").delete();
        wait = 0;
        while (wait++ < 5 && clientApiRest.keyspace("tmp_keyspace2").exist()) {
            Thread.sleep(1000);
            System.out.println("+ ");
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace created");
    }
    
    @Test
    @Order(4)
    @DisplayName("Create working namespace and check list")
    public void should_create_keyspace() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#04 Working with Keyspace" + ANSI_RESET);
        if (!clientApiRest.keyspace(WORKING_KEYSPACE).exist()) {
            clientApiRest.keyspace(WORKING_KEYSPACE)
                         .create(new DataCenter(cloudRegion.get(), 3));
            System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creation request sent");
            int wait = 0;
            while (wait++ < 5 && !clientApiRest.keyspace(WORKING_KEYSPACE).exist()) {
                Thread.sleep(2000);
                 System.out.print("+ ");
            }
        }
        Assertions.assertTrue(clientApiRest.keyspace(WORKING_KEYSPACE).exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace " + WORKING_KEYSPACE + "created");
    }
    */
    
    @Test
    @Order(5)
    public void working_keyspace_should_exist() {
        System.out.println(ANSI_YELLOW + "\n#05 Keyspace should exist" + ANSI_RESET);
        Assertions.assertTrue(clientApiRest
                .keyspaceNames()
                .collect(Collectors.toSet())
                .contains(WORKING_KEYSPACE));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace found");
    }
    
    /*
    
    @Test
    @Order(6)
    public void shoud_delete_keyspace() throws InterruptedException {
        // Given
        System.out.println(ANSI_YELLOW + "\n#06 Delete a keyspace" + ANSI_RESET);
        clientApiRest.keyspace("tmp_keyspace3").createSimple(3);
        int wait = 0;
        while (wait++ < 5 && !clientApiRest.keyspace("tmp_keyspace3").exist()) {
            Thread.sleep(1000);
        }
        Assertions.assertTrue(clientApiRest.keyspace("tmp_keyspace3").exist());
        
        // When
        clientApiRest.keyspace("tmp_keyspace3").delete();
        wait = 0;
        while (wait++ < 5 && !clientApiRest.keyspace("tmp_keyspace2").exist()) {
            Thread.sleep(1000);
        }
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Keyspace deleted");
    }
    
    */
    
    // CRUD ON TABLES
    
    /*
    * CREATE TABLE IF NOT EXISTS videos (
    *   genre     text,
    *   year      int,
    *   title     text,
    *   upload    timestamp,
    *   tags      set <text>,
    *   frames    list<int>,
    *   tuples    tuple<text,text,text>,
    *   formats   frozen<map <text,text>>,
    *   PRIMARY KEY ((genre), year, title)
    * ) WITH CLUSTERING ORDER BY (year DESC, title ASC);
    * 
    */
    @Test
    @Order(7)
    public void should_create_table() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#07 Create a table" + ANSI_RESET);
        Assertions.assertTrue(clientApiRest.keyspace(WORKING_KEYSPACE).exist());
        TableClient tc = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE + "_tmp");
        if (tc.exist()) {
            tc.delete();
            int wait = 0;
            while (wait++ < 10 && tc.exist()) {
                Thread.sleep(1000);
            }
        }
        Assertions.assertFalse(tc.exist());
        // Core Request
        CreateTable tcr = new CreateTable();
        tcr.setIfNotExists(true);
        tcr.getColumnDefinitions().add(new ColumnDefinition("genre", "text"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("year", "int"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("title", "text"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("upload", "timestamp"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("tags", "set<text>"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("frames", "list<int>"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("tuples", "tuple<text,text,text>"));
        tcr.getColumnDefinitions().add(new ColumnDefinition("formats", "frozen<map <text,text>>"));
        tcr.getPrimaryKey().getPartitionKey().add("genre");
        tcr.getPrimaryKey().getClusteringKey().add("year");
        tcr.getPrimaryKey().getClusteringKey().add("title");
        tcr.getTableOptions().getClusteringExpression().add(new ClusteringExpression("year", Ordering.DESC));
        tcr.getTableOptions().getClusteringExpression().add(new ClusteringExpression("title", Ordering.ASC));
        tc.create(tcr);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creating table " + WORKING_TABLE + "_tmp");
        int wait = 0;
        while (wait++ < 10 && !clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE + "_tmp").exist()) {
            Thread.sleep(1000);
        }
        Assertions.assertTrue(clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE + "_tmp").exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Table " + WORKING_TABLE + "_tmp now exists.");
        
        
        TableClient working_table = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        if (working_table.exist()) {
            working_table.delete();
            wait = 0;
            while (wait++ < 10 && working_table.exist()) {
                Thread.sleep(1000);
            }
        }
        Assertions.assertFalse(working_table.exist());
        
        // With a Builder
        working_table.create(CreateTable.builder()
                       .ifNotExist(true)
                       .addPartitionKey("genre", "text")
                       .addClusteringKey("year", "int", Ordering.DESC)
                       .addClusteringKey("title", "text", Ordering.ASC)
                       .addColumn("upload", "timestamp")
                       .addColumn("tags", "set<text>")
                       .addColumn("frames", "list<int>")
                       .addColumn("tuples", "tuple<text,text,text>")
                       .addColumn("formats", "frozen<map <text,text>>")
                       .build());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Creating table " + WORKING_TABLE);
        wait = 0;
        while (wait++ < 10 && !working_table.exist()) {
            Thread.sleep(1000);
        }
        Assertions.assertTrue(working_table.exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Table " + WORKING_TABLE + " now exists.");
    }
    
    @Test
    @Order(8)
    public void should_list_tables_definition() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#08 List tables in a keyspace" + ANSI_RESET);
        Assertions.assertTrue(clientApiRest
                .keyspace(WORKING_KEYSPACE)
                .tables().count() > 0);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - List OK");
    }
    
    @Test
    @Order(9)
    public void should_table_find() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#09 Table find" + ANSI_RESET);
        Optional<TableDefinition> otd = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE).find();
        Assertions.assertTrue(otd.isPresent());
        Assertions.assertEquals("genre", otd.get().getPrimaryKey().getPartitionKey().get(0));
        Assertions.assertEquals("year", otd.get().getPrimaryKey().getClusteringKey().get(0));
        Assertions.assertEquals("title", otd.get().getPrimaryKey().getClusteringKey().get(1));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Working table found");
    }
    
    @Test
    @Order(10)
    public void should_list_tables_names() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#10 List tables names in a keyspace" + ANSI_RESET);
        Assertions.assertTrue(clientApiRest
                .keyspace(WORKING_KEYSPACE)
                .tableNames().count()>0);
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Name list OK");
    }
    
    @Test
    @Order(11)
    public void should_table_exist() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#11 Table exist" + ANSI_RESET);
        Assertions.assertTrue(clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE).exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Working table found");
    }
    
    @Test
    @Order(12)
    public void should_delete_table_exist() 
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#12 Delete a table" + ANSI_RESET);
        // Given
        Assertions.assertTrue(clientApiRest
                .keyspace(WORKING_KEYSPACE)
                .exist());
        Assertions.assertTrue(clientApiRest
                .keyspace(WORKING_KEYSPACE)
                .table(WORKING_TABLE + "_tmp")
                .exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Target table exist");
        // When
        clientApiRest
            .keyspace(WORKING_KEYSPACE)
            .table(WORKING_TABLE + "_tmp").delete();
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Delete request sent");
        
        int wait = 0;
        while (wait++ < 10 && clientApiRest
                .keyspace(WORKING_KEYSPACE)
                .table(WORKING_TABLE + "_tmp").exist()) {
            Thread.sleep(1000);
        }
        
        // Then
        Assertions.assertFalse(clientApiRest
                .keyspace(WORKING_KEYSPACE)
                .table(WORKING_TABLE + "_tmp")
                .exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Target table has been deleted");
        
    }
    
    @Test
    @Order(13)
    public void should_update_tableOptions()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#13 Update table metadata" + ANSI_RESET);
        // Given
        TableClient videoTable = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(videoTable.exist());
        Assertions.assertNotEquals(25, videoTable.find().get().getTableOptions().getDefaultTimeToLive());
        // When
        videoTable.updateOptions(new TableOptions(25, null));
        // Then
        Assertions.assertNotEquals(25, videoTable.find().get().getTableOptions().getDefaultTimeToLive());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Table updated");
        videoTable.updateOptions(new TableOptions(0,null));
    }
    
    @Test
    @Order(14)
    public void should_list_columns()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#14 list columns" + ANSI_RESET);
        // Given
        TableClient videoTable = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(videoTable.exist());
        // When
        Assertions.assertTrue(videoTable.columns().filter(c -> "frames".equalsIgnoreCase(c.getName())).findFirst().isPresent());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected columns OK");
        // When
        Assertions.assertTrue(videoTable.columnNames().collect(Collectors.toList()).contains("frames"));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Expected columns names OK");
        
    }
    
    @Test
    @Order(15)
    public void should_find_a_columns()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#15 Find a column" + ANSI_RESET);
        // Given
        TableClient videoTable = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(videoTable.exist());
        // When
        Assertions.assertTrue(videoTable.column("frames").find().isPresent());
        Assertions.assertTrue(videoTable.column("frames").exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Column found");
    }
        
    @Test
    @Order(16)
    public void should_create_a_columns()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#16 Create a column" + ANSI_RESET);
        // Given
        TableClient videoTable = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(videoTable.exist());
        Assertions.assertFalse(videoTable.column("custom").find().isPresent());
        // Given
        videoTable.column("custom").create(new ColumnDefinition("custom", "text"));
        // Then
        Assertions.assertTrue(videoTable.column("custom").find().isPresent());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Column created");
    }
    
    @Test
    @Order(17)
    public void should_delete_a_columns()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#17 Delete a column" + ANSI_RESET);
        // Given
        TableClient videoTable = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(videoTable.exist());
        Assertions.assertTrue(videoTable.column("custom").find().isPresent());
        // Given
        videoTable.column("custom").delete();
        // Then
        Assertions.assertFalse(videoTable.column("custom").find().isPresent());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Column Deleted");
    }
    
    @Test
    @Order(18)
    public void should_rename_clustering_columns()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#18 Updating a column" + ANSI_RESET);
        // Given
        TableClient videoTable = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(videoTable.exist());
        Assertions.assertTrue(videoTable.column("title").exist());
        Assertions.assertTrue(videoTable.find().get().getPrimaryKey().getClusteringKey().contains("title"));
        // When
        videoTable.column("title").rename("new_title");
        // Then
        Assertions.assertTrue(videoTable.column("new_title").find().isPresent());
        // Put back original name
        videoTable.column("new_title").rename("title");
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Column Updated");
    }
    
    @Test
    @Order(19)
    public void should_create_secondaryIndex()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#19 Create Secondary Index" + ANSI_RESET);
        // Given
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
        Assertions.assertFalse(tableVideo.index("idx_test").exist());
        // When
        tableVideo.index("idx_test").create(
                CreateIndex.builder().column("title").build());
        // Then
        Assertions.assertTrue(tableVideo.index("idx_test").exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Now exist");
        IndexDefinition idxDef = tableVideo.index("idx_test").find().get();
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Index type " + idxDef.getKind());
    }
    
    @Test
    @Order(20)
    public void should_delete_secondaryIndex()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#20 Delete Secondary Index" + ANSI_RESET);
        // Given
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
        Assertions.assertTrue(tableVideo.index("idx_test").exist());
        // When
        tableVideo.index("idx_test").delete();
        // Then
        Assertions.assertFalse(tableVideo.index("idx_test").exist());
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Index has been deleted");
    }
    
    // ==============================================================
    // ========================= DATA ===============================
    // ==============================================================
    
    // Still need to implement addData to automate this test but good results
    @Test
    @Order(21)
    public void should_add_row()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#21 Should add row" + ANSI_RESET);
        // Given
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
       
        Map<String, Object> data = new HashMap<>();
        data.put("genre", "Sci-Fi");
        data.put("year", 1990);
        data.put("title", "Test Line");
        data.put("frames", "[ 1, 2, 3 ]");
        data.put("formats", "{ '2020':'good', '2019':'okay' }");
        data.put("tags", "{ 'Emma', 'The Color Purple' }");
        data.put("tuples", "( 'France', '2016-01-01', '2020-02-02' )");
        data.put("upload", 1618411879135L);
        tableVideo.upsert(data);
        
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Line added");
        
        data.put("title", "title2");
        tableVideo.upsert(data);
    }
    
    
    @Test
    @Order(22)
    public void should_delete_row()
    throws InterruptedException {
        
        System.out.println(ANSI_YELLOW + "\n#22 Should delete row" + ANSI_RESET);
        // Given
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
        
        KeyClient record = tableVideo.key("Sci-Fi", 1990);
        RowResultPage rrp = record.find(QueryWithKey.builder().build());
        Assertions.assertTrue(rrp.getResults().size() > 0);
        
        record.delete();
        rrp = record.find(QueryWithKey.builder().build());
        Assertions.assertTrue(rrp.getResults().size() == 0);
        
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Line deleted");
        
    }
    
    @Test
    @Order(23)
    @SuppressWarnings("unchecked")
    public void should_update_row()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#23 Update a Row" + ANSI_RESET);
        
        // Given
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
        Map<String, Object> data = new HashMap<>();
        data.put("genre", "Sci-Fi");
        data.put("year", 1990);
        data.put("title", "line_update");
        data.put("upload", 1618411879135L);
        tableVideo.upsert(data);
        
        // When updating just a value
        Map<String, Object> update = new HashMap<>();
        update.put("upload", 1618411879130L);
        tableVideo.key("Sci-Fi", 1990, "line_update").update(update);
        
        // Then
        RowResultPage rrp = tableVideo
                .key("Sci-Fi",1990, "line_update")
                .find(QueryWithKey.builder().build());
        Assertions.assertTrue(rrp.getResults().size() == 1);
        Map<String, Object > map = (Map<String, Object>) rrp.getResults().get(0).get("upload");
        Assertions.assertEquals(130000000, map.get("nano"));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Line updated");
    }
    
    @Test
    @Order(24)
    @SuppressWarnings("unchecked")
    public void should_replace_row()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#24 Replace a Row" + ANSI_RESET);
        
        // Given
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
        Map<String, Object> data = new HashMap<>();
        data.put("genre", "Sci-Fi");
        data.put("year", 1990);
        data.put("title", "line_replace");
        data.put("upload", 1618411879135L);
        tableVideo.upsert(data);
        // When updating just a value
        Map<String, Object> replace = new HashMap<>();
        replace.put("upload", 1618411879130L);
        tableVideo.key("Sci-Fi", 1990, "line_update").replace(replace);
        // Then
        RowResultPage rrp = tableVideo
                .key("Sci-Fi",1990, "line_update")
                .find(QueryWithKey.builder().build());
        Assertions.assertTrue(rrp.getResults().size() == 1);
        Map<String, Object > map = (Map<String, Object>) rrp.getResults().get(0).get("upload");
        Assertions.assertEquals(130000000, map.get("nano"));
        System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET + " - Line Replaced");
    }
        
    @Test
    @Order(25)
    public void should_get_rows_pk()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#25 Retrieves row from primaryKey" + ANSI_RESET);
        // Given
        TableClient tmp_table = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tmp_table.exist());
        
        RowResultPage rrp = tmp_table.key("Sci-Fi",1990)
                .find(QueryWithKey.builder()
                        .addSortedField("year", Ordering.ASC)
                        .build());
        Assertions.assertEquals(2, rrp.getResults().size());
    }
    
    @Test
    @Order(26)
    public void should_get_rows_pk_mapper()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#26 Retrieve row from primaryKey with RowMapper" + ANSI_RESET);
        // Given
        TableClient tmp_table = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tmp_table.exist());
        
        List<Video> result = tmp_table.key("Sci-Fi",1990)
            .find(QueryWithKey.builder()
                .addSortedField("year", Ordering.ASC)
                .build(), new VideoRowMapper())
                .getResults();
        Assertions.assertEquals(2, result.size());
    }
    
    @Test
    @Order(27)
    public void should_rsearch_table()
    throws InterruptedException {
        System.out.println(ANSI_YELLOW + "\n#27 Searching for Row" + ANSI_RESET);
        TableClient tableVideo = clientApiRest.keyspace(WORKING_KEYSPACE).table(WORKING_TABLE);
        Assertions.assertTrue(tableVideo.exist());
        
        // Empty table, delete per partition
        tableVideo.key("Sci-Fi").delete();
        tableVideo.key("genre1").delete();
        tableVideo.key("genre2").delete();
        
        // 3 rows with genre1 1990
        Map<String, Object> data = new HashMap<>();
        data.put("genre", "genre1");
        data.put("year", 1990);
        data.put("title", "line1");tableVideo.upsert(data);
        data.put("title", "line2");tableVideo.upsert(data);
        data.put("title", "line3");tableVideo.upsert(data);
        // 3 rows with Search 1991
        data.put("year", 1991);
        data.put("title", "line4");tableVideo.upsert(data);
        data.put("title", "line5");tableVideo.upsert(data);
        data.put("title", "line6");tableVideo.upsert(data);
        // 3 rows with genre2 1990
        data.put("genre", "genre2");
        data.put("title", "line7");tableVideo.upsert(data);
        data.put("title", "line8");tableVideo.upsert(data);
        data.put("title", "line9");tableVideo.upsert(data);
        
        // Search 
        RowResultPage res1 = tableVideo.search(
                SearchTableQuery.builder()
                          .where("genre").isEqualsTo("genre1")
                          .withReturnedFields("title", "year")
                          .build());
        Assertions.assertEquals(6, res1.getResults().size());
       
        RowResultPage res2 = tableVideo.search(
                SearchTableQuery.builder()
                          .where("genre").isEqualsTo("genre2")
                          .withReturnedFields("title", "year")
                          .build());
        Assertions.assertEquals(3, res2.getResults().size());
        
        // This req would need allow filtering
        RowResultPage res3 = tableVideo.search(SearchTableQuery.builder()
               .select("title", "year")
               .where("genre").isEqualsTo("genre1")
               .where("year").isGreaterThan(1989)
               .sortBy(new SortField("year", Ordering.ASC))
               .build());
        
        Assertions.assertEquals(3, res3.getResults().size());
        
    }
    
   
}
