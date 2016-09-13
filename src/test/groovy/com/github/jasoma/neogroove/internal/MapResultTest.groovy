package com.github.jasoma.neogroove.internal

import com.github.jasoma.graft.access.NeoNode
import com.github.jasoma.graft.access.NeoRelationship
import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.access.internal.MapResult
import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

class MapResultTest {

    def static seed = "CREATE (s:Test {location:'start'})-[r:Test {location:'middle'}]->(e:Test {location:'end'})"
    def static query = "MATCH (s:Test {location:'start'})-[r:Test {location:'middle'}]->(e:Test {location:'end'}) RETURN s,r,e"
    def static clean = "MATCH (s:Test)-[r:Test]-(e:Test) DELETE s,r,e"

    @ClassRule public static NeoServerConnection server = new NeoServerConnection()
    @ClassRule public static InMemoryDatabase embedded = new InMemoryDatabase(seed)

    @BeforeClass
    def static void prepare() {
        server.withSession { session -> session.run(seed).consume() }
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
