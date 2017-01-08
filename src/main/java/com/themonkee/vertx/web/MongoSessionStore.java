package com.themonkee.vertx.web;

import com.themonkee.vertx.web.impl.MongoSessionStoreImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * this file is based on io.vertx.ext.web.sstore.LocalSessionStore
 * (https://github.com/vert-x3/vertx-web/blob/3.3.2/vertx-web/src/main/java/io/vertx/ext/web/sstore/LocalSessionStore.java)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
public interface MongoSessionStore extends SessionStore {
    /**
     * Create a session store
     *
     * @param vertx  the Vert.x instance
     * @param mongoClient  client for accessing MongoDB
     * @return the session store
     */
    static MongoSessionStore create(Vertx vertx, MongoClient mongoClient) {
        return new MongoSessionStoreImpl(vertx, mongoClient);
    }

    /**
     * Create a session store
     *
     * @param vertx  the Vert.x instance
     * @param mongoClientPoolName  name for pool name if client was already created using provided vertx instance
     * @return the session store
     */
    static MongoSessionStore create(Vertx vertx, String mongoClientPoolName) {
        return new MongoSessionStoreImpl(vertx, mongoClientPoolName);
    }

    /**
     * Override collection name, defaults to 'sessions'
     *
     * @param collectionName  the name of collection to use
     * @return the session store
     */
    MongoSessionStore setCollectionName(String collectionName);

    /**
     * Set when the session expires
     *
     * @param sessionTimeout
     * @return
     */
    MongoSessionStore setSessionTimeout(int sessionTimeout);


    public Future<Void> init();
}
