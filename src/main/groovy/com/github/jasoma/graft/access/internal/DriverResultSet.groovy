package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.access.ResultSet
import org.neo4j.driver.v1.StatementResult

/**
 * Wrapper around a {@link StatementResult}.
 */
class DriverResultSet implements ResultSet {

    private final StatementResult result

    /**
     * Constructor.
     *
     * @param result the result from querying via the driver.
     */
    DriverResultSet(StatementResult result) {
        this.result = result
    }

    @Override
    boolean hasNext() {
        return result.hasNext()
    }

    @Override
    ResultRow next() {
        return new MapResult(result.next().asMap())
    }
}
