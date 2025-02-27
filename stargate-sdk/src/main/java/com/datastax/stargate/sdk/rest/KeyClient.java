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

package com.datastax.stargate.sdk.rest;

import static com.datastax.stargate.sdk.core.ApiSupport.getHttpClient;
import static com.datastax.stargate.sdk.core.ApiSupport.getObjectMapper;
import static com.datastax.stargate.sdk.core.ApiSupport.handleError;
import static com.datastax.stargate.sdk.core.ApiSupport.startRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.datastax.stargate.sdk.core.ApiResponse;
import com.datastax.stargate.sdk.core.ResultPage;
import com.datastax.stargate.sdk.rest.domain.QueryWithKey;
import com.datastax.stargate.sdk.rest.domain.Row;
import com.datastax.stargate.sdk.rest.domain.RowMapper;
import com.datastax.stargate.sdk.rest.domain.RowResultPage;
import com.datastax.stargate.sdk.rest.domain.SortField;
import com.datastax.stargate.sdk.utils.Assert;
import com.datastax.stargate.sdk.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Operation on a record.
 * 
 * @author Cedrick LUNVEN (@clunven)
 */
public class KeyClient {
    
    /** Collection name. */
    private final TableClient tableClient;
    
    /** Search PK. */
    private final List< Object> key;
    
    /** Current auth token. */
    private final String token;
    
    /**
     * Full constructor.
     * 
     * @param token String
     * @param tableClient TableClient
     * @param keys Object
     */
    public KeyClient(String token, TableClient tableClient, Object... keys) {
        this.token          = token;
        this.tableClient    = tableClient;
        this.key            = new ArrayList<>(Arrays.asList(keys));
    }
    
    /**
     * Build endpoint of this resource
     * 
     * @return String
     */
    private String getEndPointCurrentKey() {
        StringBuilder sbUrl = new StringBuilder(tableClient.getEndPointTable());
        Assert.notNull(key, "key");
        Assert.isTrue(!key.isEmpty(), "key");
        try {
            for(Object pk : key) {
                sbUrl.append("/" +  URLEncoder.encode(pk.toString(), StandardCharsets.UTF_8.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot enode URL", e);
        }
        return sbUrl.toString();
    }
    
    /**
     * Retrieve a set of Rows from Primary key value.
     *
     * @param query QueryWithKey
     * @return RowResultPage
     */
    // GET
    public RowResultPage find(QueryWithKey query) {
        Objects.requireNonNull(query);
        HttpResponse<String> response;
        try {
             // Invoke as JSON
            response = getHttpClient().send(
                    startRequest(buildQueryUrl(query), 
                            token).GET().build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Cannot search for Rows ", e);
        }   
        
        handleError(response);
         
        try {
             ApiResponse<List<LinkedHashMap<String,?>>> result = getObjectMapper()
                     .readValue(response.body(), 
                             new TypeReference<ApiResponse<List<LinkedHashMap<String,?>>>>(){});
             return new RowResultPage(query.getPageSize(), result.getPageState(), result.getData()
                    .stream()
                    .map(map -> {
                        Row r = new Row();
                        for (Entry<String, ?> val: map.entrySet()) {
                            r.put(val.getKey(), val.getValue());
                        }
                        return r;
                    })
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot marshall document results", e);
        }
    }
    
    /**
     * Retrieve a set of Rows from Primary key value.
     * 
     * @param <T> T
     * @param query QueryWithKey
     * @param mapper RowMapper
     * @return ResultPage
     */
    public <T> ResultPage<T> find(QueryWithKey query, RowMapper<T> mapper) {
        RowResultPage rrp = find(query);
        return new ResultPage<T>(rrp.getPageSize(), 
                rrp.getPageState().orElse(null),
                rrp.getResults().stream()
                   .map(mapper::map)
                   .collect(Collectors.toList()));
    }
    
    // DELETE
    public void delete() {
        HttpResponse<String> response;
        try {
            response = getHttpClient()
                    .send(startRequest(getEndPointCurrentKey(), token)
                           .DELETE().build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Cannot search for Rows ", e);
        }
        
        handleError(response);
    }
    
    /**
     * update
     * 
     * @param newRecord Map
     */
    public void update(Map<String, Object> newRecord) {
        HttpResponse<String> response;
        try {
            String reqBody = getObjectMapper().writeValueAsString(newRecord);
            response = getHttpClient().send(
                        startRequest(getEndPointCurrentKey(), token)
                            .method("PATCH", BodyPublishers.ofString(reqBody)).build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Cannot search for Rows ", e);
        }
        
        handleError(response);
    }
    
    /**
     * replace
     * 
     * @param newRecord Map
     */
    public void replace(Map<String, Object> newRecord) {
       HttpResponse<String> response;
        try {
            String reqBody = getObjectMapper().writeValueAsString(newRecord);
            response = getHttpClient().send(
                    startRequest(getEndPointCurrentKey(), token)
                    .PUT(BodyPublishers.ofString(reqBody)).build(),
                    BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Cannot search for Rows ", e);
        }
        handleError(response);
    }
    
    
    /**
     * Build complex URL as expected with primaryKey.
     * 
     * @param query QueryWithKey
     * @return String
     */
    private String buildQueryUrl(QueryWithKey query) {
        try {
            StringBuilder sbUrl = new StringBuilder(getEndPointCurrentKey());
            // Add query Params
            sbUrl.append("?page-size=" + query.getPageSize());
            // Depending on query you forge your URL
            if (query.getPageState().isPresent()) {
                sbUrl.append("&page-state=" + 
                        URLEncoder.encode(query.getPageState().get(), StandardCharsets.UTF_8.toString()));
            }
            // Fields to retrieve
            if (null != query.getFieldsToRetrieve() && !query.getFieldsToRetrieve().isEmpty()) {
                sbUrl.append("&fields=" + URLEncoder.encode(JsonUtils.collectionAsJson(query.getFieldsToRetrieve()), StandardCharsets.UTF_8.toString()));
            }
            // Fields to sort on 
            if (null != query.getFieldsToSort() && !query.getFieldsToSort().isEmpty()) {
                Map<String, String> sortFields = new LinkedHashMap<>();
                for (SortField sf : query.getFieldsToSort()) {
                    sortFields.put(sf.getFieldName(), sf.getOrder().name());
                }
                sbUrl.append("&sort=" + URLEncoder.encode(JsonUtils.mapAsJson(sortFields), StandardCharsets.UTF_8.toString()));
            }
            return sbUrl.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot enode URL", e);
        }
    }
}
