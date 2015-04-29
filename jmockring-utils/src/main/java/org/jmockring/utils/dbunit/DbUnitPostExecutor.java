package org.jmockring.utils.dbunit;

import org.dbunit.database.IDatabaseConnection;

/**
 * @author Pavel Lechev
 * @since 14/06/13
 */
public interface DbUnitPostExecutor {

    void execute(IDatabaseConnection connection);

}
