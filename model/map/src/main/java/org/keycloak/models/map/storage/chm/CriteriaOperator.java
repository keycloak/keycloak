/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
class CriteriaOperator {

    private static final EnumMap<Operator, Function<Object[], Predicate<Object>>> OPERATORS = new EnumMap<>(Operator.class);

    private static final Logger LOG = Logger.getLogger(CriteriaOperator.class.getSimpleName());

    private static final Predicate<Object> ALWAYS_FALSE = o -> false;
    private static final Predicate<Object> ALWAYS_TRUE = o -> true;

    static {
        OPERATORS.put(Operator.EQ, CriteriaOperator::eq);
        OPERATORS.put(Operator.NE, CriteriaOperator::ne);
        OPERATORS.put(Operator.EXISTS, CriteriaOperator::exists);
        OPERATORS.put(Operator.NOT_EXISTS, CriteriaOperator::notExists);
        OPERATORS.put(Operator.LT, CriteriaOperator::lt);
        OPERATORS.put(Operator.LE, CriteriaOperator::le);
        OPERATORS.put(Operator.GT, CriteriaOperator::gt);
        OPERATORS.put(Operator.GE, CriteriaOperator::ge);
        OPERATORS.put(Operator.IN, CriteriaOperator::in);
        OPERATORS.put(Operator.LIKE, CriteriaOperator::like);
        OPERATORS.put(Operator.ILIKE, CriteriaOperator::ilike);

        // Check that all operators are covered
        EnumSet<Operator> s = EnumSet.allOf(Operator.class);
        s.removeAll(OPERATORS.keySet());
        if (! s.isEmpty()) {
            throw new IllegalStateException("Some operators are not implemented: " + s);
        }
    }

    /**
     * Returns a predicate {@code P(x)} for comparing {@code value} and {@code x} as {@code x OP value}.
     * <b>Implementation note:</b> Note that this may mean reverse logic to e.g. {@link Comparable#compareTo}.
     * @param operator
     * @param value
     * @return
     */
    public static Predicate<Object> predicateFor(Operator op, Object[] value) {
        final Function<Object[], Predicate<Object>> funcToGetPredicate = OPERATORS.get(op);
        if (funcToGetPredicate == null) {
            throw new IllegalArgumentException("Unknown operator: " + op);
        }
        return funcToGetPredicate.apply(value);
    }

    private static Object getFirstArrayElement(Object[] value) throws IllegalStateException {
        if (value == null || value.length != 1) {
            throw new IllegalStateException("Invalid argument: " + Arrays.toString(value));
        }
        return value[0];
    }

    public static Predicate<Object> eq(Object[] value) {
        Object value0 = getFirstArrayElement(value);
        return new Predicate<Object>() {
            @Override public boolean test(Object v) { return Objects.equals(v, value0); }
        };
    }

    public static Predicate<Object> ne(Object[] value) {
        Object value0 = getFirstArrayElement(value);
        return new Predicate<Object>() {
            @Override public boolean test(Object v) { return ! Objects.equals(v, value0); }
        };
    }

    public static Predicate<Object> exists(Object[] value) {
        if (value != null && value.length != 0) {
            throw new IllegalStateException("Invalid argument: " + Arrays.toString(value));
        }
        
        return CriteriaOperator::collectionAwareExists;
    }
    
    private static boolean collectionAwareExists(Object checkedObject) {
        if (checkedObject instanceof Collection) {
            return !((Collection<?>) checkedObject).isEmpty();
        }

        return Objects.nonNull(checkedObject);
    }

    public static Predicate<Object> notExists(Object[] value) {
        if (value != null && value.length != 0) {
            throw new IllegalStateException("Invalid argument: " + Arrays.toString(value));
        }

        return CriteriaOperator::collectionAwareNotExists;
    }
    
    private static boolean collectionAwareNotExists(Object checkedObject) {
        if (Objects.isNull(checkedObject)) return true;

        if (checkedObject instanceof Collection) {
            return ((Collection<?>) checkedObject).isEmpty();
        }

        return false;
    }

