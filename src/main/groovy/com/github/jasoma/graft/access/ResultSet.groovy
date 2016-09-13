package com.github.jasoma.graft.access

/**
 * The collected results of querying a database. Like the underlying driver and embedded classes being
 * used a ResultSet will lazily fetch records from the database. A ResultSet is only accessible while
 * the {@link NeoSession} it was generated from remains open.
 *
 * @see org.neo4j.driver.v1.StatementResult
 * @see org.neo4j.graphdb.Result
 * @see ResultRow
 */
interface ResultSet extends Iterator<ResultRow> { }
