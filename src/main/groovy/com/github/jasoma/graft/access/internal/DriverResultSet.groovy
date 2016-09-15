package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.access.ResultSet
import com.github.jasoma.graft.access.TransactionHandler
import org.neo4j.driver.v1.StatementResult

/**
 * Wrapper around a {@link StatementResult}.
 */
class DriverResultSet implements ResultSet {

    private final StatementResult result
    private final TransactionHandler implicitTransaction

    /**
     * Constructor.
     *
     * @param result the result from querying via the driver.
     * @param implicitTransaction an implicit transaction to success when the result set is closed or exhausted.
     *                            Only present if the result was created from {@link com.github.jasoma.graft.access.NeoSession#run}
     */
    DriverResultSet(StatementResult result, TransactionHandler implicitTransaction = null) {
        this.result = result
        this.implicitTransaction = implicitTransaction
    }

    @Override
    boolean hasNext() {
        def hasNext = result.hasNext()
        if (implicitTransaction && !hasNext) {
            implicitTransaction.success()
            implicitTransaction.close()
        }
        return hasNext
    }

    @Override
    ResultRow next() {
        return new MapResult(result.next().asMap())
    }

    @Override
    void close() throws Exception {
        implicitTransaction?.success()
        implicitTransaction?.close()
        result.consume()
    }
}
