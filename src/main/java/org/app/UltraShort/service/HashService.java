package org.app.UltraShort.service;

import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HashService {

    public static String generateHash(String url) {
        int hash = Hashing.murmur3_32_fixed()
                .hashString(url, StandardCharsets.UTF_8)
                .asInt();

        return Integer.toUnsignedString(hash);
    }
}
