package org.jmockring.utils.hamcrest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * General purpose collection matcher which allows to assert equality/expectations on named fields of the collection elements.
 * The matching is not dependent on the ordering of the elements in the collection.
 * <p/>
 * Note: use of this matcher implicitly asserts NON NULL on the passed collection/iterable.
 * <p/>
 * Example: <br>
 * (with matchers (recommended)):
 * <pre>
 * assertThat(assets, collectionWithSize(2)
 *              .hasElementWhere("fileName", is("my3.jpeg")).andWhere("order", is(1))
 *              .hasElementWhere("fileName", is("my.jpeg")).andWhere("order", is(2))
 * );
 * </pre>
 * <p/>
 * (direct match: x.equals(y)):
 * <pre>
 * assertThat(assets, collectionWithSize(2)
 *              .hasElementWith("fileName", "my3.jpeg").andWith("order", 1)
 *              .hasElementWith("fileName", "my.jpeg").andWith("order", 2)
 * );
 * </pre>
 * where <code>assets</code> is a collection of Asset instances, which have fields named `fileName` and `order` (among others).
 *
 * @author Pavel Lechev
 * @since 12/12/12
 */
public class CollectionIntrospectMatcher extends BaseMatcher<Collection> {

    List<Introspect> introspects = new ArrayList<Introspect>();

    private int expectedSize = -1;

    private boolean nullValueDetected;

    private int realSize;

    /**
     * @param expectedSize
     * @return
     */
    public static CollectionIntrospectMatcher collectionWithSize(int expectedSize) {
        return new CollectionIntrospectMatcher().hasSize(expectedSize);
    }

    /**
     * Append more field expectations to already defined introspection via #hasElementWith().
     * This is a chained method allowing any number of calls.
     *
     * @param property
     * @param matcher
     * @return
     */
    public CollectionIntrospectMatcher andWhere(String property, Matcher matcher) {
        if (this.introspects.size() == 0) {
            throw new IllegalStateException("No introspects yet. Call #hasElementWith() at least once");
        }
        this.introspects.get(this.introspects.size() - 1).add(property, matcher);
        return this;
    }

    /**
     * Append more field expectations to already defined introspection via #hasElementWith().
     * This is a chained method allowing any number of calls.
     *
     * @param property
     * @param value
     * @return
     */
    public CollectionIntrospectMatcher andWith(String property, Object value) {
        if (this.introspects.size() == 0) {
            throw new IllegalStateException("No introspects yet. Call #hasElementWith() at least once");
        }
        this.introspects.get(this.introspects.size() - 1).add(property, value);
        return this;
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        super.describeMismatch(convertToString((Collection) item), description);

    }

    @Override
    public void describeTo(Description description) {
        if (nullValueDetected) {
            description.appendText("Non-null collection");
        } else if (expectedSize >= 0 && expectedSize != realSize) {
            description.appendText(String.format("Size of [%s], but real size was [%s]", expectedSize, realSize));
        } else {
            description.appendText("\n");
            for (Introspect introspect : introspects) {
                description.appendValue(introspect);
                description.appendText(introspect.matched ? " >> Matched \n" : " >> Unmatched \n");
                if (introspect.failedValues != null) {
                    description.appendText("\t> Failed values: ");
                    for (Object failedValue : introspect.failedValues) {
                        description.appendText("\n \t\t > " + failedValue);
                    }
                    description.appendText("\n");
                }
            }
        }
    }

    public final CollectionIntrospectMatcher hasElementWhere(String property, Matcher matcher) {
        this.introspects.add(new Introspect(property, matcher));
        return this;
    }

    /**
     * Add new introspection specification.
     * This must be called at least once. The #andWith() call is then used to append field expectations to this introspection.
     * <p/>
     * This is a chained method allowing any number of introspections to be added to the matcher.
     *
     * @param property
     * @param value
     * @return
     * @see #andWith(String, Object)
     */
    public final CollectionIntrospectMatcher hasElementWith(String property, Object value) {
        this.introspects.add(new Introspect(property, value));
        return this;
    }

