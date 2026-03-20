package dev.rafex.ether.http.core.query;

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

import java.util.ArrayList;
import java.util.List;

public final class RsqlParser {

    public RsqlNode parse(final String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        final var state = new State(input);
        final var node = parseOr(state);
        state.skipWs();
        if (!state.eof()) {
            throw new IllegalArgumentException("invalid rsql near position " + state.pos);
        }
        return node;
    }

    private RsqlNode parseOr(final State state) {
        final var nodes = new ArrayList<RsqlNode>();
        nodes.add(parseAnd(state));
        state.skipWs();
        while (state.peek(',')) {
            state.read();
            nodes.add(parseAnd(state));
            state.skipWs();
        }
        return nodes.size() == 1 ? nodes.get(0) : new RsqlNode.Or(nodes);
    }

    private RsqlNode parseAnd(final State state) {
        final var nodes = new ArrayList<RsqlNode>();
        nodes.add(parseTerm(state));
        state.skipWs();
        while (state.peek(';')) {
            state.read();
            nodes.add(parseTerm(state));
            state.skipWs();
        }
        return nodes.size() == 1 ? nodes.get(0) : new RsqlNode.And(nodes);
    }

    private RsqlNode parseTerm(final State state) {
        state.skipWs();
        if (state.peek('(')) {
            state.read();
            final var node = parseOr(state);
            state.skipWs();
            state.expect(')');
            return node;
        }
        return parseComparison(state);
    }

    private RsqlNode parseComparison(final State state) {
        final var selector = parseSelector(state);
        final var op = parseOperator(state);
        final List<String> args;
        if (op == RsqlOperator.IN || op == RsqlOperator.OUT) {
            args = parseListArguments(state);
        } else {
            args = List.of(parseValue(state));
        }
        if (args.isEmpty()) {
            throw new IllegalArgumentException("operator requires arguments");
        }
        return new RsqlNode.Comp(selector, op, args);
    }

    private String parseSelector(final State state) {
        state.skipWs();
        final int start = state.pos;
        while (!state.eof()) {
            final char c = state.ch();
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                state.read();
                continue;
            }
            break;
        }
        if (state.pos == start) {
            throw new IllegalArgumentException("missing selector at position " + start);
        }
        return state.input.substring(start, state.pos);
    }

    private RsqlOperator parseOperator(final State state) {
        state.skipWs();
        if (state.match("==")) {
            return RsqlOperator.EQ;
        }
        if (state.match("!=")) {
            return RsqlOperator.NEQ;
        }
        if (state.match("=in=")) {
            return RsqlOperator.IN;
        }
        if (state.match("=out=")) {
            return RsqlOperator.OUT;
        }
        if (state.match("=like=")) {
            return RsqlOperator.LIKE;
        }
        throw new IllegalArgumentException("unsupported operator at position " + state.pos);
    }

    private List<String> parseListArguments(final State state) {
        state.skipWs();
        state.expect('(');
        final var args = new ArrayList<String>();
        while (true) {
            state.skipWs();
            if (state.peek(')')) {
                state.read();
                break;
            }
            args.add(parseValue(state));
            state.skipWs();
            if (state.peek(',')) {
                state.read();
                continue;
            }
            state.expect(')');
            break;
        }
        return args;
    }

    private String parseValue(final State state) {
        state.skipWs();
        if (state.peek('"')) {
            return parseQuoted(state);
        }
        final int start = state.pos;
        while (!state.eof()) {
            final char c = state.ch();
            if (c == ';' || c == ',' || c == ')') {
                break;
            }
            if (Character.isWhitespace(c)) {
                break;
            }
            state.read();
        }
        if (state.pos == start) {
            throw new IllegalArgumentException("missing value at position " + start);
        }
        return state.input.substring(start, state.pos);
    }

    private String parseQuoted(final State state) {
        state.expect('"');
        final var out = new StringBuilder();
        while (!state.eof()) {
            final char c = state.read();
            if (c == '\\') {
                if (state.eof()) {
                    throw new IllegalArgumentException("unterminated escape sequence");
                }
                out.append(state.read());
                continue;
            }
            if (c == '"') {
                return out.toString();
            }
            out.append(c);
        }
        throw new IllegalArgumentException("unterminated quoted value");
    }

    private static final class State {
        private final String input;
        private int pos;

        private State(final String input) {
            this.input = input;
            this.pos = 0;
        }

        private boolean eof() {
            return pos >= input.length();
        }

        private char ch() {
            return input.charAt(pos);
        }

        private char read() {
            return input.charAt(pos++);
        }

        private boolean peek(final char c) {
            return !eof() && ch() == c;
        }

        private void expect(final char c) {
            if (!peek(c)) {
                throw new IllegalArgumentException("expected '" + c + "' at position " + pos);
            }
            read();
        }

        private boolean match(final String s) {
            if (input.regionMatches(pos, s, 0, s.length())) {
                pos += s.length();
                return true;
            }
            return false;
        }

        private void skipWs() {
            while (!eof() && Character.isWhitespace(ch())) {
                read();
            }
        }
    }
}
