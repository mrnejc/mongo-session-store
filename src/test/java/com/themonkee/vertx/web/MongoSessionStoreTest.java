package com.themonkee.vertx.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Session;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * how the hell do you unit-test Vert.x!?!?
 * this is just a quick and dirty test to see if session gets written to local MongoDB
 *
 * uncomment the org.junit decorators to actually run
 */
public class MongoSessionStoreTest {
    private static Vertx vertx;
    private static MongoClient mongoClient;
    private static MongoSessionStore sessionStore;

    //@BeforeClass
    public static void init() {
        vertx = Vertx.vertx();
        mongoClient = MongoClient.createNonShared(vertx,
                new JsonObject().put("db_name","test"));

        sessionStore = MongoSessionStore.create(vertx, mongoClient)
            .setSessionTimeout(60);
        sessionStore.init();
    }

    //@AfterClass
    public static void close() {
        mongoClient.close();
    }

    //@Test
    public void testSessionSave() {
        // create empty session that will expire after 30 seconds
        Session session = sessionStore.createSession(60);
        assertNotNull(session);

        sessionStore.put(session, r -> {
            assertTrue(r.succeeded());
            assertNotNull(session.id());
        });

        // sleep current thread to give it time to write to DB and return with AsyncResult
        try { Thread.currentThread().sleep(10000); } catch (InterruptedException ignore) {}
    }
}
