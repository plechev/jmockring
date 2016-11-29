package org.jmockring.utils.dbunit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jmockring.utils.dbunit.PropertyUtil.configurePlaceholders;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import org.dbunit.IOperationListener;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


/**
 * @author Pavel Lechev
 * @since 29/04/2015
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore("needs further setup")
public class DbUnitRuleTest implements IOperationListener {

    @Mock
    private DatabaseConfig configMock;

    @Mock
    private IDatabaseConnection connMock;

    private boolean connectionRetrievedCalled;

    @Captor
    private ArgumentCaptor<Oracle10DataTypeFactory> factoryCaptor;

    private boolean operationSetUpFinishedCalled;

    private boolean operationTearDownFinishedCalled;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    private DbUnitRule underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new DbUnitRule("/dbunit.properties",
                "entity1_dataset.xml",
                "entity2_dataset.xml"
        ).withSetupOperation(DatabaseOperation.CLEAN_INSERT);
    }

    @Override
    public void connectionRetrieved(IDatabaseConnection connection) {
        connectionRetrievedCalled = true;
    }

    @Override
    public void operationSetUpFinished(IDatabaseConnection connection) {
        operationSetUpFinishedCalled = true;
    }

    @Override
    public void operationTearDownFinished(IDatabaseConnection connection) {
        operationTearDownFinishedCalled = true;
    }

    @Test
    public void shouldCheckConnectionRetrievedWithOracleTuner() throws Exception {

        when(connMock.getConfig()).thenReturn(configMock);

        underTest.withTuner(DbUnitTuner.ORACLE).connectionRetrieved(connMock);

        verify(connMock).getConfig();
        verifyNoMoreInteractions(connMock);

        verify(configMock).setProperty(stringCaptor.capture(), factoryCaptor.capture());
        assertThat(stringCaptor.getValue(), is(DatabaseConfig.PROPERTY_DATATYPE_FACTORY));
        assertThat(factoryCaptor.getValue(), is(notNullValue()));
        verifyNoMoreInteractions(configMock);
    }

    @Test
    public void shouldCheckConnectionRetrievedWithoutTuner() throws Exception {
        underTest.connectionRetrieved(connMock);

        verifyNoMoreInteractions(connMock);
    }

    @Test
    public void shouldCheckOperationTearDownFinished() throws Exception {
        underTest.operationTearDownFinished(connMock);

        verify(connMock).close();
        verifyNoMoreInteractions(connMock);
    }

    @Test
    public void shouldCheckoperationSetUpFinished() throws Exception {
        underTest.operationSetUpFinished(connMock);

        verify(connMock).close();
        verifyNoMoreInteractions(connMock);
    }

    @Test
    public void shouldInitialiseRuleWithPropertiesInstance() throws Exception {

        Properties properties = new Properties();
        try {
            properties.load(DbUnitRule.class.getResourceAsStream(configurePlaceholders("/dbunit.properties")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        underTest = new DbUnitRule(properties, "entity1_dataset.xml", "entity2_dataset.xml");

        assertThat(underTest.getDatabaseTester().getConnection(), is(notNullValue()));
    }

    @Test
    public void shouldParseSettingsFileNameForPlaceholders() throws Exception {
        assertThat(underTest.getDatabaseTester().getConnection(), is(notNullValue()));
    }

    @Test
    public void shouldSetupDataSets() throws Throwable {

        underTest.before();

        IDataSet dataSet = underTest.getDatabaseTester().getDataSet();
        assertThat(dataSet, is(notNullValue()));
        String[] tableNames = dataSet.getTableNames();
        assertThat(tableNames.length, is(6));  // total of 6 tables are managed by the dbunit dataset (this is not the total number of tables in CES DB !!!)

        assertThat(dataSet.getTable("TABLE1").getRowCount(), is(8));
        assertThat(dataSet.getTable("TABLE2").getRowCount(), is(3));

    }

    @Test
    public void shouldVerifyBeforeAndAfterOperationCallsWithDefaultTearDown() throws Throwable {
        underTest.getDatabaseTester().setOperationListener(this);

        underTest.before();
        underTest.after();

        assertThat(connectionRetrievedCalled, is(true));
        assertThat(operationSetUpFinishedCalled, is(true));
        assertThat(operationTearDownFinishedCalled, is(true));
    }

    @Test
    public void shouldVerifyBeforeAndAfterOperationCallsWithTearDownNONE() throws Throwable {

        underTest.withTearDownOperation(DatabaseOperation.NONE)
                .getDatabaseTester().setOperationListener(this);

        underTest.before();
        underTest.after();

        assertThat(connectionRetrievedCalled, is(true));
        assertThat(operationSetUpFinishedCalled, is(true));
        assertThat(operationTearDownFinishedCalled, is(false)); // this is DatabaseOperation.NONE, so operation will not be called
    }
}

