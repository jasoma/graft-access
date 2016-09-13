package com.github.jasoma.graft.access

/**
 * Holds a single row of results from a cypher query. For example given:
 * <pre><code>
 *      MATCH (start)-[rel]->(end)
 * </code></pre>
 * A ResultRow from this query would be equivalent to {@code [start: NeoNode, rel: NeoRelation, end: NeoNode]}.
 * <p>
 * ResultRow contents can be accessed directly using {@link ResultRow#get} using the variable names from the query.
 * In addition methods are available to access elements in the result as a specific type.
 */
interface ResultRow {

    /**
     * Get a node from the result row.
     *
     * @param key the name of the variable in the query.
     * @return the node.
     * @throws ResultTypeMismatch if the value for the given key is not a node.
     */
    def NeoNode node(String key) throws ResultTypeMismatch

    /**
     * Get a relationship from the result row.
     *
     * @param key the name of the variable in the query.
     * @return the relationship.
     * @throws ResultTypeMismatch if the value for the given key is not a relationship.
     */
    def NeoRelationship relationship(String key) throws ResultTypeMismatch

    /**
     * Get a value from the result row.
     *
     * @param key the name of the variable in the query.
     * @return the result if one exists.
     */
    def Object get(String key)

    /**
     * Iterate over each entry in the result. The closure will be passed the key and the
     * value. Example:
     *
     * <pre><code>
     *     resultRow.each { key, value ->
     *          println("Result for $key is $value")
     *     }
     * </code></pre>
     *
     * @param closure the closure applied on each entry in the result row.
     */
    def void each(Closure closure)

    /**
     * Exception type for when {@link ResultRow#node} and {@link ResultRow#relationship} encounter a different
     * type then expected in the result.
     */
    public class ResultTypeMismatch extends RuntimeException {

        ResultTypeMismatch(String message) {
            super(message)
        }

        ResultTypeMismatch(String message, Throwable cause) {
            super(message, cause)
        }
    }
}
