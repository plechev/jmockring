package org.jmockring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;
import org.mockito.Mockito;

public class JMockringTest {


    /**
     * @verifies call mocking provider to determine mock state
     * @see JMockring#isMock(Object)
     */
    @Test
    public void isMock_shouldCallMockingProviderToDetermineMockState() throws Exception {
        final Dummy mock = Mockito.mock(Dummy.class);
        assertThat(JMockring.isMock(mock), is(true));
    }

    /**
     * @verifies call mocking provider to determine non mock state
     * @see JMockring#isMock(Object)
     */
    @Test
    public void isMock_shouldCallMockingProviderToDetermineNonMockState() throws Exception {
        assertThat(JMockring.isMock(new Dummy()), is(false));
    }


    static class Dummy {

    }

}
