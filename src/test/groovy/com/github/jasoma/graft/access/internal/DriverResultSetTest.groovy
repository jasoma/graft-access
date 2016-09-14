package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

class DriverResultSetTest {

    def static u = new Random().nextLong()
    def static count = 3
    def static seed = "CREATE (s:Test {u: {u}, i: {i}})-[r:Test {u: {u}, i: {i}}]->(e:Test {u: {u}, i: {i}})"
    def static query = "MATCH (s:Test {u: $u})-[r:Test {u: $u}]->(e:Test {u: $u}) RETURN s,r,e"

    @ClassRule def public static NeoServerConnection server = new NeoServerConnection()

    @BeforeClass
    def static void prepare() {
        count.times { i ->
            server.withSession { s -> s.run(seed, [u: u, i: i]) }
        }
    }

    @AfterClass
    def static void cleanup() {
        server.withSession { s-> s.run("MATCH (s:Test {u: $u})-[r:Test {u: $u}]-(e:Test {u: $u}) DELETE s,r,e")}
    }

    @Test
    def void testResultSet() {
        server.withSession { s ->
            def results = new DriverResultSet(s.run(query))
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
        server.withSession { s ->
            def tx = mock(TransactionHandler)
            def results = new DriverResultSet(s.run(query), tx)
            results.each { /* no-op, just want to run the whole iterator */}

            verify(tx).success()
            verify(tx).close()
        }
    }

    @Test
    def void testClosesTxOnClose() {
        server.withSession { s ->
            def tx = mock(TransactionHandler)
            def results = new DriverResultSet(s.run(query), tx)
            results.close()

            verify(tx).success()
            verify(tx).close()
        }
    }

}
