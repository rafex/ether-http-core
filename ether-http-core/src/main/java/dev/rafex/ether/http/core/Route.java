package dev.rafex.ether.http.core;

/*-
 * #%L
 * ether-http-core
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record Route(String pattern, Set<String> allowedMethods) {

    public Route {
        allowedMethods = normalizeMethods(allowedMethods);
    }

    public static Route of(final String pattern, final Set<String> methods) {
        return new Route(pattern, methods);
    }

    public boolean allows(final String method) {
        return allowedMethods.contains(method.toUpperCase());
    }

    public Optional<Map<String, String>> match(final String relPath) {
        if ("/**".equals(pattern)) {
            return Optional.of(Map.of());
        }

        final var patternSegments = split(pattern);
        final var pathSegments = split(relPath);
        if (patternSegments.size() != pathSegments.size()) {
            return Optional.empty();
        }

        final var params = new LinkedHashMap<String, String>();
        for (int i = 0; i < patternSegments.size(); i++) {
            final var expected = patternSegments.get(i);
            final var actual = pathSegments.get(i);

            if (expected.startsWith("{") && expected.endsWith("}")) {
                final var key = expected.substring(1, expected.length() - 1);
                params.put(key, actual);
                continue;
            }
            if (!expected.equals(actual)) {
                return Optional.empty();
            }
        }

        return Optional.of(params);
    }

    private static List<String> split(final String path) {
        final var cleaned = path.startsWith("/") ? path.substring(1) : path;
        if (cleaned.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(cleaned.split("/"));
    }

    private static Set<String> normalizeMethods(final Set<String> methods) {
        final var out = new LinkedHashSet<String>();
        if (methods != null) {
            for (final var method : methods) {
                if (method != null && !method.isBlank()) {
                    out.add(method.trim().toUpperCase());
                }
            }
        }
        return Set.copyOf(out);
    }
}
