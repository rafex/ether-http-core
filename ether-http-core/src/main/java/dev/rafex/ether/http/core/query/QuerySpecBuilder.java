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

public final class QuerySpecBuilder {

	private static final int MIN_LIMIT = 1;
	private static final int MAX_LIMIT = 200;
	private static final int MIN_OFFSET = 0;
	private static final int MAX_OFFSET = 100_000;

	private final RsqlParser parser = new RsqlParser();

	public QuerySpec fromRawParams(final String q, final String tags, final String locationId, final String enabled,
			final String sort, final String limit, final String offset) {

		final RsqlNode rsqlFilter = parser.parse(trimToNull(q));
		final RsqlNode queryFilter = buildClassicFilter(tags, locationId, enabled);
		final RsqlNode finalFilter = mergeWithAnd(rsqlFilter, queryFilter);

		final int finalLimit = parseIntClamp(limit, QuerySpec.DEFAULT_LIMIT, MIN_LIMIT, MAX_LIMIT, "limit");
		final int finalOffset = parseIntClamp(offset, QuerySpec.DEFAULT_OFFSET, MIN_OFFSET, MAX_OFFSET, "offset");
		final var sorts = parseSort(sort);

		return new QuerySpec(finalFilter, finalLimit, finalOffset, sorts);
	}

	private RsqlNode buildClassicFilter(final String tagsRaw, final String locationIdRaw, final String enabledRaw) {
		final var filters = new ArrayList<RsqlNode>();

		final var tags = parseCsv(tagsRaw);
		if (!tags.isEmpty()) {
			filters.add(new RsqlNode.Comp("tags", RsqlOperator.IN, tags));
		}

		final var locationId = trimToNull(locationIdRaw);
		if (locationId != null) {
			filters.add(new RsqlNode.Comp("locationId", RsqlOperator.EQ, List.of(locationId)));
		}

		final var enabled = trimToNull(enabledRaw);
		if (enabled != null) {
			final String normalized;
			if ("true".equalsIgnoreCase(enabled) || "false".equalsIgnoreCase(enabled)) {
				normalized = enabled.toLowerCase();
			} else {
				throw new IllegalArgumentException("enabled must be true or false");
			}
			filters.add(new RsqlNode.Comp("enabled", RsqlOperator.EQ, List.of(normalized)));
		}

		if (filters.isEmpty()) {
			return null;
		}
		return filters.size() == 1 ? filters.get(0) : new RsqlNode.And(filters);
	}

	private List<Sort> parseSort(final String sortRaw) {
		final var sort = trimToNull(sortRaw);
		if (sort == null) {
			return List.of();
		}

		final var out = new ArrayList<Sort>();
		for (final String tokenRaw : sort.split(",")) {
			final var token = tokenRaw.trim();
			if (token.isEmpty()) {
				continue;
			}
			if (token.startsWith("-")) {
				if (token.length() == 1) {
					throw new IllegalArgumentException("invalid sort field");
				}
				out.add(new Sort(token.substring(1), Sort.Direction.DESC));
			} else {
				out.add(new Sort(token, Sort.Direction.ASC));
			}
		}
		return out;
	}

	private static List<String> parseCsv(final String raw) {
		final var value = trimToNull(raw);
		if (value == null) {
			return List.of();
		}
		final var out = new ArrayList<String>();
		for (final String tokenRaw : value.split(",")) {
			final var token = tokenRaw.trim();
			if (!token.isEmpty()) {
				out.add(token);
			}
		}
		return out;
	}

	private static int parseIntClamp(final String valueRaw, final int defaultValue, final int min, final int max,
			final String fieldName) {
		final var value = trimToNull(valueRaw);
		if (value == null) {
			return defaultValue;
		}
		try {
			final var parsed = Integer.parseInt(value);
			if (parsed < min) {
				return min;
			}
			if (parsed > max) {
				return max;
			}
			return parsed;
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(fieldName + " must be an integer");
		}
	}

	private static RsqlNode mergeWithAnd(final RsqlNode left, final RsqlNode right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		return new RsqlNode.And(List.of(left, right));
	}

	private static String trimToNull(final String value) {
		if (value == null) {
			return null;
		}
		final var trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
