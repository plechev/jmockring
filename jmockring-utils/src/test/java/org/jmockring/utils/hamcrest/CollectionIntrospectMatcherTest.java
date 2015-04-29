package org.jmockring.utils.hamcrest;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.jmockring.utils.hamcrest.CollectionIntrospectMatcher.collectionWithSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Pavel Lechev
 * @since 14/12/12
 */
public class CollectionIntrospectMatcherTest {

    private CollectionIntrospectMatcher underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CollectionIntrospectMatcher();
    }

    @Test
    public void shouldAddMultipleIntrospects() throws Exception {
        underTest.hasElementWith("stringField1", "field1Value_1")
                .andWith("intField1", 101)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("intField1", 202)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2);

        assertThat(underTest.introspects.size(), is(2));

        assertThat(underTest.introspects.get(0).fields.size(), is(2));
        assertThat((String) underTest.introspects.get(0).fields.get("stringField1"), is("field1Value_1"));
        assertThat((Integer) underTest.introspects.get(0).fields.get("intField1"), is(101));

        assertThat(underTest.introspects.get(1).fields.size(), is(4));
        assertThat((String) underTest.introspects.get(1).fields.get("stringField1"), is("field1Value_2"));
        assertThat((Integer) underTest.introspects.get(1).fields.get("intField1"), is(202));
        assertThat((DummyEnum) underTest.introspects.get(1).fields.get("enumField1"), is(DummyEnum.CONST1));
        assertThat((DummyEnum) underTest.introspects.get(1).fields.get("enumField2"), is(DummyEnum.CONST2));
    }

    @Test
    public void shouldAddNewIntrospect() throws Exception {
        underTest.hasElementWith("stringField1", "field1Value_1")
                .andWith("intField1", 101);
        assertThat(underTest.introspects.size(), is(1));
        assertThat(underTest.introspects.get(0).fields.size(), is(2));
        assertThat((String) underTest.introspects.get(0).fields.get("stringField1"), is("field1Value_1"));
        assertThat((Integer) underTest.introspects.get(0).fields.get("intField1"), is(101));
    }

    @Test
    public void shouldMatchAllCollectionElementsInAnyOrder() throws Exception {
        underTest = collectionWithSize(4)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("stringField2", "field2Value_2")
                .andWith("intField1", 202)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST3)
                .hasElementWith("stringField1", "field1Value_1")
                .andWith("stringField2", "field2Value_1")
                .andWith("intField1", 101)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2)
                .hasElementWith("stringField1", "field1Value_4")
                .andWith("stringField2", "field2Value_4")
                .andWith("intField1", 404)
                .andWith("enumField1", DummyEnum.CONST2)
                .andWith("enumField2", DummyEnum.CONST3)
                .hasElementWith("stringField1", "field1Value_3")
                .andWith("stringField2", "field2Value_3")
                .andWith("intField1", 303)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2);

        assertThat(underTest.matches(getDummyCollection()), is(true));
    }

    @Test
    public void shouldNotMatchAllCollectionElements() throws Exception {
        underTest = collectionWithSize(4)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("stringField2", "field2Value_2")
                .andWith("intField1", 202)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST3)
                .hasElementWith("stringField1", "field1Value_1")
                .andWith("stringField2", "field2Value_1")
                .andWith("intField1", 202) // the bean has 101, so this should not match
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2)
                .hasElementWith("stringField1", "field1Value_4")
                .andWith("stringField2", "field2Value_4")
                .andWith("intField1", 404)
                .andWith("enumField1", DummyEnum.CONST2)
                .andWith("enumField2", DummyEnum.CONST3)
                .hasElementWith("stringField1", "field1Value_3")
                .andWith("stringField2", "field2Value_3")
                .andWith("intField1", 303)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2);
        assertThat(underTest.matches(getDummyCollection()), is(false));
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectIfExpectedSizeIsLessThanZero() throws Exception {
        underTest = collectionWithSize(-1)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("intField1", 202)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectIfNoIntrospectionsDefined() throws Exception {
        underTest = collectionWithSize(4);
        underTest.matches(getDummyCollection());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldReject_andWith$_if_hasElementWith$_notCalled() throws Exception {
        underTest.andWith("intField1", 101)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("intField1", 202)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2);
    }

    @Test
    public void shouldReturnFalseForIncorrectSize() throws Exception {
        underTest = collectionWithSize(2)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("stringField2", "field2Value_2")
                .andWith("intField1", 202)
                .andWith("enumField2", DummyEnum.CONST3);
        assertThat(underTest.matches(getDummyCollection()), is(false));
    }

    @Test
    public void shouldReturnFalseForNonMatchedCollectionOfBeansWithNestedBeans() throws Exception {
        underTest = collectionWithSize(3)
                .hasElementWhere("privateProperty", is("pivateString1"))
                .andWhere("publicProperty", is("publicString1"))
                .andWhere("privateNested.stringField1", is("nested1Private_1.1"))
                .andWhere("privateNested.stringField2", is("nested1Private_2.1"))
                .andWhere("privateNested.intField1", is(101))
                .andWhere("privateNestedNested.privateNested.stringField1", is("<<< BREAK_THE_MATCH_HERE >>>"))
                .andWhere("privateNestedNested.privateNested.stringField2", is("nested1nested1_1.2"))
                .andWhere("privateNestedNested.privateNested.intField1", is(1001))
                .andWhere("privateNestedNested.privateNestedNested", is(nullValue()))
                .hasElementWhere("privateProperty", is("pivateString2"))
                .andWhere("publicProperty", is("publicString2"))
                .andWhere("privateNested.stringField1", is("nested2Private_1.1"))
                .andWhere("privateNested.stringField2", is("nested2Private_2.1"))
                .andWhere("privateNested.intField1", is(201))
                .andWhere("privateNestedNested.privateNested.stringField1", is("nested2nested2_1.1"))
                .andWhere("privateNestedNested.privateNested.stringField2", is("nested2nested2_1.2"))
                .andWhere("privateNestedNested.privateNested.intField1", is(2001))
                .andWhere("privateNestedNested.privateNestedNested", is(nullValue()))
                .hasElementWhere("privateProperty", is("pivateString3"))
                .andWhere("publicProperty", is("publicString2"))
                .andWhere("privateNested.stringField1", is("nested3Private_1.1"))
                .andWhere("privateNested.stringField2", is("nested3Private_2.1"))
                .andWhere("privateNested.intField1", is(301))
                .andWhere("privateNestedNested.privateNested.stringField1", is("nested3nested3_1.1"))
                .andWhere("privateNestedNested.privateNested.stringField2", is("nested3nested3_1.2"))
                .andWhere("privateNestedNested.privateNested.intField1", is(3001))
                .andWhere("privateNestedNested.privateNestedNested", is(nullValue()))
        ;
        assertThat(underTest.matches(getDummyCollectionWithNestedBeans()), is(false));
    }

    @Test
    public void shouldReturnFalseForNullInput() throws Exception {
        underTest = collectionWithSize(4)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("stringField2", "field2Value_2")
                .andWith("intField1", 202)
                .andWith("enumField2", DummyEnum.CONST3);
        assertThat(underTest.matches(null), is(false));
    }

    @Test
    public void shouldReturnTrueForMatchedCollectionOfBeansWithNestedBeans() throws Exception {
        underTest = collectionWithSize(3)
                .hasElementWhere("privateProperty", is("pivateString1"))
                .andWhere("publicProperty", is("publicString1"))
                .andWhere("privateNested.stringField1", is("nested1Private_1.1"))
                .andWhere("privateNested.stringField2", is("nested1Private_2.1"))
                .andWhere("privateNested.intField1", is(101))
                .andWhere("privateNestedNested.privateNested.stringField1", is("nested1nested1_1.1"))
                .andWhere("privateNestedNested.privateNested.stringField2", is("nested1nested1_1.2"))
                .andWhere("privateNestedNested.privateNested.intField1", is(1001))
                .andWhere("privateNestedNested.privateNestedNested", is(nullValue()))
                .hasElementWhere("privateProperty", is("pivateString2"))
                .andWhere("publicProperty", is("publicString2"))
                .andWhere("privateNested.stringField1", is("nested2Private_1.1"))
                .andWhere("privateNested.stringField2", is("nested2Private_2.1"))
                .andWhere("privateNested.intField1", is(201))
                .andWhere("privateNestedNested.privateNested.stringField1", is("nested2nested2_1.1"))
                .andWhere("privateNestedNested.privateNested.stringField2", is("nested2nested2_1.2"))
                .andWhere("privateNestedNested.privateNested.intField1", is(2001))
                .andWhere("privateNestedNested.privateNestedNested", is(nullValue()))
                .hasElementWhere("privateProperty", is("pivateString3"))
                .andWhere("publicProperty", is("publicString3"))
                .andWhere("privateNested.stringField1", is("nested3Private_1.1"))
                .andWhere("privateNested.stringField2", is("nested3Private_2.1"))
                .andWhere("privateNested.intField1", is(301))
                .andWhere("privateNestedNested.privateNested.stringField1", is("nested3nested3_1.1"))
                .andWhere("privateNestedNested.privateNested.stringField2", is("nested3nested3_1.2"))
                .andWhere("privateNestedNested.privateNested.intField1", is(3001))
                .andWhere("privateNestedNested.privateNestedNested", is(nullValue()))
        ;
        assertThat(underTest.matches(getDummyCollectionWithNestedBeans()), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionForInvalidExpectedSize() throws Exception {
        underTest = collectionWithSize(1) // expecting size 1, but defining 2 introspected elements
                .hasElementWith("stringField1", "field1Value_1")
                .andWith("stringField2", "field2Value_1")
                .andWith("intField1", 101)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2)
                .hasElementWith("stringField1", "field1Value_4")
                .andWith("stringField2", "field2Value_4")
                .andWith("intField1", 404)
                .andWith("enumField1", DummyEnum.CONST2)
                .andWith("enumField2", DummyEnum.CONST3);
        underTest.matches(getDummyCollection());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionForInvalidFieldName() throws Exception {
        underTest = collectionWithSize(4)
                .hasElementWith("stringField1", "field1Value_1")
                .andWith("stringField2", "field2Value_1")
                .andWith("intField1", 101)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2)
                .hasElementWith("stringField1", "field1Value_4")
                .andWith("stringField2", "field2Value_4")
                .andWith("intField1", 404)
                .andWith("enumField1", DummyEnum.CONST2)
                .andWith("enumField2", DummyEnum.CONST3)
                .hasElementWith("stringField1", "field1Value_3")
                .andWith("stringField2", "field2Value_3")
                .andWith("intField1", 303)
                .andWith("enumField1", DummyEnum.CONST1)
                .andWith("enumField2", DummyEnum.CONST2)
                .hasElementWith("stringField1", "field1Value_2")
                .andWith("stringField2", "field2Value_2")
                .andWith("intField1", 202)
                .andWith("_invalidFieldName_", DummyEnum.CONST1) // does not exist
                .andWith("enumField2", DummyEnum.CONST3);
        underTest.matches(getDummyCollection());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfFieldNotAccessible() throws Exception {
        underTest.hasElementWith("nonAccessibleField", 101.2);
        underTest.matches(getDummyCollection());
    }

    /**
     * @return
     */
    private Collection<DummyBean> getDummyCollection() {
        List<DummyBean> dummyBeans = new ArrayList<DummyBean>();
        dummyBeans.add(new DummyBean("field1Value_1", "field2Value_1", 101, DummyEnum.CONST1, DummyEnum.CONST2));
        dummyBeans.add(new DummyBean("field1Value_2", "field2Value_2", 202, DummyEnum.CONST1, DummyEnum.CONST3));
        dummyBeans.add(new DummyBean("field1Value_3", "field2Value_3", 303, DummyEnum.CONST1, DummyEnum.CONST2));
        dummyBeans.add(new DummyBean("field1Value_4", "field2Value_4", 404, DummyEnum.CONST2, DummyEnum.CONST3));
        return dummyBeans;
    }


    private Collection<DummyParentBean> getDummyCollectionWithNestedBeans() {
        List<DummyParentBean> dummyParentBeans = new ArrayList<DummyParentBean>();
        dummyParentBeans.add(new DummyParentBean(
                new DummyBean("nested1Private_1.1", "nested1Private_2.1", 101, DummyEnum.CONST1, DummyEnum.CONST2),
                new DummyParentBean(
                        new DummyBean("nested1nested1_1.1", "nested1nested1_1.2", 1001, DummyEnum.CONST1, DummyEnum.CONST2),
                        null,
                        "privateNestedNested1",
                        "publicNestedNested1"
                ),
                "pivateString1",
                "publicString1"
        ));
        dummyParentBeans.add(new DummyParentBean(
                new DummyBean("nested2Private_1.1", "nested2Private_2.1", 201, DummyEnum.CONST1, DummyEnum.CONST2),
                new DummyParentBean(
                        new DummyBean("nested2nested2_1.1", "nested2nested2_1.2", 2001, DummyEnum.CONST1, DummyEnum.CONST2),
                        null,
                        "privateNestedNested2",
                        "publicNestedNested2"
                ),
                "pivateString2",
                "publicString2"
        ));
        dummyParentBeans.add(new DummyParentBean(
                new DummyBean("nested3Private_1.1", "nested3Private_2.1", 301, DummyEnum.CONST1, DummyEnum.CONST2),
                new DummyParentBean(
                        new DummyBean("nested3nested3_1.1", "nested3nested3_1.2", 3001, DummyEnum.CONST1, DummyEnum.CONST2),
                        null,
                        "privateNestedNested3",
                        "publicNestedNested3"
                ),
                "pivateString3",
                "publicString3"
        ));
        return dummyParentBeans;
    }


    private static enum DummyEnum {
        CONST1, CONST2, CONST3
    }

    public static class DummyBean {

        private DummyEnum enumField1;

        private DummyEnum enumField2;

        private int intField1;

        private double nonAccessibleField;    // has private accessor (just to ascertain the test case);

        private String stringField1;

        private String stringField2;

        private DummyBean(String stringField1, String stringField2, int intField1, DummyEnum enumField1, DummyEnum enumField2) {
            this.stringField1 = stringField1;
            this.stringField2 = stringField2;
            this.intField1 = intField1;
            this.enumField1 = enumField1;
            this.enumField2 = enumField2;
            this.nonAccessibleField = 101.1;
        }

        public DummyEnum getEnumField1() {
            return enumField1;
        }

        public DummyEnum getEnumField2() {
            return enumField2;
        }

        public int getIntField1() {
            return intField1;
        }

        public String getStringField1() {
            return stringField1;
        }

        public String getStringField2() {
            return stringField2;
        }

        private double getNonAccessibleField() {
            return nonAccessibleField;
        }

    }

    public static class DummyParentBean {

        public String publicProperty;

        private DummyBean privateNested;

        private DummyParentBean privateNestedNested;

        private String privateProperty;

        public DummyParentBean(final DummyBean privateNested, final DummyParentBean privateNestedNested, final String privateProperty, final String publicProperty) {
            this.privateNested = privateNested;
            this.privateNestedNested = privateNestedNested;
            this.privateProperty = privateProperty;
            this.publicProperty = publicProperty;
        }

        public DummyBean getPrivateNested() {
            return privateNested;
        }

        public void setPrivateNested(final DummyBean privateNested) {
            this.privateNested = privateNested;
        }

        public DummyParentBean getPrivateNestedNested() {
            return privateNestedNested;
        }

        public void setPrivateNestedNested(final DummyParentBean privateNestedNested) {
            this.privateNestedNested = privateNestedNested;
        }

        public String getPrivateProperty() {
            return privateProperty;
        }

        public void setPrivateProperty(final String privateProperty) {
            this.privateProperty = privateProperty;
        }
    }
}
