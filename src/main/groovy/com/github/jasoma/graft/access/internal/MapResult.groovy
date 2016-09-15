package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoNode
import com.github.jasoma.graft.access.NeoRelationship
import com.github.jasoma.graft.access.ResultRow

/**
 * Converts a query result from either the diver or embedded database into {@link ResultRow} by
 * wrapping the map representation directly.
 */
class MapResult implements ResultRow {

    private Map<String, Object> resultMap;

    /**
     * Construct the wrapper.
     *
     * @param resultMap the query results in map form.
     * @param copyValues whether or not a copy should be made of the resultMap, this should be true anytime
     *                   the map comes directly from the database as it will be mutated internally.
     */
    MapResult(Map<String, Object> resultMap, boolean copyValues = true) {
        this.resultMap = new HashMap<>(resultMap)
    }

    def propertyMissing(String name) {
        if (resultMap.containsKey(name)) {
            return get(name)
        }
        throw new NoSuchElementException("No result entry for key $name")
    }

    @Override
    NeoNode node(String key) {
        return (NeoNode) resultMap.computeIfPresent(key, this.&wrapNode)
    }

    private NeoNode wrapNode(String key, Object value) {
        try {
            def node = NodeWrapper.wrap(value)
            resultMap[key] = node
            return node
        }
        catch (Exception e) {
            throw new ResultRow.ResultTypeMismatch("Value [${value.class}]($value) at key '$key' cannot be converted to a node", e)
        }
    }

    @Override
    NeoRelationship relationship(String key) {
        return (NeoRelationship) resultMap.computeIfPresent(key, this.&wrapRelationship)
    }

    private NeoRelationship wrapRelationship(String key, Object value) {
        try {
            def relation = RelationWrapper.wrap(value)
            resultMap[key] = relation
            return relation
        }
        catch (Exception e) {
            throw new ResultRow.ResultTypeMismatch("Value [${value.class}]($value) at key '$key' cannot be converted to a relationship", e)
        }
    }

    @Override
    def get(String key) {
        def value = resultMap[key]

        if (value == null) {
            return null
        }
        else if (NodeWrapper.canWrap(value)) {
            return wrapNode(key, value)
        }
        else if (RelationWrapper.canWrap(value)) {
            return wrapRelationship(key, value)
        }
        else {
            return value
        }
    }

    @Override
    def void each(Closure closure) {
        resultMap.each { key, value ->
            def transformed = get(key)
            closure(key, transformed)
        }
    }
}
