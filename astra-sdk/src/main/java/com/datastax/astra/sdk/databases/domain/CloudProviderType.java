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

package com.datastax.astra.sdk.databases.domain;

/**
 * Encoded all values for 'cloudProvider'
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public enum CloudProviderType {
    /** */
    ALL("ALL"), 
    /** */
    GCP("GCP"), 
    /** */
    GCP_MARKETPLACE("GCP_MARKETPLACE"), 
    /** */
    AZURE("AZURE"), 
    /** */
    AWS("AWS");
    /** */
    private String code;
    
    /**
     * 
     */
    private CloudProviderType(String code) {
        this.code = code;
    }
    
    /**
     * 
     */
    public String getCode() {
        return code;
    }
    
}
