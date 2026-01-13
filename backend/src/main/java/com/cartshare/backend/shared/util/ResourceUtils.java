package com.cartshare.backend.shared.util;

import java.io.InputStream;

public final class ResourceUtils {
    private ResourceUtils() {}

    public static InputStream getResourceStream(String resourcePath) {
        InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return is;
    }
}
