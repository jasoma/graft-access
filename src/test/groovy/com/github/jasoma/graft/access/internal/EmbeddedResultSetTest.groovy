package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.TransactionHandler
import com.github.jasoma.graft.test.InMemoryDatabase
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.neo4j.graphdb.Result

import static org.mockito.Mockito.*

class EmbeddedResultSetTest {

    def static u = new Random().nextLong()
    def static count = 3
    def static seed = "CREATE (s:Test {u: {u}, i: {i}})-[r:Test {u: {u}, i: {i}}]->(e:Test {u: {u}, i: {i}})"
    def static query = "MATCH (s:Test {u: $u})-[r:Test {u: $u}]->(e:Test {u: $u}) RETURN s,r,e"

    @ClassRule def public static InMemoryDatabase db = new InMemoryDatabase()

    @BeforeClass
    def static void prepare() {
        count.times { i ->
            db.withTransaction { db ->
                db.execute(seed, [u: u, i: i])
                success()
            }
        }
    }

    @Test
    def void testResultSet() {
        db.withTransaction { db ->
            def results = new EmbeddedResultSet(db.execute(query))
            def found = 0
            results.eachWithIndex{ row, i ->
                ['s', 'r', 'e'].each { elem ->
                    assert row[elem].u == u
                    assert row[elem].i >= 0 && row[elem].i < 3
                }
                found++
            }
            assert found == count
        }
    }

    @Test
    def void testClosesTxOnExhaustion() {
        def tx = mock(TransactionHandler)
        def rowIterator = [new HashMap(), new HashMap()].iterator()
        def dbResult = [
                next: { rowIterator.next() },
                hasNext: { rowIterator.hasNext() }
        ] as Result

        def results = new EmbeddedResultSet(dbResult, tx)
        results.each { /* no-op, just want to run the whole iterator */}

        verify(tx).success()
        verify(tx).close()
    }

    @Test
    def void testClosesTxOnClose() {
        def tx = mock(TransactionHandler)
        def results = new EmbeddedResultSet(mock(Result), tx)

        results.close()
        verify(tx).success()
        verify(tx).close()
    }

}
