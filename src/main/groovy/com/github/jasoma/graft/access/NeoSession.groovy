package com.github.jasoma.graft.access

import com.github.jasoma.graft.access.internal.TransactionHandler
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

/**
 * A NeoSession is the main interaction point for executing commands against a neo4j database, whether via the driver or
 * the embedded database.
 * <p>
 * When the underlying access method is {@link org.neo4j.driver.v1.Driver} a NeoSession maps more or less directly to a
 * {@link org.neo4j.driver.v1.Session}.
 * <p>
 * The embedded database does not have the same concept of sessions as the driver. When this is the underlying access mode
 * the NeoSession handles creating transactions automatically and ensures they are closed when the session itself is closed.
 * <p>
 * <strong>A NeoSession is not thread safe.</strong> Any session should only be accessed by one thread at a time and multiple
 * sessions used if concurrent access is required.
 * <p>
 * The {@link NeoSession#run} convenience methods create an implicit transaction as is required by neo4j before executing the
 * query. This transaction will be maintained so long as the {@link ResultSet} remains open or has records remaining to be read.
 * A session will only maintain a single implicit transaction however and a second call to {@link NeoSession#run} will close any
 * previous transaction.
 */
interface NeoSession extends AutoCloseable {

    /**
     * Runs a query within an implicit transaction and returns the results. The transaction will remain open until the
     * {@link ResultSet} is closed or exhausted at which point {@link com.github.jasoma.graft.access.internal.TransactionHandler#success} will be called.
     *
     * @param query the cypher query to run.
     * @return the results of the query.
     */
    def ResultSet run(String query)

    /**
     * Runs a parameterized query within an implicit transaction and returns the results.  The transaction will remain open
     * until the {@link ResultSet} is closed or exhausted at which point {@link com.github.jasoma.graft.access.internal.TransactionHandler#success}
     * will be called.
     * <p>
     * Query parameters can be specified using named parameter syntax. Example:
     *
     * <pre><code>
     *     def results = session.run("MATCH (n:Test {name: {name}}) RETURN n", name: "Foo")
     * </code></pre>
     *
     * <p>
     * Alternatively a pre-constructed map can be provided. Due to how groovy handles named parameters the map must come
     * before the query in this case. Example:
     *
     * <pre><code>
     *     def params = [name: "Foo")
     *     def results = session.run(params, "MATCH (n:Test {name: {name}}) RETURN n")
     * </code></pre>
     *
     * @param parameters values to place into the parameterized query.
     * @param query the cypher query to run.
     * @return the results of the query.
     */
    def ResultSet run(Map parameters, String query)

    /**
     * Runs a closure isolated within a transaction. The transaction is passed the session object and can success or failure
     * the transaction at any point. Once the closure returns {@link com.github.jasoma.graft.access.internal.TransactionHandler#close} will be
     * closed regardless of whether or not an explicit call to {@link com.github.jasoma.graft.access.internal.TransactionHandler#success} or
     * {@link com.github.jasoma.graft.access.internal.TransactionHandler#failure} was made.
     * <p>
     * Example:
     *
     * <pre><code>
     *     session.withTransaction { session ->
     *          def results = session.run("CREATE (n:TEST)")
     *          // ...
     *          if (condition) {
     *              success()
     *          }
     *          else {
     *              failure()
     *          }
     *     }
     * </code></pre>
     * <p>
     * Nested transactions may be used but there are no parent-child semantics in this case. The success or failure of an outer transaction
     * will not effect the outcome of an inner transaction.
     *
     * @param closure the closure to run inside the transaction.
     */
    def void withTransaction(@DelegatesTo(TransactionHandler)
                             @ClosureParams(value = SimpleType, options = "com.github.jasoma.graft.access.NeoSession") Closure closure)

    /**
     * Begins a new transaction with the underlying database. If the transaction remains open when the session is closed the transaction
     * will be closed also. The transaction must always be explicitly closed, {@link TransactionHandler#success} and
     * {@link TransactionHandler#failure} do not themselves close the transaction.
     * <p>
     * This is a method is a convenience for Java users. Groovy code should make use of {@link NeoSession#withTransaction} instead.
     *
     * @return a handler for marking the transaction as success or failure.
     */
    def TransactionHandler beginTransaction()

}
