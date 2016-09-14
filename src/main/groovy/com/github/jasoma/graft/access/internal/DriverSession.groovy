package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoSession
import com.github.jasoma.graft.access.ResultSet
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.neo4j.driver.v1.Session
import org.neo4j.driver.v1.Transaction

/**
 * Wrapper around a {@link Session}.
 */
@Slf4j
class DriverSession implements NeoSession {

    private final Session session
    private DriverTransaction currentTx

    /**
     * Constructor.
     *
     * @param session the driver session to delegate to.
     */
    DriverSession(Session session) {
        this.session = session
    }

    @Override
    ResultSet run(String query) {
        def tx = implicitTx()
        return new DriverResultSet(session.run(query), tx)
    }

    @Override
    ResultSet run(Map parameters, String query) {
        def tx = implicitTx()
        return new DriverResultSet(session.run(query, parameters), tx)
    }

    @Override
    void withTransaction(@DelegatesTo(TransactionHandler)
                         @ClosureParams(value = SimpleType, options = "com.github.jasoma.graft.access.NeoSession") Closure closure) {
        def tx = new DriverTransaction(session.beginTransaction())
        closure.delegate = tx
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        try {
            closure(this)
        }
        finally {
            tx.close()
        }
    }

    @Override
    void close() throws Exception {
        if (currentTx?.isOpen) {
            log.warn("Implicit transaction open during session close, closing the transaction also")
            currentTx.close()
        }
        session.close()
    }

    def private TransactionHandler implicitTx() {
        if (currentTx?.isOpen) {
            log.warn("Starting a new implicit transaction while one is currently open. Existing transaction will be closed.")
            currentTx.close()
        }
        currentTx = new DriverTransaction(session.beginTransaction())
        return currentTx
    }

    private class DriverTransaction implements TransactionHandler {

        private final Transaction tx
        private def isOpen = true

        DriverTransaction(Transaction tx) {
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
