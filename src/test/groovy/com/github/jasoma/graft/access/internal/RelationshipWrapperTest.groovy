package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.ClassRule
import org.junit.Test
import org.neo4j.test.TestGraphDatabaseFactory

import static org.junit.Assert.*

class RelationshipWrapperTest {

    def static seed = "CREATE (s:Test)-[r:Test {string:'string', int:2, list:[true, false]}]->(e:Test) RETURN r"
    def static clean = "MATCH (s:Test)-[r:Test]->(e:Test) DELETE s,r,e"

    @ClassRule public static NeoServerConnection server = new NeoServerConnection()
    @ClassRule public static InMemoryDatabase embedded = new InMemoryDatabase()

    def params = [string: 'string', int: 2, list: [true, false]]

    @Test
    def void testDriver() {
        def session = server.session()
        def results = session.run(seed)

        def wrapped = new RelationWrapper(results.next().get("r").asRelationship())
        assertEquals("Test", wrapped.type())
        assertNotNull(wrapped.unwrap(org.neo4j.driver.v1.types.Relationship))

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

        def wrapped = new RelationWrapper(results.first()["r"] as org.neo4j.graphdb.Relationship)
        assertEquals("Test", wrapped.type())
        assertNotNull(wrapped.unwrap(org.neo4j.graphdb.Relationship))

        assertEquals(params["string"], wrapped["string"])
        assertEquals(params["int"], wrapped["int"])
        // embedded will return an array even if a list is passed in
        assertArrayEquals(params["list"] as boolean[], wrapped["list"])

        tx.close()
    }

}
