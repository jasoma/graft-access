package com.github.jasoma.graft.access

/**
 * Normalized interface for Nodes across the driver and embedded neo4j APIs.
 */
interface NeoNode extends NeoEntity {

    /**
     * @return all labels present on the node.
     */
    def Iterable<String> labels();

}
