package org.jmockring.utils.dbunit;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

/**
 * Fine-tune the operation of the {@link DbUnitRule}
 *
 * @since 06/06/13
 */
public enum DbUnitTuner {


    /**
     * Oracle specific tuning...
     */
    ORACLE {
        @Override
        public void tune(IDatabaseConnection connection) {
            try {
                // Configure Oracle connection to handle BLOB datatypes.
                connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
            } catch (Exception e) {
                throw new IllegalStateException("Can't configure Oracle DB connection", e);
            }
        }
    },

    H2 {
        @Override
        public void tune(IDatabaseConnection connection) {
            try {
                // Configure H2 connection to handle BLOB datatypes.
                connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new H2DataTypeFactory());
            } catch (Exception e) {
                throw new IllegalStateException("Can't configure H2 DB connection", e);
            }
        }
    },

    POSTGRES {
        @Override
        public void tune(IDatabaseConnection connection) {
            try {
                connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
            } catch (Exception e) {
                throw new IllegalStateException("Can't configure PostgreSQL DB connection", e);
            }
        }
    };

    /**
     * @param connection this is the active connection DbUnit tester will use to setup/tear down the database.
     */
    public abstract void tune(IDatabaseConnection connection);

}
