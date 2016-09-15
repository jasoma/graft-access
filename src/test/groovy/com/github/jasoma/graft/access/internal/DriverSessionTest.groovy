package com.github.jasoma.graft.access.internal

import org.junit.Test
import org.neo4j.driver.v1.Session
import org.neo4j.driver.v1.StatementResult
import org.neo4j.driver.v1.Transaction

import static org.mockito.Mockito.*

class DriverSessionTest {

    def Session innerSession = mock(Session)
    def StatementResult result = mock(StatementResult)
    def Transaction tx = mock(Transaction)

    def session = new DriverSession(innerSession)

    @Test
    def void testRun() {
        when(innerSession.run("query")).thenReturn(result)
        when(innerSession.beginTransaction()).thenReturn(tx)

        session.run("query")

        verify(innerSession).beginTransaction()
        verify(tx).run("query")
    }

    @Test
    def void testRunWithParameters() {
        when(innerSession.run("query", [with: 'params'])).thenReturn(result)
        when(innerSession.beginTransaction()).thenReturn(tx)

        session.run("query", with: 'params')

        verify(innerSession).beginTransaction()
        verify(tx).run("query", [with: 'params'])
    }

    @Test
    def void testWithTransaction() {
        when(innerSession.beginTransaction()).thenReturn(tx)

        session.withTransaction { s ->
            success()
            failure()
        }

        verify(tx).success()
        verify(tx).failure()
        verify(tx).close()
    }

    @Test
    def void testClosesImplicitTransactionOnNextRun() {
        def txs = [mock(Transaction), mock(Transaction)]
        when(innerSession.run("query")).thenReturn(result)
        when(innerSession.beginTransaction()).thenReturn(txs.first(), txs.last())

        session.run("query")
        session.run("query")

        verify(txs[0]).close()
    }

    @Test
    def void testClosesImplicitTransactionWhenClosed() {
        when(innerSession.run("query")).thenReturn(result)
        when(innerSession.beginTransaction()).thenReturn(tx)

        session.run("query")
        session.close()

        verify(tx).close()
        verify(innerSession).close()
    }

}
