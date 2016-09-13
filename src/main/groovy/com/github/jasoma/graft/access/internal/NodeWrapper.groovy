package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoNode
import com.github.jasoma.graft.access.ResultRow

/**
 * Wraps either a driver or an embedded node to the {@link NeoNode} interface.
 */
class NodeWrapper implements NeoNode {

    private def node
    private long id;
    private Map<String, Object> nodeProperties
    private Iterable<String> labels

    /**
     * Attempts to wrap an unknown object into a node. Fails if the object is not either of the supported
     * node types.
     *
     * @param object the object to attempt to wrap.
     * @return a new node wrapper instance.
     * @throws ResultRow.ResultTypeMismatch if the object is null or not a supported node type.
     */
    def static NodeWrapper wrap(Object object) throws ResultRow.ResultTypeMismatch {
        if (object instanceof NodeWrapper) {
            return object
        }
        if (object instanceof org.neo4j.driver.v1.types.Node) {
            return new NodeWrapper(object as org.neo4j.driver.v1.types.Node)
        }
        if (object instanceof org.neo4j.graphdb.Node) {
            return new NodeWrapper(object as org.neo4j.graphdb.Node)
        }
        throw new ResultRow.ResultTypeMismatch("Cannot wrap object ($object) into a node");
    }

    /**
     * Checks if an object can be wrapped as a node.
     *
     * @param object the object to check.
     * @return true if it can be wrapped.
     */
    def static boolean canWrap(Object object) {
        if (object instanceof NodeWrapper) {
            return true
        }
        if (object instanceof org.neo4j.driver.v1.types.Node) {
            return true
        }
        if (object instanceof org.neo4j.graphdb.Node) {
            return true
        }
        return false
    }

    /**
     * Construct the wrapper from a driver node.
     *
     * @param driverNode the driver node to wrap.
     */
    NodeWrapper(org.neo4j.driver.v1.types.Node driverNode) {
        this.node = driverNode
        this.id = driverNode.id()
        this.nodeProperties = driverNode.asMap()
        this.labels = driverNode.labels()
    }

    /**
     * Construct the wrapper from an embedded node.
     *
     * @param embeddedNode the embedded node to wrap.
     */
    NodeWrapper(org.neo4j.graphdb.Node embeddedNode) {
        this.node = embeddedNode
        this.id = embeddedNode.id
        this.nodeProperties = embeddedNode.allProperties;
        this.labels = embeddedNode.labels.collect { l -> l.name() }
    }

    def propertyMissing(String name) {
        if (nodeProperties.containsKey(name)) {
            return nodeProperties[name]
        }
        return node[name]
    }

    def propertyMissing(String name, def arg) {
        throw new UnsupportedOperationException("Cannot set values")
    }

    def methodMissing(String name, def args) {
        return node."$name"(args)
    }

    def asType(Class type) {
        return unwrap(type)
    }

    @Override
    Iterable<String> labels() {
        return labels
    }

    @Override
    long graphId() {
        return id
    }

    @Override
    Object get(String key) {
        return nodeProperties[key]
    }

    @Override
    Map<String, Object> properties() {
        return nodeProperties
    }

    @Override
    def <T> T unwrap(Class<T> type) {
        if (type.isInstance(node)) {
            return node as T
        }
        return null
    }

    @Override
    String toString() {
        return "Node(id: ${graphId()}, labels: $labels)"
    }
}
