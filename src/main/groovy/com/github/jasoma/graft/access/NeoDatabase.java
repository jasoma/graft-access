package com.github.jasoma.graft.access;

import com.github.jasoma.graft.access.internal.DriverDatabase;
import com.github.jasoma.graft.access.internal.EmbeddedDatabase;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import org.neo4j.driver.v1.Driver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * NeoDatabase is the primary database access point replacing {@link org.neo4j.driver.v1.Driver} and {@link org.neo4j.graphdb.GraphDatabaseService}.
 * A NeoDatabase instance is essentially just a source for creating session with all query execution being performed via {@link NeoSession} instances.
 * <p>
 * Closing a NeoDatabase will terminate the underlying database access method via either {@link org.neo4j.driver.v1.Driver#close} or
 * {@link org.neo4j.graphdb.GraphDatabaseService#shutdown}.
 * <p>
 * If access to the underlying database is required {@link NeoDatabase#asType} can be used to recover it using either Groovy casting:
 *
 * <pre><code>
 *     NeoDatabase neoDb = NeoDatabase.driver("bolt://localhost")
 *     Driver driver = neoDb as Driver
 * </code></pre>
 * <p>
 *
 * Or by calling the method directly in Java or Groovy code:
 *
 * <pre><code>
 *     NeoDatabase neoDb = NeoDatabase.inMemory()
 *     GraphDatabaseService graphDb = (GraphDatabaseService) neoDb.asType(GraphDatabaseService)
 * </code></pre>
 */
public interface NeoDatabase extends AutoCloseable {

    /**
     * Creates a new session. The session must be closed in order to avoid resource leaks.
     *
     * @return the newly created session.
     */
    NeoSession session();

    /**
     * Creates a new session and runs the provided closure with it. The session is guaranteed to be closed when the
     * closure exits.
     *
     * @param closure the closure to run with the session.
     */
    void withSession(@ClosureParams(value = SimpleType.class, options = "com.github.jasoma.graft.access.NeoSession") Closure closure);

    /**
     * Attempt to access the underlying database type. {@code null} will be returned if the given type does not match
     * the underlying access type.
     *
     * <pre><code>
     *     NeoDatabase neoDb = NeoDatabase.inMemory()
     *
     *     GraphDatabaseService graphDb = (GraphDatabaseService) neoDb.asType(GraphDatabaseService)
     *     assert graphDb != null
     *
     *     Driver driver = (Driver) neoDb.asType(Driver)
     *     assert driver == null
     * </code></pre>
     *
     * @param type the goal type
     * @return the underlying database type or null if the type does not match.
     */
    Object asType(Class type);

    /**
     * Wrap a driver connection as a {@link NeoDatabase}.
     *
     * @param driver the driver to wrap.
     * @return a NeoDatabase instance.
     */
    static NeoDatabase wrap(Driver driver) {
        return new DriverDatabase(driver);
    }

    /**
     * Wrap an embedded database as a {@link NeoDatabase}.
     *
     * @param embedded the database to wrap.
     * @return a NeoDatabase instance.
     */
    static NeoDatabase wrap(GraphDatabaseService embedded) {
        return new EmbeddedDatabase(embedded);
    }

    /**
     * Creates a new in memory embedded database and wraps it as a {@link NeoDatabase}.
     *
     * @return a NeoDatabase instance using in-memory storage.
     */
    static NeoDatabase inMemory() {
        return wrap(new TestGraphDatabaseFactory().newImpermanentDatabase());
    }

}
