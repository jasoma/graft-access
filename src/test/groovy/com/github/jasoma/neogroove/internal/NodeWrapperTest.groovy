package com.github.jasoma.neogroove.internal

import com.github.jasoma.graft.access.internal.NodeWrapper
import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.ClassRule
import org.junit.Test
import org.neo4j.test.TestGraphDatabaseFactory

import static org.junit.Assert.*

class NodeWrapperTest {

    def static seed = "CREATE (n:Test {string:'string', int:2, list:[true, false]}) RETURN n"
    def static clean = "MATCH (n:Test) DELETE n"

    @ClassRule public static NeoServerConnection server = new NeoServerConnection()
    @ClassRule public static InMemoryDatabase embedded = new InMemoryDatabase()

    def params = [string: 'string', int: 2, list: [true, false]]

    @Test
    def void testDriver() {
        def session = server.session()
        def results = session.run(seed)

        def wrapped = new NodeWrapper(results.next().get("n").asNode())
        assertEquals(["Test"], wrapped.labels().toList())
        assertNotNull(wrapped.unwrap(org.neo4j.driver.v1.types.Node))

        assertEquals(params["string"], wrapped["string"])
        assertEquals(params["int"], wrapped["int"])
        // driver will return a list even if an array is passed in
        assertEquals(params["list"], wrapped["list"])

        session.run(clean)
    }

    @Test
    def void testEmbedded() {
        def db = new TestGraphDatabaseFactory().newImpermanentDatabase()
        def tx = db.beginTx()
        def results = db.execute(seed).toList()

        def wrapped = new NodeWrapper(results.first()["n"] as org.neo4j.graphdb.Node)
        assertEquals(["Test"], wrapped.labels().toList())
        assertNotNull(wrapped.unwrap(org.neo4j.graphdb.Node))

        assertEquals(params["string"], wrapped["string"])
        assertEquals(params["int"], wrapped["int"])
        // embedded will return an array even if a list is passed in
        assertArrayEquals(params["list"] as boolean[], wrapped["list"])

        tx.close()
    }
}
