package com.github.jasoma.graft.access

/**
 * Represents any object capable of running a cypher query against a neo4j database.
 */
interface NeoQueryRunner {

    /**
     * Runs a query within an implicit transaction and returns the results.
     *
     * @param query the cypher query to run.
     * @return the results of the query.
     */
    def ResultSet run(String query)

    /**
     * Runs a parameterized query within an implicit transaction and returns the results.
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

}
