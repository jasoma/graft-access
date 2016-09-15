package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoSession
import com.github.jasoma.graft.access.ResultSet
import com.github.jasoma.graft.access.TransactionHandler
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.neo4j.driver.v1.Driver
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
        return new DriverResultSet(tx.run(query), tx)
    }

    @Override
    ResultSet run(Map parameters, String query) {
        def tx = implicitTx()
        return new DriverResultSet(tx.run(query, parameters), tx)
    }

    @Override
    void withTransaction(@DelegatesTo(TransactionHandler)
                         @ClosureParams(value = SimpleType, options = "com.github.jasoma.graft.access.NeoSession") Closure closure) {

        if (currentTx?.isOpen) {
            log.warn("Implicit transaction open during explicit transaction creating, closing the implicit transaction first.")
            currentTx.close()
        }

        def tx = new DriverTransaction(session.beginTransaction())
        closure.delegate = tx
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            closure(new TransactionSession(tx))
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

    def private DriverTransaction implicitTx() {
        if (currentTx?.isOpen) {
            log.warn("Starting a new implicit transaction while one is currently open. Existing transaction will be closed.")
            currentTx.close()
        }
        currentTx = new DriverTransaction(session.beginTransaction())
        return currentTx
    }

    private static class DriverTransaction implements TransactionHandler, Transaction {

        @Delegate private final Transaction tx
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

    /**
     * When a transaction is open statements cannot be run directly against the {@link Session}. This wrapper
     * allows a transaction to function as a session within the closure used in {@link DriverSession#withTransaction}.
     */
    private class TransactionSession implements NeoSession {

        private final Transaction tx

        TransactionSession(Transaction tx) {
            this.tx = tx
        }

        @Override
        ResultSet run(String query) {
            return new DriverResultSet(tx.run(query))
        }

        @Override
        ResultSet run(Map parameters, String query) {
            return new DriverResultSet(tx.run(query, parameters))
        }

        @Override
        void withTransaction(@DelegatesTo(TransactionHandler)
                             @ClosureParams(value = SimpleType, options = "com.github.jasoma.graft.access.NeoSession") Closure closure) {
            throw new IllegalStateException("Nested transactions are not support when the underling access method is ${Driver.name}")
        }

        @Override
        void close() throws Exception {

        }
    }
}
