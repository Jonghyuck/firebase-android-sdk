// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.firestore;

import static com.google.firebase.firestore.core.Query.LimitType.LIMIT_TO_LAST;
import static com.google.firebase.firestore.util.Assert.fail;
import static com.google.firebase.firestore.util.Assert.hardAssert;
import static com.google.firebase.firestore.util.Preconditions.checkNotNull;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;
import com.google.firebase.firestore.core.ActivityScope;
import com.google.firebase.firestore.core.AsyncEventListener;
import com.google.firebase.firestore.core.Bound;
import com.google.firebase.firestore.core.CompositeFilter;
import com.google.firebase.firestore.core.EventManager.ListenOptions;
import com.google.firebase.firestore.core.FieldFilter;
import com.google.firebase.firestore.core.FieldFilter.Operator;
import com.google.firebase.firestore.core.ListenerRegistrationImpl;
import com.google.firebase.firestore.core.OrderBy;
import com.google.firebase.firestore.core.QueryListener;
import com.google.firebase.firestore.core.ViewSnapshot;
import com.google.firebase.firestore.model.Document;
import com.google.firebase.firestore.model.DocumentKey;
import com.google.firebase.firestore.model.ResourcePath;
import com.google.firebase.firestore.model.ServerTimestamps;
import com.google.firebase.firestore.model.Values;
import com.google.firebase.firestore.util.Executors;
import com.google.firebase.firestore.util.Util;
import com.google.firestore.v1.ArrayValue;
import com.google.firestore.v1.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * A {@code Query} which you can read or listen to. You can also construct refined {@code Query}
 * objects by adding filters and ordering.
 *
 * <p><b>Subclassing Note</b>: Cloud Firestore classes are not meant to be subclassed except for use
 * in test mocks. Subclassing is not supported in production code and new SDK releases may break
 * code that does so.
 */
public class Query {
  final com.google.firebase.firestore.core.Query query;

  final FirebaseFirestore firestore;

  /** An enum for the direction of a sort. */
  public enum Direction {
    ASCENDING,
    DESCENDING
  }

  Query(com.google.firebase.firestore.core.Query query, FirebaseFirestore firestore) {
    this.query = checkNotNull(query);
    this.firestore = checkNotNull(firestore);
  }

