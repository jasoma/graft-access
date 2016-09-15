package com.github.jasoma.graft.access.internal

import org.junit.Test
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Result

import static org.mockito.Mockito.*

class EmbeddedSessionTest {

    def GraphDatabaseService db = mock(GraphDatabaseService)
    def Result result = mock(Result)
    def Transaction tx = mock(Transaction)

    def session = new EmbeddedSession(db)

    @Test
    def void testRun() {
        when(db.execute("query")).thenReturn(result)
        when(db.beginTx()).thenReturn(tx)

        session.run("query")

        verify(db).beginTx()
        verify(db).execute("query")
    }

    @Test
    def void testRunWithParameters() {
        when(db.execute("query", [with: 'params'])).thenReturn(result)
        when(db.beginTx()).thenReturn(tx)

        session.run("query", with: 'params')

        verify(this.db).beginTx()
        verify(this.db).execute("query", [with: 'params'])
    }

    @Test
    def void testWithTransaction() {
        when(db.beginTx()).thenReturn(tx)

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
        when(db.execute("query")).thenReturn(result)
        when(db.beginTx()).thenReturn(txs.first(), txs.last())

        session.run("query")
        session.run("query")

        verify(txs[0]).close()
    }

    @Test
    def void testClosesImplicitTransactionWhenClosed() {
        when(db.execute("query")).thenReturn(result)
        when(db.beginTx()).thenReturn(tx)

        session.run("query")
        session.close()

        verify(tx).close()
    }

}
