package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoSession
import com.github.jasoma.graft.access.ResultSet
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Transaction

/**
 * Wrapper around a {@link GraphDatabaseService}.
 */
@Slf4j
class EmbeddedSession implements NeoSession {

    private final GraphDatabaseService db
    private EmbeddedTransaction currentTx
    private List<EmbeddedTransaction> manualTxs = []

    /**
     * Constructor.
     *
     * @param db the database being accessed in the session.
     */
    EmbeddedSession(GraphDatabaseService db) {
        this.db = db
    }

    @Override
    ResultSet run(String query) {
        def tx = implicitTx()
        return new EmbeddedResultSet(db.execute(query), tx)
    }

    @Override
    ResultSet run(Map parameters, String query) {
        def tx = implicitTx()
        return new EmbeddedResultSet(db.execute(query, parameters), tx)
    }

    @Override
    void withTransaction(@DelegatesTo(TransactionHandler)
                         @ClosureParams(value = SimpleType, options = "com.github.jasoma.graft.access.NeoSession") Closure closure) {
        def tx = new EmbeddedTransaction(db.beginTx())
        closure.delegate = tx
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            closure(this)
        }
        finally {
            tx.close()
        }
    }

    @Override
    TransactionHandler beginTransaction() {
        manualTxs.removeAll { !it.isOpen }
        def tx = new EmbeddedTransaction(db.beginTx())
        manualTxs << tx
        return tx
    }

    @Override
    void close() throws Exception {
        if (currentTx?.isOpen) {
            log.warn("Implicit transaction open during session close, closing the transaction also")
            currentTx.close()
        }
        manualTxs.each { tx ->
            if (tx.isOpen) {
                log.warn("Manual transaction open during session close, closing the transaction also")
                tx.close()
            }
        }
    }

    def private TransactionHandler implicitTx() {
        if (currentTx?.isOpen) {
            log.warn("Starting a new implicit transaction while one is currently open. Existing transaction will be closed.")
            currentTx.close()
        }
        currentTx = new EmbeddedTransaction(db.beginTx())
        return currentTx
    }

    private class EmbeddedTransaction implements TransactionHandler {

        private final Transaction tx;
        private def isOpen = true

        EmbeddedTransaction(Transaction tx) {
            this.tx = tx
        }

        @Override
        void success() {
            tx.success()
        }

        @Override
        void failure() {
            tx.failure()
        }

        @Override
        void close() throws Exception {
            isOpen = false
            tx.close()
        }
    }

}
