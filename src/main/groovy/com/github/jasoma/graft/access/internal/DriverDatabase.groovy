package com.github.jasoma.graft.access.internal

import com.github.jasoma.graft.access.NeoDatabase
import com.github.jasoma.graft.access.NeoSession
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.neo4j.driver.v1.Driver

/**
 * Created by jason on 9/14/16.
 */
class DriverDatabase implements NeoDatabase {

    private final Driver driver

    DriverDatabase(Driver driver) {
        this.driver = driver
    }

    @Override
    NeoSession session() {
        return new DriverSession(driver.session())
    }

    @Override
    void withSession(@ClosureParams(value = SimpleType.class, options = "com.github.jasoma.graft.access.NeoSession") Closure closure) {
        def session = session()
        try {
            closure(session)
        }
        finally {
            session.close()
        }
    }

    @Override
    Object asType(Class type) {
        if (type == Driver) {
            return driver
        }
        return null
    }

    @Override
    void close() throws Exception {
        driver.close()
    }
}
