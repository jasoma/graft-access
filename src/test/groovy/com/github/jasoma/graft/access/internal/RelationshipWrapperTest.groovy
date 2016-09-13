package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static org.junit.Assert.*

class RelationshipWrapperTest {

    def static u = new Random().nextLong()
    def static seed = "CREATE (s:Test {u: {u}})-[r:Test {string:'string', int:2, list:[true, false], u: {u}}]->(e:Test {u: {u}})"
    def static query = "MATCH (s:Test {u: $u})-[r:Test {u: $u}]->(e:Test {u: $u}) RETURN r"
    def static clean = "MATCH (s:Test {u: $u})-[r:Test {u: $u}]->(e:Test {u: $u}) DELETE s,r,e"

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
            def wrapped = new RelationWrapper(results.next().get("r").asRelationship())

            assertEquals("Test", wrapped.type())
            assertNotNull(wrapped.unwrap(org.neo4j.driver.v1.types.Relationship))
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
            def results = db.execute(query)
            def wrapped = new RelationWrapper(results.first()["r"] as org.neo4j.graphdb.Relationship)

            assertEquals("Test", wrapped.type())
            assertNotNull(wrapped.unwrap(org.neo4j.graphdb.Relationship))
            assertEquals(wrapped.properties(), wrapped as Map)

            assertEquals(params["string"], wrapped["string"])
            assertEquals(params["int"], wrapped["int"])
            // embedded will return an array even if a list is passed in
            assertArrayEquals(params["list"] as boolean[], wrapped["list"])
        }
    }

}
