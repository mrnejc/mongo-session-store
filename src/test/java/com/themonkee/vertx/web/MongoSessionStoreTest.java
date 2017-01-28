package com.themonkee.vertx.web;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
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
    private static HttpServer server;

    @BeforeClass
    public static void init(TestContext context) {
        Async async = context.async();

        vertx = Vertx.vertx();
        mongoClient = MongoClient.createNonShared(vertx,
                new JsonObject().put("db_name","test"));

        server = vertx.createHttpServer();

        MongoSessionStore.create(vertx, mongoClient, null).setHandler(r->{
            context.assertTrue(r.succeeded());
            sessionStore = r.result();
            async.complete();
        });
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

    @Test
    public void testSessionHandler(TestContext context) {
        Async async = context.async();

        Router router = Router.router(vertx);
        router.route().handler(CookieHandler.create());
        SessionHandler sessionHandler = SessionHandler.create(sessionStore)
                // this security warning is not needed for tests
                .setNagHttps(false)
                // set expire after 60 seconds
                .setSessionTimeout(60);

        router.route().handler(sessionHandler);
        router.route("/").handler(routingContext->{
            Session s = routingContext.session();
            context.assertNotNull(s);

            s.put("testKey","testValue");
            context.assertEquals("testValue", s.get("testKey"));
            routingContext.response().end("foo");
        });

        server.requestHandler(router::accept).listen(8080);

        HttpClient client = vertx.createHttpClient();
        client.getNow(8080, "localhost", "/", r->{
            client.close();
            async.complete();
        });
    }
}
