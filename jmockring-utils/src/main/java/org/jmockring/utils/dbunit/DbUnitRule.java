package org.jmockring.utils.dbunit;

import static org.jmockring.utils.dbunit.PropertyUtil.configurePlaceholders;
import static org.jmockring.utils.dbunit.PropertyUtil.getProperty;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.dbunit.IDatabaseTester;
import org.dbunit.IOperationListener;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.operation.DeleteAllOperation;
import org.dbunit.operation.InsertOperation;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to manage the DBUnit setup/tear down operations.
 * Inclusion of this rule in the test is the only requirement to make DBUnit facility work.
 * <p/>
 * Example:
 * <pre>
 *  ....
 *
 * {@literal @}Rule
 *  public DbUnitRule dbUnitRule = new DbUnitRule("/dbunit/dbunit.properties",
 *      "dbunit/entity1_dataset.xml",
 *      "dbunit/entity2_dataset.xml",
 *      "dbunit/entity3_dataset.xml"
 *  ).withTuner(DbUnitTuner.H2);
 *
 *  ....
 * </pre>
 * <p/>
 * Expects the following properties in order to set up the DB connection:
 * <li><b>dbunit.driver</b></li>
 * <li><b>dbunit.connectionUrl</b></li>
 * <li><b>dbunit.user</b></li>
 * <li><b>dbunit.password</b></li>
 * <p/>
 * These can be provided via {@link Properties} or via a filename of a classpath relative `*.properties` resource
 * <p/>
 * <u><b>IMPORTANT:</b></u>
 * <p/>
 * Rules can be used in test suites, however bear in mind that when test is executed outside the suite these rules will not apply.
 * This may require temporarily copying the suite rule inside the test, while debugging.
 *
 * @author Pavel Lechev
 * @since 05/06/13
 */
public class DbUnitRule extends ExternalResource implements IOperationListener {

    private static final Logger log = LoggerFactory.getLogger(DbUnitRule.class);

    private String[] dataSetFileNames;

    private IDatabaseTester databaseTester;

    private boolean debugOnExit;

    private CompositeDataSet loadedDataSet;

    private DbUnitPostExecutor postExecutor;

    private DatabaseOperation setupOperation = DatabaseOperation.CLEAN_INSERT;

    private DatabaseOperation tearDownOperation = DatabaseOperation.DELETE_ALL;

    private DbUnitTuner tuner;

