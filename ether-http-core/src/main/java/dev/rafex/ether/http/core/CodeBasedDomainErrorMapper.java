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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CodeBasedDomainErrorMapper implements DomainErrorMapper {

    private record Key(String operation, String code) {
    }

    private final Map<Key, HttpError> rules;
    private final HttpError fallback;

    private CodeBasedDomainErrorMapper(final Map<Key, HttpError> rules, final HttpError fallback) {
        this.rules = Map.copyOf(rules);
        this.fallback = fallback;
    }

    @Override
    public HttpError map(final String operation, final String code, final Throwable cause) {
        final var byOperation = rules.get(new Key(normalize(operation), normalize(code)));
        if (byOperation != null) {
            return byOperation;
        }
        final var byWildcardOp = rules.get(new Key("*", normalize(code)));
        if (byWildcardOp != null) {
            return byWildcardOp;
        }
        return fallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String normalize(final String value) {
        return value == null || value.isBlank() ? "*" : value.trim();
    }

    public static final class Builder {

        private final Map<Key, HttpError> rules = new HashMap<>();
        private HttpError fallback = new HttpError(400, "domain_error", "domain error");

        public Builder rule(final String operation, final String code, final int status, final String error,
                final String message) {
            final var key = new Key(normalize(operation), normalize(code));
            rules.put(key, new HttpError(status, error, message));
            return this;
        }

        public Builder fallback(final int status, final String error, final String message) {
            fallback = new HttpError(status, error, message);
            return this;
        }

        public CodeBasedDomainErrorMapper build() {
            return new CodeBasedDomainErrorMapper(rules, Objects.requireNonNull(fallback));
        }
    }
}