    /**
     * Set size expectations for the collection. If not called, size will not be verified.
     * Can be any value equal or greater than 0.
     *
     * @param expectedSize
     * @return
     * @deprecated - use {@link #collectionWithSize(int)} instead
     */
    private CollectionIntrospectMatcher hasSize(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("Illegal value for collection size: " + expectedSize);
        } else if (this.expectedSize >= 0) {
            // already called
            throw new IllegalStateException("Expected collection size already specified: " + expectedSize + ". Did you call #hasSize() twice?");
        }
        this.expectedSize = expectedSize;
        return this;
    }

    @Override
    public boolean matches(Object input) {
        // some pre-checks
        if (expectedSize >= 0 && expectedSize < introspects.size()) {
            throw new IllegalStateException(
                    String.format("Expected size [%s] is less than the defined number of introspections [%s]. Must be equal(ideally) or greater.",
                            expectedSize,
                            introspects.size())
            );
        } else if (introspects.size() == 0) {
            // no introspections defined :: do we really need to use this matcher ??
            throw new IllegalStateException("No introspections defined. If only collection size is asserted, consider using the standard hamcrest matchers which provide better error feedback.");
        }
        // begin assertions
        if (nullValueDetected = (input == null)) {
            return false;
        }

        Collection collection = (Collection) input;
        realSize = collection.size();
        if (expectedSize >= 0 && realSize != expectedSize) {
            return false;
        }

        PropertyUtilsBean propertyBean = new PropertyUtilsBean();
        for (Introspect introspect : this.introspects) {
            introspect.matched = false;
            List<Object> failedValues = new ArrayList<Object>();
            propertyBean.clearDescriptors();
            for (Object item : collection) {
                boolean introspectMatched = true;
                for (Map.Entry<String, Object> entry : introspect.fields.entrySet()) {
                    String propertyName = entry.getKey();
                    Object expectedValue = entry.getValue();
                    try {
                        Object realValue = getRealValue(propertyBean, item, propertyName);

                        // process extracted value
                        if (expectedValue instanceof Matcher) {
                            Matcher matcher = (Matcher) expectedValue;
                            if (!matcher.matches(realValue)) {
                                failedValues.add(realValue);
                                introspectMatched = false;
                                break;
                            }
                        } else if ((expectedValue == null && realValue != null) || (expectedValue != null && !expectedValue.equals(realValue))) {
                            failedValues.add(realValue);
                            introspectMatched = false;
                            break;
                        }
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException(String.format("No public property or accessors for '%s' found in class '%s'", propertyName, item.getClass().getName()), e);
                    } catch (Exception e) {
                        throw new IllegalStateException("Illegal method call", e);
                    }
                }
                if (introspectMatched) {
                    introspect.matched = true;
                    break;
                }
            }
            if (!introspect.matched) {
                introspect.failedValues = failedValues;
            }
        }

        // return TRUE only if all introspects have been matched
        return (expectedSize < 0 || expectedSize == realSize) && Iterables.all(this.introspects, new Predicate<Introspect>() {
            @Override
            public boolean apply(@Nullable Introspect input) {
                assert input != null;
                return input.matched;
            }
        });
    }

    private String convertToString(Collection elements) {
        if (elements == null) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder("Collection of type: ").append(elements.getClass().getName()).append(" [");
        int i = 0;
        for (Object elt : elements) {
            sb.append(++i).append(". ").append(elt == null ? null : elt.toString()).append("\n");
        }
        return sb.append("]").toString();
    }

    private Object getRealValue(final PropertyUtilsBean propertyBean, final Object item, final String propertyName)
            throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Object realValue = null;
        try {
            realValue = propertyBean.getProperty(item, propertyName);
        } catch (NestedNullException e) {
            // nested property in NULL parent - treat it as NULL, instead of failing
            return realValue;
        } catch (NoSuchMethodException e) {
            // check if the class has public field
            String[] nestedFieldNames = propertyName.split("\\.");
            if (nestedFieldNames.length >= 1) {
                Field publicField = item.getClass().getField(nestedFieldNames[0]);
                realValue = publicField.get(item);
                if (nestedFieldNames.length == 2) {
                    Field publicField2 = realValue.getClass().getField(nestedFieldNames[1]);
                    realValue = publicField2.get(realValue);
                } else if (nestedFieldNames.length > 2) {
                    throw new UnsupportedOperationException("Double-nested public fields not supported", e);
                }
            }
        }
        return realValue;
    }

    /**
     *
     */
    static final class Introspect {

        public List<Object> failedValues;

        Map<String, Object> fields;

        boolean matched = false;

        private Introspect(String property, Object value) {
            this.fields = new HashMap<String, Object>();
            this.fields.put(property, value);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Introspect");
            sb.append("{fields=").append(fields);
            sb.append('}');
            return sb.toString();
        }

        private Introspect add(String property, Object value) {
            this.fields.put(property, value);
            return this;
        }


    }

}
