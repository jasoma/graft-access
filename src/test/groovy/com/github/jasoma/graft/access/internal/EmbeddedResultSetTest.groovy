package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.test.InMemoryDatabase
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import java.time.Instant

class EmbeddedResultSetTest {

    def static u = Instant.now().toEpochMilli()
    def static count = 3
    def static seed = "CREATE (s:Test {u: {u}, i: {i}})-[r:Test {u: {u}, i: {i}}]->(e:Test {u: {u}, i: {i}})"
    def static query = "MATCH (s:Test {u: $u})-[r:Test {u: $u}]->(e:Test {u: $u}) RETURN s,r,e"

    @ClassRule def public static InMemoryDatabase db = new InMemoryDatabase()

    @BeforeClass
    def static void prepare() {
        3.times { i ->
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
            results.eachWithIndex{ row, i ->
                ['s', 'r', 'e'].each { elem ->
                    assert row[elem].u == u
                    assert row[elem].i >= 0 && row[elem].i < 3
                }
            }
        }
    }

}
