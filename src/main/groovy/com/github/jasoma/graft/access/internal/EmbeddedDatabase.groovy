package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoDatabase
import com.github.jasoma.graft.access.NeoSession
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.neo4j.graphdb.GraphDatabaseService

/**
 * Created by jason on 9/14/16.
 */
class EmbeddedDatabase implements NeoDatabase {

    private final GraphDatabaseService db

    EmbeddedDatabase(GraphDatabaseService db) {
        this.db = db
    }

    @Override
    NeoSession session() {
        return new EmbeddedSession(db)
    }

    @Override
    void withSession(@ClosureParams(value = SimpleType.class, options = "com.github.jasoma.graft.access.NeoSession") Closure closure) {
        def session = session()
        try {
            closure(session)
        }
        finally {
            session.close()
        }
    }

    @Override
    Object asType(Class type) {
        if (type == GraphDatabaseService) {
            return db
        }
        return null
    }

    @Override
    void close() throws Exception {
        db.shutdown()
    }
}
