package com.themonkee.vertx.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Session;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

/**
 * test require running MongoDB instance on localhost:27017
 */
@RunWith(VertxUnitRunner.class)
public class MongoSessionStoreTest {
    private static Vertx vertx;
    private static MongoClient mongoClient;
    private static MongoSessionStore sessionStore;

    @BeforeClass
    public static void init(TestContext context) {
        vertx = Vertx.vertx();
        mongoClient = MongoClient.createNonShared(vertx,
                new JsonObject().put("db_name","test"));

        sessionStore = MongoSessionStore.create(vertx, mongoClient)
            .setSessionTimeout(60);
        sessionStore.init();
    }

    @AfterClass
    public static void close(TestContext context) {
        mongoClient.close();
    }

    @Test
    public void testSessionSaveAndRestore(TestContext context) {
        Async async = context.async();

        // create a session that will expire after 60 seconds
        Session sessionS = sessionStore.createSession(60);
        context.assertNotNull(sessionS);

        String strKey = "string_key";
        sessionS.put(strKey, "StrValue");

        String intKey = "int_key";
        sessionS.put(intKey, Integer.MAX_VALUE);

        String longKey = "long_key";
        sessionS.put(longKey, Long.MAX_VALUE);

        String instKey = "instant_key";
        sessionS.put(instKey, Instant.now());

        sessionStore.put(sessionS, r1 ->{
            context.assertTrue(r1.succeeded());
            context.assertNotNull(sessionS.id());
            String id = sessionS.id();

            // fetch the session from
            sessionStore.get(id, r2 -> {
                context.assertTrue( r2.succeeded());
                Session sessionR = r2.result();
                context.assertEquals(sessionS.get(strKey), sessionR.get(strKey));
                context.assertEquals(sessionS.get(intKey), sessionR.get(intKey));
                context.assertEquals(sessionS.get(longKey), sessionR.get(longKey));
                context.assertEquals(sessionS.get(instKey), sessionR.get(instKey));

                async.complete();
            });
        });
    }
}