    public static Predicate<Object> in(Object[] value) {
        if (value == null || value.length == 0) {
            return ALWAYS_FALSE;
        }
        final Collection<?> operand;
        if (value.length == 1) {
            final Object value0 = value[0];
            if (value0 instanceof Collection) {
                operand = (Collection<?>) value0;
            } else if (value0 instanceof Stream) {
                try (Stream<?> valueS = (Stream<?>) value0) {
                    operand = valueS.collect(Collectors.toSet());
                }
            } else {
                operand = Collections.singleton(value0);
            }
        } else {
            operand = new HashSet(Arrays.asList(value));
        }
        return operand.isEmpty() ? ALWAYS_FALSE : new Predicate<Object>() {
            @Override public boolean test(Object v) { return operand.contains(v); }
        };
    }

    public static Predicate<Object> lt(Object[] value) {
        return getComparisonPredicate(ComparisonPredicateImpl.Op.LT, value);
    }

    public static Predicate<Object> le(Object[] value) {
        return getComparisonPredicate(ComparisonPredicateImpl.Op.LE, value);
    }

    public static Predicate<Object> gt(Object[] value) {
        return getComparisonPredicate(ComparisonPredicateImpl.Op.GT, value);
    }

    public static Predicate<Object> ge(Object[] value) {
        return getComparisonPredicate(ComparisonPredicateImpl.Op.GE, value);
    }

    private static Predicate<Object> getComparisonPredicate(ComparisonPredicateImpl.Op op, Object[] value) throws IllegalArgumentException {
        Object value0 = getFirstArrayElement(value);
        if (value0 instanceof Comparable) {
            Comparable cValue = (Comparable) value0;
            return new ComparisonPredicateImpl(op, cValue);
        } else {
            throw new IllegalArgumentException("Incomparable argument for comparison operation: " + value0);
        }
    }

    public static Predicate<Object> like(Object[] value) {
        Object value0 = getFirstArrayElement(value);
        if (value0 instanceof String) {
            String sValue = (String) value0;

            if(Pattern.matches("^%+$", sValue)) {
                return ALWAYS_TRUE;
            }

            boolean anyBeginning = sValue.startsWith("%");
            boolean anyEnd = sValue.endsWith("%");

            Pattern pValue = Pattern.compile(
              (anyBeginning ? ".*" : "")
              + Pattern.quote(sValue.substring(anyBeginning ? 1 : 0, sValue.length() - (anyEnd ? 1 : 0)))
              + (anyEnd ? ".*" : ""),
              Pattern.DOTALL
            );
            return o -> {
                return o instanceof String && pValue.matcher((String) o).matches();
            };
        }
        return ALWAYS_FALSE;
    }

    public static Predicate<Object> ilike(Object[] value) {
        Object value0 = getFirstArrayElement(value);
        if (value0 instanceof String) {
            String sValue = (String) value0;

            if(Pattern.matches("^%+$", sValue)) {
                return ALWAYS_TRUE;
            }

            boolean anyBeginning = sValue.startsWith("%");
            boolean anyEnd = sValue.endsWith("%");

            Pattern pValue = Pattern.compile(
              (anyBeginning ? ".*" : "")
              + Pattern.quote(sValue.substring(anyBeginning ? 1 : 0, sValue.length() - (anyEnd ? 1 : 0)))
              + (anyEnd ? ".*" : ""),
              Pattern.CASE_INSENSITIVE + Pattern.DOTALL
            );
            return o -> {
                return o instanceof String && pValue.matcher((String) o).matches();
            };
        }
        return ALWAYS_FALSE;
    }

    private static class ComparisonPredicateImpl implements Predicate<Object> {

        private static enum Op {
            LT { @Override boolean isComparisonTrue(int compareToValue) { return compareToValue > 0; } },
            LE { @Override boolean isComparisonTrue(int compareToValue) { return compareToValue >= 0; } },
            GT { @Override boolean isComparisonTrue(int compareToValue) { return compareToValue < 0; } },
            GE { @Override boolean isComparisonTrue(int compareToValue) { return compareToValue <= 0; } },
            ;
            abstract boolean isComparisonTrue(int compareToValue);
        }

        private final Op op;
        private final Comparable cValue;

        public ComparisonPredicateImpl(Op op, Comparable cValue) {
            this.op = op;
            this.cValue = cValue;
        }

        @Override
        public boolean test(Object o) {
            try {
                return o != null && op.isComparisonTrue(cValue.compareTo(o));
            } catch (ClassCastException ex) {
                LOG.log(Level.WARNING, "Incomparable argument type for comparison operation: {0}", cValue.getClass().getSimpleName());
                return false;
            }
        }

    }
}
