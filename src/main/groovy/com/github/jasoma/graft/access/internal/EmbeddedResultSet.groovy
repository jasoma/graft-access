package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.ResultRow
import com.github.jasoma.graft.access.ResultSet
import org.neo4j.graphdb.Result

/**
 * Wrapper around a {@link Result}.
 */
class EmbeddedResultSet implements ResultSet {

    private final Result result;

    /**
     * Constructor.
     *
     * @param result the result from querying the embedded database.
     */
    EmbeddedResultSet(Result result) {
        this.result = result
    }

    @Override
    boolean hasNext() {
        return result.hasNext()
    }

    @Override
    ResultRow next() {
        return new MapResult(result.next())
    }
}
