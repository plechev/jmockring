package org.jmockring.junit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import org.jmockring.annotation.BootstrapConfig;
import org.jmockring.annotation.Param;
import org.jmockring.annotation.Servers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.TestClass;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Pavel Lechev <plechev@cisco.com>
 * @since 04/11/13
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerBootstrapTest {

    private ServerBootstrap underTest;

    @Mock
    private ConfigurableTargetRunner runnerMock;

    @Before
    public void setUp() throws Exception {

        when(runnerMock.getConfiguredTestClass()).thenReturn(new TestClass(DummyTestClass.class));

        underTest = new ServerBootstrap(runnerMock);


    }

    @Test
    public void shouldVerifyBootstrapConfig() throws Exception {

        underTest.runAll();

        assertThat(System.getProperty("sProp1"), is("sValue1"));
        assertThat(System.getProperty("sProp2"), is("sValue2"));
    }


    @RunWith(ExternalServerJUnitRunner.class)
    @BootstrapConfig(numberOfAttempts = 10,
            systemProperties = {@Param(name = "sProp1", value = "sValue1"), @Param(name = "sProp2", value = "sValue2")})
    @Servers({})
    static final class DummyTestClass {


    }


}
