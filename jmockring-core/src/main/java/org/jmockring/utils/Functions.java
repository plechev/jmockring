package org.jmockring.utils;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 21/04/13
 */
public class Functions {

    /**
     * Return the second argument if the first one evaluates to null or empty string.
     *
     * @param optionOne
     * @param optionTwo
     * @param <T>
     *
     * @return
     */
    public static <T extends Object> T ifEmpty(T optionOne, T optionTwo) {
        if (optionOne == null || optionOne.toString() == null || optionOne.toString().isEmpty()) {
            return optionTwo;
        }
        return optionOne;
    }

    /**
     * Return optionOne if it does not equal criteria, else return optionTwo
     *
     * @param criteria
     * @param optionOne
     * @param optionTwo
     * @param <T>
     *
     * @return
     */
    public static <T extends Object> T ifNot(T criteria, T optionOne, T optionTwo) {
        if (!criteria.equals(optionOne)) {
            return optionOne;
        }
        return optionTwo;
    }

}
