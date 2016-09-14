package com.github.jasoma.graft.access.internal

/**
 * Common interface over the driver and embedded specific Transaction classes.
 *
 * @see org.neo4j.graphdb.Transaction
 * @see org.neo4j.driver.v1.Transaction
 */
interface TransactionHandler extends AutoCloseable {

    /**
     * Commit the underlying transaction.
     */
    def void success()

    /**
     * Rollback the underlying transaction.
     */
    def void failure()

}