  /** Gets the Cloud Firestore instance associated with this query. */
  @NonNull
  public FirebaseFirestore getFirestore() {
    return firestore;
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be equal to the specified value.
   *
   * @param field The name of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereEqualTo(@NonNull String field, @Nullable Object value) {
    return where(Filter.equalTo(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be equal to the specified value.
   *
   * @param fieldPath The path of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereEqualTo(@NonNull FieldPath fieldPath, @Nullable Object value) {
    return where(Filter.equalTo(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value does not equal the specified value.
   *
   * <p>A {@code Query} can have only one {@code whereNotEqualTo()} filter, and it cannot be
   * combined with {@code whereNotIn()}.
   *
   * @param field The name of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereNotEqualTo(@NonNull String field, @Nullable Object value) {
    return where(Filter.notEqualTo(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value does not equal the specified value.
   *
   * <p>A {@code Query} can have only one {@code whereNotEqualTo()} filter, and it cannot be
   * combined with {@code whereNotIn()}.
   *
   * @param fieldPath The path of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereNotEqualTo(@NonNull FieldPath fieldPath, @Nullable Object value) {
    return where(Filter.notEqualTo(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be less than the specified value.
   *
   * @param field The name of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereLessThan(@NonNull String field, @NonNull Object value) {
    return where(Filter.lessThan(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be less than the specified value.
   *
   * @param fieldPath The path of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereLessThan(@NonNull FieldPath fieldPath, @NonNull Object value) {
    return where(Filter.lessThan(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be less than or equal to the specified value.
   *
   * @param field The name of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereLessThanOrEqualTo(@NonNull String field, @NonNull Object value) {
    return where(Filter.lessThanOrEqualTo(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be less than or equal to the specified value.
   *
   * @param fieldPath The path of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereLessThanOrEqualTo(@NonNull FieldPath fieldPath, @NonNull Object value) {
    return where(Filter.lessThanOrEqualTo(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be greater than the specified value.
   *
   * @param field The name of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereGreaterThan(@NonNull String field, @NonNull Object value) {
    return where(Filter.greaterThan(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be greater than the specified value.
   *
   * @param fieldPath The path of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereGreaterThan(@NonNull FieldPath fieldPath, @NonNull Object value) {
    return where(Filter.greaterThan(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be greater than or equal to the specified value.
   *
   * @param field The name of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereGreaterThanOrEqualTo(@NonNull String field, @NonNull Object value) {
    return where(Filter.greaterThanOrEqualTo(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value should be greater than or equal to the specified value.
   *
   * @param fieldPath The path of the field to compare
   * @param value The value for comparison
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereGreaterThanOrEqualTo(@NonNull FieldPath fieldPath, @NonNull Object value) {
    return where(Filter.greaterThanOrEqualTo(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field, the value must be an array, and that the array must contain the provided
   * value.
   *
   * <p>A {@code Query} can have only one {@code whereArrayContains()} filter and it cannot be
   * combined with {@code whereArrayContainsAny()}.
   *
   * @param field The name of the field containing an array to search.
   * @param value The value that must be contained in the array
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereArrayContains(@NonNull String field, @NonNull Object value) {
    return where(Filter.arrayContains(field, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field, the value must be an array, and that the array must contain the provided
   * value.
   *
   * <p>A {@code Query} can have only one {@code whereArrayContains()} filter and it cannot be
   * combined with {@code whereArrayContainsAny()}.
   *
   * @param fieldPath The path of the field containing an array to search.
   * @param value The value that must be contained in the array
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereArrayContains(@NonNull FieldPath fieldPath, @NonNull Object value) {
    return where(Filter.arrayContains(fieldPath, value));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field, the value must be an array, and that the array must contain at least one
   * value from the provided list.
   *
   * <p>A {@code Query} can have only one {@code whereArrayContainsAny()} filter and it cannot be
   * combined with {@code whereArrayContains()} or {@code whereIn()}.
   *
   * @param field The name of the field containing an array to search.
   * @param values The list that contains the values to match.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereArrayContainsAny(
      @NonNull String field, @NonNull List<? extends Object> values) {
    return where(Filter.arrayContainsAny(field, values));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field, the value must be an array, and that the array must contain at least one
   * value from the provided list.
   *
   * <p>A {@code Query} can have only one {@code whereArrayContainsAny()} filter and it cannot be
   * combined with {@code whereArrayContains()} or {@code whereIn()}.
   *
   * @param fieldPath The path of the field containing an array to search.
   * @param values The list that contains the values to match.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereArrayContainsAny(
      @NonNull FieldPath fieldPath, @NonNull List<? extends Object> values) {
    return where(Filter.arrayContainsAny(fieldPath, values));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value must equal one of the values from the provided list.
   *
   * <p>A {@code Query} can have only one {@code whereIn()} filter, and it cannot be combined with
   * {@code whereArrayContainsAny()}.
   *
   * @param field The name of the field to search.
   * @param values The list that contains the values to match.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereIn(@NonNull String field, @NonNull List<? extends Object> values) {
    return where(Filter.inArray(field, values));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value must equal one of the values from the provided list.
   *
   * <p>A {@code Query} can have only one {@code whereIn()} filter, and it cannot be combined with
   * {@code whereArrayContainsAny()}.
   *
   * @param fieldPath The path of the field to search.
   * @param values The list that contains the values to match.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereIn(@NonNull FieldPath fieldPath, @NonNull List<? extends Object> values) {
    return where(Filter.inArray(fieldPath, values));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value does not equal any of the values from the provided list.
   *
   * <p>One special case is that {@code whereNotIn} cannot match {@code null} values. To query for
   * documents where a field exists and is {@code null}, use {@code whereNotEqualTo}, which can
   * handle this special case.
   *
   * <p>A {@code Query} can have only one {@code whereNotIn()} filter, and it cannot be combined
   * with {@code whereArrayContains()}, {@code whereArrayContainsAny()}, {@code whereIn()}, or
   * {@code whereNotEqualTo()}.
   *
   * @param field The name of the field to search.
   * @param values The list that contains the values to match.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereNotIn(@NonNull String field, @NonNull List<? extends Object> values) {
    return where(Filter.notInArray(field, values));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter that documents must contain
   * the specified field and the value does not equal any of the values from the provided list.
   *
   * <p>One special case is that {@code whereNotIn} cannot match {@code null} values. To query for
   * documents where a field exists and is {@code null}, use {@code whereNotEqualTo}, which can
   * handle this special case.
   *
   * <p>A {@code Query} can have only one {@code whereNotIn()} filter, and it cannot be combined
   * with {@code whereArrayContains()}, {@code whereArrayContainsAny()}, {@code whereIn()}, or
   * {@code whereNotEqualTo()}.
   *
   * @param fieldPath The path of the field to search.
   * @param values The list that contains the values to match.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query whereNotIn(@NonNull FieldPath fieldPath, @NonNull List<? extends Object> values) {
    return where(Filter.notInArray(fieldPath, values));
  }

  /**
   * Creates and returns a new {@code Query} with the additional filter.
   *
   * @param filter The new filter to apply to the existing query.
   * @return The newly created {@code Query}.
   */
  @NonNull
  public Query where(@NonNull Filter filter) {
    com.google.firebase.firestore.core.Filter parsedFilter = parseFilter(filter);
    if (parsedFilter.getFilters().isEmpty()) {
      // Return the existing query if not adding any more filters (because composite filter is
      // empty).
      return this;
    }
    validateNewFilter(parsedFilter);
    return new Query(query.filter(parsedFilter), firestore);
  }

  /**
   * Takes a {@link Filter.UnaryFilter} object, parses the value object and returns a new {@link
   * FieldFilter} for the field, operator, and parsed value.
   *
   * @param fieldFilterData The Filter.UnaryFilter object to parse.
   * @return The created {@link FieldFilter}.
   */
  private FieldFilter parseFieldFilter(Filter.UnaryFilter fieldFilterData) {
    FieldPath fieldPath = fieldFilterData.getField();
    Operator op = fieldFilterData.getOperator();
    Object value = fieldFilterData.getValue();
    checkNotNull(fieldPath, "Provided field path must not be null.");
    checkNotNull(op, "Provided op must not be null.");
    Value fieldValue;
    com.google.firebase.firestore.model.FieldPath internalPath = fieldPath.getInternalPath();
    if (internalPath.isKeyField()) {
      if (op == Operator.ARRAY_CONTAINS || op == Operator.ARRAY_CONTAINS_ANY) {
        throw new IllegalArgumentException(
            "Invalid query. You can't perform '"
                + op.toString()
                + "' queries on FieldPath.documentId().");
      } else if (op == Operator.IN || op == Operator.NOT_IN) {
        validateDisjunctiveFilterElements(value, op);
        ArrayValue.Builder referenceList = ArrayValue.newBuilder();
        for (Object arrayValue : (List) value) {
          referenceList.addValues(parseDocumentIdValue(arrayValue));
        }
        fieldValue = Value.newBuilder().setArrayValue(referenceList).build();
      } else {
        fieldValue = parseDocumentIdValue(value);
      }
    } else {
      if (op == Operator.IN || op == Operator.NOT_IN || op == Operator.ARRAY_CONTAINS_ANY) {
        validateDisjunctiveFilterElements(value, op);
      }
      fieldValue =
          firestore
              .getUserDataReader()
              .parseQueryValue(value, op == Operator.IN || op == Operator.NOT_IN);
    }
    FieldFilter filter = FieldFilter.create(fieldPath.getInternalPath(), op, fieldValue);
    return filter;
  }

  /**
   * Takes a {@link Filter.CompositeFilter} object, parses each of its subfilters, and returns a new
   * {@link Filter} that is constructed using the parsed values.
   */
  private com.google.firebase.firestore.core.Filter parseCompositeFilter(
      Filter.CompositeFilter compositeFilterData) {
    List<com.google.firebase.firestore.core.Filter> parsedFilters = new ArrayList<>();
    for (Filter filter : compositeFilterData.getFilters()) {
      com.google.firebase.firestore.core.Filter parsedFilter = parseFilter(filter);
      if (!parsedFilter.getFilters().isEmpty()) {
        parsedFilters.add(parsedFilter);
      }
    }

    // For composite filters containing 1 filter, return the only filter.
    // For example: AND(FieldFilter1) == FieldFilter1
    if (parsedFilters.size() == 1) {
      return parsedFilters.get(0);
    }
    return new CompositeFilter(parsedFilters, compositeFilterData.getOperator());
  }

  /**
   * Takes a filter whose value has not been parsed, parses the value object and returns a
   * FieldFilter or CompositeFilter with parsed values.
   */
  private com.google.firebase.firestore.core.Filter parseFilter(Filter filter) {
    hardAssert(
        filter instanceof Filter.UnaryFilter || filter instanceof Filter.CompositeFilter,
        "Parsing is only supported for Filter.UnaryFilter and Filter.CompositeFilter.");
    if (filter instanceof Filter.UnaryFilter) {
      return parseFieldFilter((Filter.UnaryFilter) filter);
    }
    return parseCompositeFilter((Filter.CompositeFilter) filter);
  }

  /**
   * Parses the given documentIdValue into a ReferenceValue, throwing appropriate errors if the
   * value is anything other than a DocumentReference or String, or if the string is malformed.
   */
  private Value parseDocumentIdValue(Object documentIdValue) {
    if (documentIdValue instanceof String) {
      String documentId = (String) documentIdValue;
      if (documentId.isEmpty()) {
        throw new IllegalArgumentException(
            "Invalid query. When querying with FieldPath.documentId() you must provide a valid "
                + "document ID, but it was an empty string.");
      }
      if (!query.isCollectionGroupQuery() && documentId.contains("/")) {
        throw new IllegalArgumentException(
            "Invalid query. When querying a collection by FieldPath.documentId() you must "
                + "provide a plain document ID, but '"
                + documentId
                + "' contains a '/' character.");
      }
      ResourcePath path = query.getPath().append(ResourcePath.fromString(documentId));
      if (!DocumentKey.isDocumentKey(path)) {
        throw new IllegalArgumentException(
            "Invalid query. When querying a collection group by FieldPath.documentId(), the "
                + "value provided must result in a valid document path, but '"
                + path
                + "' is not because it has an odd number of segments ("
                + path.length()
                + ").");
      }
      return Values.refValue(this.getFirestore().getDatabaseId(), DocumentKey.fromPath(path));
    } else if (documentIdValue instanceof DocumentReference) {
      DocumentReference ref = (DocumentReference) documentIdValue;
      return Values.refValue(this.getFirestore().getDatabaseId(), ref.getKey());
    } else {
      throw new IllegalArgumentException(
          "Invalid query. When querying with FieldPath.documentId() you must provide a valid "
              + "String or DocumentReference, but it was of type: "
              + Util.typeName(documentIdValue));
    }
  }

  /** Validates that the value passed into a disjunctive filter satisfies all array requirements. */
  private void validateDisjunctiveFilterElements(Object value, Operator op) {
    if (!(value instanceof List) || ((List) value).size() == 0) {
      throw new IllegalArgumentException(
          "Invalid Query. A non-empty array is required for '" + op.toString() + "' filters.");
    }
  }

  /**
   * Given an operator, returns the set of operators that cannot be used with it.
   *
   * <p>This is not a comprehensive check, and this function should be removed in the long term.
   * Validations should occur in the Firestore backend.
   *
   * <p>Operators in a query must adhere to the following set of rules:
   *
   * <ol>
   *   <li>Only one inequality per query.
   *   <li>NOT_IN cannot be used with array, disjunctive, or NOT_EQUAL operators.
   * </ol>
   */
  private List<Operator> conflictingOps(Operator op) {
    switch (op) {
      case NOT_EQUAL:
        return Arrays.asList(Operator.NOT_EQUAL, Operator.NOT_IN);
      case ARRAY_CONTAINS_ANY:
      case IN:
        return Arrays.asList(Operator.NOT_IN);
      case NOT_IN:
        return Arrays.asList(
            Operator.ARRAY_CONTAINS_ANY, Operator.IN, Operator.NOT_IN, Operator.NOT_EQUAL);
      default:
        return new ArrayList<>();
    }
  }

  /** Checks that adding the given field filter to the given query yields a valid query */
  private void validateNewFieldFilter(
      com.google.firebase.firestore.core.Query query,
      com.google.firebase.firestore.core.FieldFilter fieldFilter) {
    Operator filterOp = fieldFilter.getOperator();

    Operator conflictingOp = findOpInsideFilters(query.getFilters(), conflictingOps(filterOp));
    if (conflictingOp != null) {
      // We special case when it's a duplicate op to give a slightly clearer error message.
      if (conflictingOp == filterOp) {
        throw new IllegalArgumentException(
            "Invalid Query. You cannot use more than one '" + filterOp.toString() + "' filter.");
      } else {
        throw new IllegalArgumentException(
            "Invalid Query. You cannot use '"
                + filterOp.toString()
                + "' filters with '"
                + conflictingOp.toString()
                + "' filters.");
      }
    }
  }

  /** Checks that adding the given filter to the current query is valid */
  private void validateNewFilter(com.google.firebase.firestore.core.Filter filter) {
    com.google.firebase.firestore.core.Query testQuery = query;
    for (FieldFilter subfilter : filter.getFlattenedFilters()) {
      validateNewFieldFilter(testQuery, subfilter);
      testQuery = testQuery.filter(subfilter);
    }
  }

  /**
   * Checks if any of the provided filter operators are included in the given list of filters and
   * returns the first one that is, or null if none are.
   */
  @Nullable
  private Operator findOpInsideFilters(
      List<com.google.firebase.firestore.core.Filter> filters, List<Operator> operators) {
    for (com.google.firebase.firestore.core.Filter filter : filters) {
      for (FieldFilter fieldFilter : filter.getFlattenedFilters()) {
        if (operators.contains(fieldFilter.getOperator())) {
          return fieldFilter.getOperator();
        }
      }
    }
    return null;
  }

  /**
   * Creates and returns a new {@code Query} that's additionally sorted by the specified field.
   *
   * @param field The field to sort by.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query orderBy(@NonNull String field) {
    return orderBy(FieldPath.fromDotSeparatedPath(field), Direction.ASCENDING);
  }

  /**
   * Creates and returns a new {@code Query} that's additionally sorted by the specified field.
   *
   * @param fieldPath The field to sort by.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query orderBy(@NonNull FieldPath fieldPath) {
    checkNotNull(fieldPath, "Provided field path must not be null.");
    return orderBy(fieldPath.getInternalPath(), Direction.ASCENDING);
  }

  /**
   * Creates and returns a new {@code Query} that's additionally sorted by the specified field,
   * optionally in descending order instead of ascending.
   *
   * @param field The field to sort by.
   * @param direction The direction to sort.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query orderBy(@NonNull String field, @NonNull Direction direction) {
    return orderBy(FieldPath.fromDotSeparatedPath(field), direction);
  }

  /**
   * Creates and returns a new {@code Query} that's additionally sorted by the specified field,
   * optionally in descending order instead of ascending.
   *
   * @param fieldPath The field to sort by.
   * @param direction The direction to sort.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query orderBy(@NonNull FieldPath fieldPath, @NonNull Direction direction) {
    checkNotNull(fieldPath, "Provided field path must not be null.");
    return orderBy(fieldPath.getInternalPath(), direction);
  }

  private Query orderBy(
      @NonNull com.google.firebase.firestore.model.FieldPath fieldPath,
      @NonNull Direction direction) {
    checkNotNull(direction, "Provided direction must not be null.");
    if (query.getStartAt() != null) {
      throw new IllegalArgumentException(
          "Invalid query. You must not call Query.startAt() or Query.startAfter() before "
              + "calling Query.orderBy().");
    }
    if (query.getEndAt() != null) {
      throw new IllegalArgumentException(
          "Invalid query. You must not call Query.endAt() or Query.endBefore() before "
              + "calling Query.orderBy().");
    }
    OrderBy.Direction dir =
        direction == Direction.ASCENDING
            ? OrderBy.Direction.ASCENDING
            : OrderBy.Direction.DESCENDING;
    return new Query(query.orderBy(OrderBy.getInstance(dir, fieldPath)), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that only returns the first matching documents up to
   * the specified number.
   *
   * @param limit The maximum number of items to return.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query limit(long limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException(
          "Invalid Query. Query limit (" + limit + ") is invalid. Limit must be positive.");
    }
    return new Query(query.limitToFirst(limit), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that only returns the last matching documents up to the
   * specified number.
   *
   * <p>You must specify at least one {@code orderBy} clause for {@code limitToLast} queries,
   * otherwise an exception will be thrown during execution.
   *
   * @param limit The maximum number of items to return.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query limitToLast(long limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException(
          "Invalid Query. Query limitToLast (" + limit + ") is invalid. Limit must be positive.");
    }
    return new Query(query.limitToLast(limit), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that starts at the provided document (inclusive). The
   * starting position is relative to the order of the query. The document must contain all of the
   * fields provided in the orderBy of this query.
   *
   * @param snapshot The snapshot of the document to start at.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query startAt(@NonNull DocumentSnapshot snapshot) {
    Bound bound = boundFromDocumentSnapshot("startAt", snapshot, /* inclusive= */ true);
    return new Query(query.startAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that starts at the provided fields relative to the
   * order of the query. The order of the field values must match the order of the order by clauses
   * of the query.
   *
   * @param fieldValues The field values to start this query at, in order of the query's order by.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query startAt(Object... fieldValues) {
    Bound bound = boundFromFields("startAt", fieldValues, /* inclusive= */ true);
    return new Query(query.startAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that starts after the provided document (exclusive).
   * The starting position is relative to the order of the query. The document must contain all of
   * the fields provided in the orderBy of this query.
   *
   * @param snapshot The snapshot of the document to start after.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query startAfter(@NonNull DocumentSnapshot snapshot) {
    Bound bound = boundFromDocumentSnapshot("startAfter", snapshot, /* inclusive= */ false);
    return new Query(query.startAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that starts after the provided fields relative to the
   * order of the query. The order of the field values must match the order of the order by clauses
   * of the query.
   *
   * @param fieldValues The field values to start this query after, in order of the query's order
   *     by.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query startAfter(Object... fieldValues) {
    Bound bound = boundFromFields("startAfter", fieldValues, /* inclusive= */ false);
    return new Query(query.startAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that ends before the provided document (exclusive). The
   * end position is relative to the order of the query. The document must contain all of the fields
   * provided in the orderBy of this query.
   *
   * @param snapshot The snapshot of the document to end before.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query endBefore(@NonNull DocumentSnapshot snapshot) {
    Bound bound = boundFromDocumentSnapshot("endBefore", snapshot, /* inclusive= */ false);
    return new Query(query.endAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that ends before the provided fields relative to the
   * order of the query. The order of the field values must match the order of the order by clauses
   * of the query.
   *
   * @param fieldValues The field values to end this query before, in order of the query's order by.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query endBefore(Object... fieldValues) {
    Bound bound = boundFromFields("endBefore", fieldValues, /* inclusive= */ false);
    return new Query(query.endAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that ends at the provided document (inclusive). The end
   * position is relative to the order of the query. The document must contain all of the fields
   * provided in the orderBy of this query.
   *
   * @param snapshot The snapshot of the document to end at.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query endAt(@NonNull DocumentSnapshot snapshot) {
    Bound bound = boundFromDocumentSnapshot("endAt", snapshot, /* inclusive= */ true);
    return new Query(query.endAt(bound), firestore);
  }

  /**
   * Creates and returns a new {@code Query} that ends at the provided fields relative to the order
   * of the query. The order of the field values must match the order of the order by clauses of the
   * query.
   *
   * @param fieldValues The field values to end this query at, in order of the query's order by.
   * @return The created {@code Query}.
   */
  @NonNull
  public Query endAt(Object... fieldValues) {
    Bound bound = boundFromFields("endAt", fieldValues, /* inclusive= */ true);
    return new Query(query.endAt(bound), firestore);
  }

  /**
   * Create a Bound from a query given the document.
   *
   * <p>Note that the Bound will always include the key of the document and so only the provided
   * document will compare equal to the returned position.
   *
   * <p>Will throw if the document does not contain all fields of the order by of the query or if
   * any of the fields in the order by are an uncommitted server timestamp.
   */
  private Bound boundFromDocumentSnapshot(
      String methodName, DocumentSnapshot snapshot, boolean inclusive) {
    checkNotNull(snapshot, "Provided snapshot must not be null.");
    if (!snapshot.exists()) {
      throw new IllegalArgumentException(
          "Can't use a DocumentSnapshot for a document that doesn't exist for "
              + methodName
              + "().");
    }
    Document document = snapshot.getDocument();
    List<Value> components = new ArrayList<>();

    // Because people expect to continue/end a query at the exact document provided, we need to
    // use the implicit sort order rather than the explicit sort order, because it's guaranteed to
    // contain the document key. That way the position becomes unambiguous and the query
    // continues/ends exactly at the provided document. Without the key (by using the explicit sort
    // orders), multiple documents could match the position, yielding duplicate results.
    for (OrderBy orderBy : query.getNormalizedOrderBy()) {
      if (orderBy.getField().equals(com.google.firebase.firestore.model.FieldPath.KEY_PATH)) {
        components.add(Values.refValue(firestore.getDatabaseId(), document.getKey()));
      } else {
        Value value = document.getField(orderBy.getField());
        if (ServerTimestamps.isServerTimestamp(value)) {
          throw new IllegalArgumentException(
              "Invalid query. You are trying to start or end a query using a document for which "
                  + "the field '"
                  + orderBy.getField()
                  + "' is an uncommitted server timestamp. (Since the value of this field is "
                  + "unknown, you cannot start/end a query with it.)");
        } else if (value != null) {
          components.add(value);
        } else {
          throw new IllegalArgumentException(
              "Invalid query. You are trying to start or end a query using a document for which "
                  + "the field '"
                  + orderBy.getField()
                  + "' (used as the orderBy) does not exist.");
        }
      }
    }
    return new Bound(components, inclusive);
  }

  /** Converts a list of field values to Bound. */
  private Bound boundFromFields(String methodName, Object[] values, boolean inclusive) {
    // Use explicit order by's because it has to match the query the user made
    List<OrderBy> explicitOrderBy = query.getExplicitOrderBy();
    if (values.length > explicitOrderBy.size()) {
      throw new IllegalArgumentException(
          "Too many arguments provided to "
              + methodName
              + "(). The number of arguments must be less "
              + "than or equal to the number of orderBy() clauses.");
    }

    List<Value> components = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
      Object rawValue = values[i];
      OrderBy orderBy = explicitOrderBy.get(i);
      if (orderBy.getField().equals(com.google.firebase.firestore.model.FieldPath.KEY_PATH)) {
        if (!(rawValue instanceof String)) {
          throw new IllegalArgumentException(
              "Invalid query. Expected a string for document ID in "
                  + methodName
                  + "(), but got "
                  + rawValue
                  + ".");
        }
        String documentId = (String) rawValue;
        if (!query.isCollectionGroupQuery() && documentId.contains("/")) {
          throw new IllegalArgumentException(
              "Invalid query. When querying a collection and ordering by FieldPath.documentId(), "
                  + "the value passed to "
                  + methodName
                  + "() must be a plain document ID, but '"
                  + documentId
                  + "' contains a slash.");
        }
        ResourcePath path = query.getPath().append(ResourcePath.fromString(documentId));
        if (!DocumentKey.isDocumentKey(path)) {
          throw new IllegalArgumentException(
              "Invalid query. When querying a collection group and ordering by "
                  + "FieldPath.documentId(), the value passed to "
                  + methodName
                  + "() must result in a valid document path, but '"
                  + path
                  + "' is not because it contains an odd number of segments.");
        }
        DocumentKey key = DocumentKey.fromPath(path);
        components.add(Values.refValue(firestore.getDatabaseId(), key));
      } else {
        Value wrapped = firestore.getUserDataReader().parseQueryValue(rawValue);
        components.add(wrapped);
      }
    }

    return new Bound(components, inclusive);
  }

  /**
   * Executes the query and returns the results as a {@code QuerySnapshot}.
   *
   * @return A Task that will be resolved with the results of the {@code Query}.
   */
  @NonNull
  public Task<QuerySnapshot> get() {
    return get(Source.DEFAULT);
  }

  /**
   * Executes the query and returns the results as a {@code QuerySnapshot}.
   *
   * <p>By default, {@code get()} attempts to provide up-to-date data when possible by waiting for
   * data from the server, but it may return cached data or fail if you are offline and the server
   * cannot be reached. This behavior can be altered via the {@code Source} parameter.
   *
   * @param source A value to configure the get behavior.
   * @return A Task that will be resolved with the results of the {@code Query}.
   */
  @NonNull
  public Task<QuerySnapshot> get(@NonNull Source source) {
    validateHasExplicitOrderByForLimitToLast();
    if (source == Source.CACHE) {
      return firestore
          .getClient()
          .getDocumentsFromLocalCache(query)
          .continueWith(
              Executors.DIRECT_EXECUTOR,
              (Task<ViewSnapshot> viewSnap) ->
                  new QuerySnapshot(new Query(query, firestore), viewSnap.getResult(), firestore));
    } else {
      return getViaSnapshotListener(source);
    }
  }

  private Task<QuerySnapshot> getViaSnapshotListener(Source source) {
    final TaskCompletionSource<QuerySnapshot> res = new TaskCompletionSource<>();
    final TaskCompletionSource<ListenerRegistration> registration = new TaskCompletionSource<>();

    ListenOptions options = new ListenOptions();
    options.includeDocumentMetadataChanges = true;
    options.includeQueryMetadataChanges = true;
    options.waitForSyncWhenOnline = true;

    ListenerRegistration listenerRegistration =
        addSnapshotListenerInternal(
            // No need to schedule, we just set the task result directly
            Executors.DIRECT_EXECUTOR,
            options,
            null,
            (snapshot, error) -> {
              if (error != null) {
                res.setException(error);
                return;
              }

              try {
                // This await should be very short; we're just forcing synchronization between this
                // block and the outer registration.setResult.
                ListenerRegistration actualRegistration = Tasks.await(registration.getTask());

                // Remove query first before passing event to user to avoid user actions affecting
                // the now stale query.
                actualRegistration.remove();

                if (snapshot.getMetadata().isFromCache() && source == Source.SERVER) {
                  res.setException(
                      new FirebaseFirestoreException(
                          "Failed to get documents from server. (However, these documents may "
                              + "exist in the local cache. Run again without setting source to "
                              + "SERVER to retrieve the cached documents.)",
                          Code.UNAVAILABLE));
                } else {
                  res.setResult(snapshot);
                }
              } catch (ExecutionException e) {
                throw fail(e, "Failed to register a listener for a query result");
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw fail(e, "Failed to register a listener for a query result");
              }
            });

    registration.setResult(listenerRegistration);

    return res.getTask();
  }

  /**
   * Starts listening to this query.
   *
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(@NonNull EventListener<QuerySnapshot> listener) {
    return addSnapshotListener(MetadataChanges.EXCLUDE, listener);
  }

  /**
   * Starts listening to this query.
   *
   * @param executor The executor to use to call the listener.
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(
      @NonNull Executor executor, @NonNull EventListener<QuerySnapshot> listener) {
    return addSnapshotListener(executor, MetadataChanges.EXCLUDE, listener);
  }

  /**
   * Starts listening to this query using an Activity-scoped listener.
   *
   * <p>The listener will be automatically removed during {@link Activity#onStop}.
   *
   * @param activity The activity to scope the listener to.
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(
      @NonNull Activity activity, @NonNull EventListener<QuerySnapshot> listener) {
    return addSnapshotListener(activity, MetadataChanges.EXCLUDE, listener);
  }

  /**
   * Starts listening to this query with the given options.
   *
   * @param metadataChanges Indicates whether metadata-only changes (specifically, only {@code
   *     QuerySnapshot.getMetadata()} changed) should trigger snapshot events.
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(
      @NonNull MetadataChanges metadataChanges, @NonNull EventListener<QuerySnapshot> listener) {
    return addSnapshotListener(Executors.DEFAULT_CALLBACK_EXECUTOR, metadataChanges, listener);
  }

  /**
   * Starts listening to this query with the given options.
   *
   * @param executor The executor to use to call the listener.
   * @param metadataChanges Indicates whether metadata-only changes (specifically, only {@code
   *     QuerySnapshot.getMetadata()} changed) should trigger snapshot events.
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(
      @NonNull Executor executor,
      @NonNull MetadataChanges metadataChanges,
      @NonNull EventListener<QuerySnapshot> listener) {
    checkNotNull(executor, "Provided executor must not be null.");
    checkNotNull(metadataChanges, "Provided MetadataChanges value must not be null.");
    checkNotNull(listener, "Provided EventListener must not be null.");
    return addSnapshotListenerInternal(executor, internalOptions(metadataChanges), null, listener);
  }

  /**
   * Starts listening to this query with the given options, using an Activity-scoped listener.
   *
   * <p>The listener will be automatically removed during {@link Activity#onStop}.
   *
   * @param activity The activity to scope the listener to.
   * @param metadataChanges Indicates whether metadata-only changes (specifically, only {@code
   *     QuerySnapshot.getMetadata()} changed) should trigger snapshot events.
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(
      @NonNull Activity activity,
      @NonNull MetadataChanges metadataChanges,
      @NonNull EventListener<QuerySnapshot> listener) {
    checkNotNull(activity, "Provided activity must not be null.");
    checkNotNull(metadataChanges, "Provided MetadataChanges value must not be null.");
    checkNotNull(listener, "Provided EventListener must not be null.");
    return addSnapshotListenerInternal(
        Executors.DEFAULT_CALLBACK_EXECUTOR, internalOptions(metadataChanges), activity, listener);
  }

  /**
   * Starts listening to this query with the given options.
   *
   * @param options Sets snapshot listener options, including whether metadata-only changes should
   *     trigger snapshot events, the source to listen to, the executor to use to call the listener,
   *     or the activity to scope the listener to.
   * @param listener The event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  @NonNull
  public ListenerRegistration addSnapshotListener(
      @NonNull SnapshotListenOptions options, @NonNull EventListener<QuerySnapshot> listener) {
    checkNotNull(options, "Provided options value must not be null.");
    checkNotNull(listener, "Provided EventListener must not be null.");
    return addSnapshotListenerInternal(
        options.getExecutor(),
        internalOptions(options.getMetadataChanges(), options.getSource()),
        options.getActivity(),
        listener);
  }

  /**
   * Internal helper method to create add a snapshot listener.
   *
   * <p>Will be Activity scoped if the activity parameter is non-{@code null}.
   *
   * @param executor The executor to use to call the listener.
   * @param options The options to use for this listen.
   * @param activity Optional activity this listener is scoped to.
   * @param userListener The user-supplied event listener that will be called with the snapshots.
   * @return A registration object that can be used to remove the listener.
   */
  private ListenerRegistration addSnapshotListenerInternal(
      Executor executor,
      ListenOptions options,
      @Nullable Activity activity,
      EventListener<QuerySnapshot> userListener) {
    validateHasExplicitOrderByForLimitToLast();

    // Convert from ViewSnapshots to QuerySnapshots.
    EventListener<ViewSnapshot> viewListener =
        (@Nullable ViewSnapshot snapshot, @Nullable FirebaseFirestoreException error) -> {
          if (error != null) {
            userListener.onEvent(null, error);
            return;
          }

          hardAssert(snapshot != null, "Got event without value or error set");

          QuerySnapshot querySnapshot = new QuerySnapshot(this, snapshot, firestore);
          userListener.onEvent(querySnapshot, null);
        };

    // Call the viewListener on the userExecutor.
    AsyncEventListener<ViewSnapshot> asyncListener =
        new AsyncEventListener<>(executor, viewListener);

    QueryListener queryListener = firestore.getClient().listen(query, options, asyncListener);
    return ActivityScope.bind(
        activity,
        new ListenerRegistrationImpl(firestore.getClient(), queryListener, asyncListener));
  }

  private void validateHasExplicitOrderByForLimitToLast() {
    if (query.getLimitType().equals(LIMIT_TO_LAST) && query.getExplicitOrderBy().isEmpty()) {
      throw new IllegalStateException(
          "limitToLast() queries require specifying at least one orderBy() clause");
    }
  }

  /**
   * Returns a query that counts the documents in the result set of this query.
   *
   * <p>The returned query, when executed, counts the documents in the result set of this query
   * <em>without actually downloading the documents</em>.
   *
   * <p>Using the returned query to count the documents is efficient because only the final count,
   * not the documents' data, is downloaded. The returned query can count the documents in cases
   * where the result set is prohibitively large to download entirely (thousands of documents).
   *
   * @return The {@code AggregateQuery} that counts the documents in the result set of this query.
   */
  @NonNull
  public AggregateQuery count() {
    return new AggregateQuery(this, Collections.singletonList(AggregateField.count()));
  }

  /**
   * Calculates the specified aggregations over the documents in the result set of the given query
   * without actually downloading the documents.
   *
   * <p>Using the returned query to perform aggregations is efficient because only the final
   * aggregation values, not the documents' data, is downloaded. The returned query can perform
   * aggregations of the documents in cases where the result set is prohibitively large to download
   * entirely (thousands of documents).
   *
   * @return The {@code AggregateQuery} that performs aggregations on the documents in the result
   *     set of this query.
   */
  @NonNull
  public AggregateQuery aggregate(
      @NonNull AggregateField aggregateField, @NonNull AggregateField... aggregateFields) {
    List<AggregateField> fields =
        new ArrayList<AggregateField>() {
          {
            add(aggregateField);
          }
        };
    fields.addAll(Arrays.asList(aggregateFields));
    return new AggregateQuery(this, fields);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Query)) {
      return false;
    }

    Query that = (Query) o;

    return query.equals(that.query) && firestore.equals(that.firestore);
  }

  @Override
  public int hashCode() {
    int result = query.hashCode();
    result = 31 * result + firestore.hashCode();
    return result;
  }

  /** Converts the public API options object to the internal options object. */
  private static ListenOptions internalOptions(MetadataChanges metadataChanges) {
    return internalOptions(metadataChanges, ListenSource.DEFAULT);
  }

  private static ListenOptions internalOptions(
      MetadataChanges metadataChanges, ListenSource source) {
    ListenOptions internalOptions = new ListenOptions();
    internalOptions.includeDocumentMetadataChanges = (metadataChanges == MetadataChanges.INCLUDE);
    internalOptions.includeQueryMetadataChanges = (metadataChanges == MetadataChanges.INCLUDE);
    internalOptions.waitForSyncWhenOnline = false;
    internalOptions.source = source;
    return internalOptions;
  }
}
