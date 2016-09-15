package com.github.jasoma.graft.access

import com.github.jasoma.graft.test.InMemoryDatabase
import com.github.jasoma.graft.test.NeoServerConnection
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized)
class NeoDatabaseTest {

    public static def u = new Random().nextLong()

    @ClassRule public static NeoServerConnection server = new NeoServerConnection()
    @ClassRule public static InMemoryDatabase emedded = new InMemoryDatabase()

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> dbTypes() {
        return [ [NeoDatabase.wrap(server)].toArray() , [NeoDatabase.wrap(emedded)].toArray() ]
    }

    @Parameterized.Parameter public NeoDatabase db;

    @AfterClass
    public static void cleanup() {
        server.withSession { s-> s.run("MATCH (s:Test {u: $u})-[r:Test {u: $u}]-(e:Test {u: $u}) DELETE s,r,e").consume() }
    }

    @Test
    def void testCreate() {
        db.withSession { s ->
            def results = s.run("CREATE (s:Test {u: $u})-[r:Test {u: $u}]->(e:Test {u: $u}) RETURN s,r,e")

            def list = results.toList()
            assert list.size() == 1

            def row = list.first()
            assert row.s instanceof NeoNode
            assert row.r instanceof NeoRelationship
            assert row.e instanceof NeoNode
            row.each { k, v -> assert v.u == u }
        }
    }

    @Test
    def void testMatch() {
        db.withSession { s ->
            s.run("CREATE (s:Test {u: $u, name:'testMatch'})-[r:Test {u: $u, name:'testMatch'}]->(e:Test {u: $u, name:'testMatch'})").close()
            def results = s.run("MATCH (s:Test {u: $u, name:'testMatch'})-[r:Test {u: $u, name:'testMatch'}]->(e:Test {u: $u, name:'testMatch'}) RETURN s,r,e")

            def list = results.toList()
            assert list.size() == 1

            def row = list.first()
            assert row.s instanceof NeoNode
            assert row.r instanceof NeoRelationship
            assert row.e instanceof NeoNode
            row.each { k, v ->
                assert v.u == u
                assert v.name == 'testMatch'
            }
        }
    }

    @Test
    def void testUpdateProperty() {
        db.withSession { s ->
            s.run("CREATE (s:Test {u: $u, name:'testUpdateProperty'})").close()
            def result = s.run("MATCH (s:Test {u: $u, name:'testUpdateProperty'}) SET s.updated = true RETURN s")

            def list = result.toList()
            assert list.size() == 1

            def row = list.first()
            assert row.s instanceof NeoNode
            assert row.s.name == 'testUpdateProperty'
            assert row.s.updated == true
        }
    }

    @Test
    def void testReturnProperty() {
        db.withSession { s ->
            def result = s.run("CREATE (s:Test {u: $u, name:'testReturnProperty'}) RETURN s.name")

            def list = result.toList()
            assert list.size() == 1

            def row = list.first()
            assert row['s.name']== 'testReturnProperty'
        }
    }

    @Test
    def void testWithTransactionSuccess() {
        db.withSession { s ->
            s.withTransaction { inner ->
                inner.run("CREATE (s:Test {u: $u, name:'testWithTransactionSuccess'})").close()
                success()
            }

            def result = s.run("MATCH (s:Test {u: $u, name:'testWithTransactionSuccess'}) RETURN s")
            def list = result.toList()
            assert list.size() == 1

            def row = list.first()
            assert row.s instanceof NeoNode
            assert row.s.name == 'testWithTransactionSuccess'
        }
    }

    @Test
    def void testWithTransactionFailure() {
        db.withSession { s ->
            s.withTransaction { inner ->
                inner.run("CREATE (s:Test {u: $u, name:'testWithTransactionFailure'})").close()
                failure()
            }

            def result = s.run("MATCH (s:Test {u: $u, name:'testWithTransactionFailure'}) RETURN s")
            def list = result.toList()
            assert list.isEmpty()
        }
    }

    @Test
    def void testWithTransactionWithUnclosedImplicitTx() {
        db.withSession { s ->

            def unclosed = s.run("CREATE (s:Test {u: $u, name:'testWithTransactionWithUnclosedImplicitTx'}) RETURN s")

            s.withTransaction { inner ->
                def result = inner.run("MATCH (s:Test {u: $u, name:'testWithTransactionWithUnclosedImplicitTx'}) RETURN s")
                def list = result.toList()

                // termination of the implicit transaction from 'unclosed' should stop the node being created.
                assert list.isEmpty()
            }
        }
    }

}
