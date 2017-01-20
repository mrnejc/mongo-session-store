package com.themonkee.vertx.web.impl;

import com.themonkee.vertx.web.MongoSessionStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Session;

import java.util.concurrent.TimeUnit;

/**
 * based on io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl
 * (https://github.com/vert-x3/vertx-web/blob/3.3.2/vertx-web/src/main/java/io/vertx/ext/web/sstore/impl/LocalSessionStoreImpl.java)
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
public class MongoSessionStoreImpl implements MongoSessionStore {

    private final MongoClient mongoClient;

    private String sessionCollection = "sessions";
    /**
     * defaults to 7 days
     */
    private long sessionTimeoutAfter = 7 * 24 * 60 * 60;

    public MongoSessionStoreImpl(Vertx ignore, MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }


    public MongoSessionStoreImpl(Vertx vertx, String mongoClientPoolName) {
        this(vertx, MongoClient.createShared(vertx, new JsonObject(), mongoClientPoolName));
    }


    @Override
    public long retryTimeout() {
        return 0;
    }

    @Override
    public Session createSession(long timeout) {
        return new SessionImpl(timeout);
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> handler) {
        this.mongoClient.findOne(this.sessionCollection,
                new JsonObject().put(SessionImpl.FIELD_ID, id),
                null,
                r -> handler.handle(Future.succeededFuture(new SessionImpl(id, sessionTimeoutAfter).fromJsonObject(r.result()))));
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Boolean>> handler) {
        this.mongoClient.removeDocument(this.sessionCollection,
                new JsonObject().put(SessionImpl.FIELD_ID, id),
                r -> handler.handle(Future.succeededFuture(r.succeeded())));
    }

    @Override
    public void put(Session session, Handler<AsyncResult<Boolean>> handler) {
        SessionImpl si = (SessionImpl) session;

        if(session.id() == null) {
            this.mongoClient.save(this.sessionCollection,
                    si.toJsonObject(),
                    r -> {
                        // result is an id
                        ((SessionImpl) session).setId(r.result());
                        handler.handle(Future.succeededFuture(r.succeeded()));
                    });
        } else {
            this.mongoClient.replaceDocuments(this.sessionCollection,
                    si.toJsonObject(),
                    new JsonObject().put(SessionImpl.FIELD_ID, session.id()),
                    r -> handler.handle(Future.succeededFuture(r.succeeded())));
        }
    }


    @Override
    public void clear(Handler<AsyncResult<Boolean>> handler) {
        this.mongoClient.removeDocuments(this.sessionCollection, new JsonObject(), c ->
            handler.handle(Future.succeededFuture(c.succeeded()))
        );
    }

    @Override
    public void size(Handler<AsyncResult<Integer>> handler) {
        this.mongoClient.count(this.sessionCollection, null, c -> {
            Long result = c.result();
            Integer x = result.intValue();
            handler.handle(Future.succeededFuture(x));
        });
    }

    @Override
    public void close() { /* nothing to do */ }

    @Override
    public MongoSessionStore setCollectionName(String collectionName) {
        this.sessionCollection = collectionName;
        return this;
    }

    @Override
    public MongoSessionStore setSessionTimeout(int sessionTimeout) {
        this.sessionTimeoutAfter = sessionTimeout;
        return this;
    }

    public Future<Void> init() {
        Future<Void> startFuture = Future.future();

        Future<Void> futCreateColl = Future.future();
        // try to create collection, if it is created or already exists its OK
        this.mongoClient.createCollection(this.sessionCollection, (AsyncResult<Void> res) -> {
            if(res.succeeded() || res.cause().getMessage().contains("collection already exists")) {
                futCreateColl.complete();
            } else {
                futCreateColl.fail(res.cause());
            }
        });

        futCreateColl.compose(v -> {
            // create the session expiry index
            // SessionImpl sets _expire field to Date when session document must be deleted on save
            // so we set expireAfterSeconds to 0 so its deleted when that Date is hit
            // see https://docs.mongodb.com/manual/tutorial/expire-data/
            this.mongoClient.createIndexWithOptions(this.sessionCollection,
                    new JsonObject().put(SessionImpl.FIELD_EXPIRE, 1),
                    new IndexOptions().expireAfter(0L, TimeUnit.SECONDS),
                    res -> {
                if(res.succeeded()) {
                    startFuture.complete();
                } else {
                    startFuture.fail(res.cause());
                }
            });
        }, startFuture);

        return startFuture;
    }
}