    /**
     * See {@link #DbUnitRule(Properties, String...)}
     *
     * @param propertiesFileName
     * @param dataSetFileNames
     */
    public DbUnitRule(String propertiesFileName, String... dataSetFileNames) {
        Properties properties = new Properties();
        try {
            String fileName = configurePlaceholders(propertiesFileName);
            properties.load(DbUnitRule.class.getResourceAsStream((fileName.startsWith("/") ? "" : "/") + fileName));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        init(properties, dataSetFileNames);
    }

    /**
     * Constructs a rule with:
     * <p/>
     * - DBUnit setUpOperation = CLEAN_INSERT  <br>
     * - DBUnit tearDownOperation = DELETE_ALL <br>
     *
     * @param properties
     * @param dataSetFileNames
     */
    public DbUnitRule(Properties properties, String... dataSetFileNames) {
        init(properties, dataSetFileNames);
    }

    @Override
    public void connectionRetrieved(IDatabaseConnection connection) {
        // log.debug("[DB UNIT] connectionRetrieved called //");
        if (this.tuner != null) {
            // log.debug("[DB UNIT] calling the tuner");
            this.tuner.tune(connection);
        }
    }

    /**
     * Call this to cause dump of some debug info on exit.
     *
     * @return
     * @see #debug(Connection)
     */
    public DbUnitRule debugOnExit() {
        this.debugOnExit = true;
        return this;
    }

    @Override
    public void operationSetUpFinished(IDatabaseConnection connection) {
        // log.debug("[DB UNIT] operationSetUpFinished called");
        closeConnection(connection);
    }

    @Override
    public void operationTearDownFinished(IDatabaseConnection connection) {
        // log.debug("[DB UNIT] operationTearDownFinished called");
        closeConnection(connection);
        if (this.postExecutor != null) {
            this.postExecutor.execute(connection);
        }
    }

    /**
     * Test classes can get hold of the DB Unit tester and register
     * themselves as listeners or perform other setup operations.
     *
     * @return
     */
    public IDatabaseTester getDatabaseTester() {
        return databaseTester;
    }

    /**
     * @param postExecutor
     * @return
     */
    public DbUnitRule withPostExecutor(DbUnitPostExecutor postExecutor) {
        this.postExecutor = postExecutor;
        return this;
    }

    public DbUnitRule withSetupOperation(DatabaseOperation setupOperation) {
        this.setupOperation = setupOperation;
        return this;
    }

    /**
     * Typically the default tear down operation should be used, but when debugging failing tests
     * it is sometimes useful to preserve the data in the database for verification, etc ...
     * <p/>
     * In this case we can temporary set up custom tear down operation ({@link DatabaseOperation#NONE}).
     * <p/>
     * Example usage:
     * <pre>
     *  ....
     *
     * {@literal @}Rule
     *  public DbUnitRule dbUnitRule = new DbUnitRule("/dbunit/dbunit.properties",
     *      "dbunit/entity1_dataset.xml",
     *      "dbunit/entity2_dataset.xml",
     *      "dbunit/entity3_dataset.xml"
     *      )
     *      .withTuner(DbUnitTuner.ORACLE)
     *      .withTearDownOperation(DatabaseOperation.NONE);
     *
     *  ....
     * </pre>
     * <p/>
     * <u><b>IMPORTANT:</b></u>
     * <p/>
     * After this facility is used, make sure you run the changed test
     * with the default tearDown operation at least once to ensure the test cleans up.
     * Failure to do so may cause foreign key violations in other tests which
     * happen to skip some of the affected tables from their XML dataset configurations.
     *
     * @param tearDownOperation
     * @return
     */
    public DbUnitRule withTearDownOperation(DatabaseOperation tearDownOperation) {
        this.tearDownOperation = tearDownOperation;
        return this;
    }

    /**
     * Add an optional tuner to be executed after DBUnit obtains the connection.
     *
     * @param tuner
     * @return
     */
    public DbUnitRule withTuner(DbUnitTuner tuner) {
        this.tuner = tuner;
        return this;
    }

    @Override
    protected void before() throws Throwable {
        loadedDataSet = new CompositeDataSet(buildDataSets(dataSetFileNames));
        databaseTester.setDataSet(loadedDataSet);
        databaseTester.onSetup();
    }

    @Override
    protected void after() {
        try {
            if (debugOnExit) {
                debug(databaseTester.getConnection().getConnection());
            }
            databaseTester.onTearDown();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param connection
     * @throws SQLException
     * @see #debugOnExit
     */
    private void debug(final Connection connection) throws SQLException {
        ResultSet rs = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE", "VIEW"});
        final StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            final String tableName = rs.getString("TABLE_NAME");
            sb.append("\n" + tableName);
            final ResultSet rr = connection.createStatement().executeQuery("SHOW COLUMNS FROM " + tableName);
            while (rr.next()) {
                sb.append("\n\t" + rr.getString(1) + "  |  " + rr.getString(2));
            }
        }
        System.out.println(sb);
    }

    /**
     * @param dataSetFileNames
     * @return
     * @throws DataSetException
     */
    private IDataSet[] buildDataSets(String[] dataSetFileNames) throws DataSetException {
        IDataSet[] dataSets = new IDataSet[dataSetFileNames.length];
        int i = 0;
        FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
        flatXmlDataSetBuilder.setColumnSensing(true);
        for (String fileName : dataSetFileNames) {
            dataSets[i++] = flatXmlDataSetBuilder.build(this.getClass().getClassLoader().getResourceAsStream(fileName));
        }
        return dataSets;
    }

    private void closeConnection(IDatabaseConnection connection) {
        // log.debug("closeConnection(connection={}) - start", connection);
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Exception while closing the connection: " + e, e);
        }
    }


    /**
     * @param properties
     * @param dataSetFileNames
     */
    private void init(Properties properties, String... dataSetFileNames) {
        this.dataSetFileNames = dataSetFileNames;
        try {
            this.databaseTester = new JdbcDatabaseTester(
                    getProperty(properties, "dbunit.driver"),
                    getProperty(properties, "dbunit.connectionUrl"),
                    getProperty(properties, "dbunit.user"),
                    getProperty(properties, "dbunit.password")
            ) {
                /**
                 *
                 * @return
                 * @see CompositeOperation
                 * @see DeleteAllOperation
                 * @see InsertOperation
                 */
                @Override
                protected DatabaseOperation getSetUpOperation() {
                    return setupOperation;
                }

                /**
                 *
                 * @return
                 * @see DeleteAllOperation
                 */
                @Override
                protected DatabaseOperation getTearDownOperation() {
                    return tearDownOperation;
                }
            };
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        this.databaseTester.setOperationListener(this);
    }


}
