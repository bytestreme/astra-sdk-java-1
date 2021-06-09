package com.datastax.astra.sdk.utils;

import java.util.UUID;

public class IdUtils {

    /**
     * Check if it is uuid.
     *
     * @param uuid
     *      identiique identifier
     * @return
     *      check if this is uuid
     */
    public static boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
        } catch(IllegalArgumentException ieox) {
            return false;
        }
        return true;
    }
}
