package org.jmockring.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 21/04/13
 */
public class FunctionsTest {

    @Test
    public void shouldReturnOptionOneIfNonNullOrEmpty() throws Exception {
        assertThat(Functions.ifEmpty("test1", "test2"), is("test1"));
    }

    @Test
    public void shouldReturnOptionTwoIfNullOrEmpty() throws Exception {
        assertThat(Functions.ifEmpty("", "test2"), is("test2"));
        assertThat(Functions.ifEmpty(null, "test2"), is("test2"));
    }

}
