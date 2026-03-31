package dev.rafex.ether.http.core.builtin;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.http.core.HttpExchange;

class BuiltinResourcesTest {

    @Test
    void helloResourceShouldReplyWithHelloPayload() {
        final var exchange = new RecordingExchange();

        final var handled = new HelloResource().get(exchange);

        assertTrue(handled);
        assertEquals(200, exchange.status);
        assertEquals(Map.of("message", "hello", "service", "ether"), exchange.jsonBody);
    }

    @Test
    void healthResourceShouldReplyWithUpPayload() {
        final var exchange = new RecordingExchange();

        final var handled = new HealthResource().get(exchange);

        assertTrue(handled);
        assertEquals(200, exchange.status);
        assertEquals(Map.of("status", "UP", "service", "ether"), exchange.jsonBody);
    }

    private static final class RecordingExchange implements HttpExchange {

        private int status;
        private Object jsonBody;

        @Override
        public String method() {
            return "GET";
        }

        @Override
        public String path() {
            return "/";
        }

        @Override
        public String pathParam(final String name) {
            return null;
        }

        @Override
        public String queryFirst(final String name) {
            return null;
        }

        @Override
        public List<String> queryAll(final String name) {
            return List.of();
        }

        @Override
        public Map<String, String> pathParams() {
            return Map.of();
        }

        @Override
        public Map<String, List<String>> queryParams() {
            return Map.of();
        }

        @Override
        public Set<String> allowedMethods() {
            return Set.of("GET");
        }

        @Override
        public void json(final int status, final Object body) {
            this.status = status;
            this.jsonBody = body;
        }

        @Override
        public void text(final int status, final String body) {
            this.status = status;
            this.jsonBody = body;
        }

        @Override
        public void noContent(final int status) {
            this.status = status;
            this.jsonBody = null;
        }
    }
}
