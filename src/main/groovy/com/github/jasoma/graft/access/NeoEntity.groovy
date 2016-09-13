package com.github.jasoma.graft.access

/**
 * The neo4j Java driver and embedded mode present slightly different interfaces to the same basic
 * graph structures. This interface normalizes access to the basic data across the two APIs.
 */
interface NeoEntity {

    /**
     * @return the graph id of the entity.
     */
    def long graphId();

    /**
     * Access a property by name.
     *
     * @param key the name of the property.
     * @return the value of the property or null if there is no property with that name.
     */
    def Object get(String key);

    /**
     * @return all nodeProperties of the entity.
     */
    def Map<String, Object> properties();

    /**
     * Attempts to unwrap the entity to its underlying driver/embedded type.
     *
     * @param type the underlying type to unwrap to.
     * @return the wrapped entity instance or null if the type does not match.
     */
    def <T> T unwrap(Class<T> type);

}
