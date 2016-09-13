package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoNode
import com.github.jasoma.graft.access.NeoRelationship
import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

class MapResultTest {

    def static u = new Random().nextLong()
    def static seed = "CREATE (s:Test {location:'start', u: {u}})-[r:Test {location:'middle', u: {u}}]->(e:Test {location:'end', u: {u}})"
    def static query = "MATCH (s:Test {location:'start', u: $u})-[r:Test {location:'middle', u: $u}]->(e:Test {location:'end', u: $u}) RETURN s,r,e"
    def static clean = "MATCH (s:Test {u: $u})-[r:Test {u: $u}]-(e:Test {u: $u}) DELETE s,r,e"

    @ClassRule public static NeoServerConnection server = new NeoServerConnection()
    @ClassRule public static InMemoryDatabase embedded = new InMemoryDatabase()

    @BeforeClass
    def static void prepare() {
        server.withSession { session -> session.run(seed, [u: u]).consume() }
        embedded.withTransaction { db ->
            db.execute(seed, [u: u])
            success()
        }
    }

    @AfterClass
    def static void cleanup() {
        server.withSession { session -> session.run(clean).consume() }
    }

    @Test
    def void testWrapDriverResults() {
        server.withSession { session ->
            def results = session.run(query)
            def row = new MapResult(results.next().asMap())
            validateRow(row)
        }
    }

    @Test
    def void testWrapEmbeddedResults() {
        embedded.withTransaction { db ->
            def results = db.execute(query)
            def row = new MapResult(results.next())
            validateRow(row)
        }
    }

    private static void validateRow(ResultRow row) {
        assert row.s instanceof NeoNode
        assert row.s == row["s"]
        assert row.s == row.node("s")

        assert row.r instanceof NeoRelationship
        assert row.r == row["r"]
        assert row.r == row.relationship("r")

        assert row.e instanceof NeoNode
        assert row.e == row["e"]
        assert row.e == row.node("e")

        row.each { k, v ->  assert row[k] == v  }
    }

    @Test(expected = ResultRow.ResultTypeMismatch)
    def void testNodeTypeMismatch() {
        embedded.withTransaction { db ->
            def results = db.execute(query)
            def row = new MapResult(results.next())
            row.relationship("s")
        }
    }

    @Test(expected = ResultRow.ResultTypeMismatch)
    def void testRelationTypeMismatch() {
        embedded.withTransaction { db ->
            def results = db.execute(query)
            def row = new MapResult(results.next())
            row.node("r")
        }
    }

    @Test
    def void testGetRawValue() {
        def row = new MapResult(raw: "value")
        assert row.raw == "value"
        assert row["raw"] == "value"
        assert row.get("raw") == "value"
    }

    @Test
    def void testGetNullValue() {
        def row = new MapResult(node: null, rel: null, raw: null)
        assert row.raw == null
        assert row.node("node") == null
        assert row.relationship("rel") == null
    }

}
