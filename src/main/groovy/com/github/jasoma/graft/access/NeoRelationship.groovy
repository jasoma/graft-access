package com.github.jasoma.graft.access

/**
 * Normalized interface for Relationships across the driver and embedded neo4j APIs.
 */
interface NeoRelationship extends NeoEntity {

    /**
     * @return the type of the relationship.
     */
    def String type();

}
