package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.access.ResultSet
import com.github.jasoma.graft.access.TransactionHandler
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Result
/**
 * Wrapper around a {@link Result}.
 */
class EmbeddedResultSet implements ResultSet {

    private final Result result
    private final TransactionHandler implicitTransaction

    /**
     * Constructor.
     *
     * @param result the result from querying the embedded database.
     * @param implicitTransaction an implicit transaction to success when the result set is closed or exhausted.
     *                            Only present if the result was created from {@link com.github.jasoma.graft.access.NeoSession#run}
     */
    EmbeddedResultSet(Result result, TransactionHandler implicitTransaction = null) {
        this.result = result
        this.implicitTransaction = implicitTransaction
    }

    @Override
    boolean hasNext() {
        def hasNext = result.hasNext()
        if (implicitTransaction && !hasNext) {
            implicitTransaction.success()
            implicitTransaction.close()
        }
        return hasNext
    }

    @Override
    ResultRow next() {
        def map = [:]

        // each property access reads through to the embedded database and so must take place
        // within the transaction that fetched the results. to avoid issues where the result is
        // stored but it's properties not read until after the transaction closes we iterate once
        // over all the values here and and perform conversions to the graft types now instead of
        // allowing MapResult to do so lazily.
        result.next().each { k, v ->
            if (v instanceof Node) {
                map[k] = new NodeWrapper(v)
            }
            else if (v instanceof Relationship) {
                map[k] = new RelationWrapper(v)
            }
            else {
                map[k] = v
            }
        }

        return new MapResult(map, false)
    }

    @Override
    void close() throws Exception {
        implicitTransaction?.success()
        implicitTransaction?.close()
        result.close()
    }

    private static class CachedPropertiesNode implements Node {

        @Delegate private final Node node
        private final Map<String, Object> cachedProperties

        CachedPropertiesNode(Node node, Map<String, Object> cachedProperties) {
            this.node = node
            this.cachedProperties = cachedProperties
        }

        @Override
        Map<String, Object> getAllProperties() {
            return cachedProperties
        }
    }

    private static class CachedPropertiesRelationship implements Relationship {

        @Delegate private final Relationship relationship
        private final Map<String, Object> cachedProperties

        CachedPropertiesRelationship(Relationship relationship, Map<String, Object> cachedProperties) {
            this.relationship = relationship
            this.cachedProperties = cachedProperties
        }

        @Override
        Map<String, Object> getAllProperties() {
            return cachedProperties
        }
    }

}
