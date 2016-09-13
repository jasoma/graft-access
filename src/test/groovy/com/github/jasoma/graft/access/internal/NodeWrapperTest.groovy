package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static org.junit.Assert.*

class NodeWrapperTest {

    def static u = new Random().nextLong()
    def static seed = "CREATE (n:Test {string:'string', int:2, list:[true, false], u: {u}}) RETURN n"
    def static query = "MATCH (n:Test {u: $u}) RETURN n"
    def static clean = "MATCH (n:Test {u: $u}) DELETE n"

    @ClassRule public static NeoServerConnection server = new NeoServerConnection()
    @ClassRule public static InMemoryDatabase embedded = new InMemoryDatabase()

    def params = [string: 'string', int: 2, list: [true, false]]

    @BeforeClass
    def static void prepare() {
        server.withSession { s-> s.run(seed, [u: u])}
        embedded.withTransaction { db ->
            db.execute(seed, [u: u])
            success()
        }
    }

    @AfterClass
    def static void cleanup() {
        server.withSession { s-> s.run(clean) }
    }

    @Test
    def void testDriver() {
        server.withSession { session ->
            def results = session.run(query)
            def wrapped = new NodeWrapper(results.next().get("n").asNode())

            assertEquals(["Test"], wrapped.labels().toList())
            assertNotNull(wrapped.unwrap(org.neo4j.driver.v1.types.Node))
            assertEquals(wrapped.properties(), wrapped as Map)

            assertEquals(params["string"], wrapped["string"])
            assertEquals(params["int"], wrapped["int"])
            // driver will return a list even if an array is passed in
            assertEquals(params["list"], wrapped["list"])

        }
    }

    @Test
    def void testEmbedded() {
        embedded.withTransaction { db ->
            def results = db.execute(query).toList()
            def wrapped = new NodeWrapper(results.first()["n"] as org.neo4j.graphdb.Node)

            assertEquals(["Test"], wrapped.labels().toList())
            assertNotNull(wrapped.unwrap(org.neo4j.graphdb.Node))
            assertEquals(wrapped.properties(), wrapped as Map)

            assertEquals(params["string"], wrapped["string"])
            assertEquals(params["int"], wrapped["int"])
            // embedded will return an array even if a list is passed in
            assertArrayEquals(params["list"] as boolean[], wrapped["list"])
        }
    }
}
