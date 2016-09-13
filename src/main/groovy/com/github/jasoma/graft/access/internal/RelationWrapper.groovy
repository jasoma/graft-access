package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoRelationship
import com.sun.javaws.exceptions.InvalidArgumentException

/**
 * Wraps either a driver or an embedded relationship to the {@link NeoRelationship} interface.
 */
class RelationWrapper implements NeoRelationship {

    private def relation
    private long id
    private String type
    private Map<String, Object> properties

    /**
     * Attempts to wrap an unknown object into a relationship. Fails if the object is not either of the supported
     * relationship types.
     *
     * @param object the object to attempt to wrap.
     * @return a new relation wrapper instance.
     * @throws InvalidArgumentException if the object is null or not a supported relation type.
     */
    def static RelationWrapper wrap(Object object) throws InvalidArgumentException {
        if (object instanceof RelationWrapper) {
            return object
        }
        if (object instanceof org.neo4j.driver.v1.types.Relationship) {
            return new RelationWrapper(object as org.neo4j.driver.v1.types.Relationship)
        }
        if (object instanceof org.neo4j.graphdb.Relationship) {
            return new RelationWrapper(object as org.neo4j.graphdb.Relationship)
        }
        throw new InvalidArgumentException("Cannot wrap object ($object) into a relationship");
    }

    /**
     * Checks if an object can be wrapped as a relationship.
     *
     * @param object the object to check.
     * @return true if it can be wrapped.
     */
    def static boolean canWrap(Object object) {
        if (object instanceof RelationWrapper) {
            return true
        }
        if (object instanceof org.neo4j.driver.v1.types.Relationship) {
            return true
        }
        if (object instanceof org.neo4j.graphdb.Relationship) {
            return true
        }
        return false
    }

    /**
     * Construct the wrapper from a driver relationship.
     *
     * @param driverRelation the driver relationship to wrap.
     */
    RelationWrapper(org.neo4j.driver.v1.types.Relationship driverRelation) {
        this.relation = driverRelation
        this.id = driverRelation.id()
        this.type = driverRelation.type()
        this.properties = driverRelation.asMap()
    }

    /**
     * Construct the wrapper from an embedded relationship.
     *
     * @param embeddedRelationship the embedded relationship to wrap.
     */
    RelationWrapper(org.neo4j.graphdb.Relationship embeddedRelationship) {
        this.relation = embeddedRelationship
        this.id = embeddedRelationship.id
        this.type = embeddedRelationship.type.name()
        this.properties = embeddedRelationship.allProperties
    }

    def propertyMissing(String name) {
        if (properties.containsKey(name)) {
            return properties[name]
        }
        return relation[name]
    }

    def propertyMissing(String name, def arg) {
        throw new UnsupportedOperationException("Cannot set values")
    }

    def methodMissing(String name, def args) {
        return relation."$name"(args)
    }

    def asType(Class type) {
        return unwrap(type)
    }

    @Override
    String type() {
        return type
    }

    @Override
    long graphId() {
        return id
    }

    @Override
    Object get(String key) {
        return properties[key]
    }

    @Override
    Map<String, Object> properties() {
        return properties
    }

    @Override
    def <T> T unwrap(Class<T> type) {
        if (type.isInstance(relation)) {
            return relation as T
        }
        return null
    }

    @Override
    String toString() {
        return "Relationship(id: ${graphId()}, type: $type)"
    }
}
