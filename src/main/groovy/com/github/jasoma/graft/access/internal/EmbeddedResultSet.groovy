package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.access.ResultSet
import org.neo4j.graphdb.Result

/**
 * Wrapper around a {@link Result}.
 */
class EmbeddedResultSet implements ResultSet {

    private final Result result
    private final TransactionHandler implicitTransaction

    /**
     * Constructor.
     *
     * @param result the result from querying the embedded database.
     * @param implicitTransaction an implicit transaction to success when the result set is closed or exhausted.
     *                            Only present if the result was created from {@link com.github.jasoma.graft.access.NeoSession#run}
     */
    EmbeddedResultSet(Result result, TransactionHandler implicitTransaction = null) {
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
        return new MapResult(result.next())
    }

    @Override
    void close() throws Exception {
        implicitTransaction?.success()
        implicitTransaction?.close()
        result.close()
    }
}
