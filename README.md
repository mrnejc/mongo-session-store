# MongoDB Session Store

## About

Small (java only) implementation of [SessionStore](http://vertx.io/docs/apidocs/io/vertx/ext/web/sstore/SessionStore.html)
that uses MongoDB backend for storing web client sessions. 

Code is based on *LocalSessionStore* implementation in [vertx-web](https://github.com/vert-x3/vertx-web).

## Usage

To use the session store first create MongoClient

    vertx = Vertx.vertx();
    mongoClient = MongoClient.createNonShared(vertx,
                                              new JsonObject().put("host", "localhost")
                                                              .put("port", 27017)
                                                              .put("db_name", "test));

and then pass the connection to create session store, if you want to change the name of session collection (default is 
"sessions") set it via configuration _JsonObject_ and setup _SessionHandler_ to use this store in handler 
    
    MongoSessionStore.create(vertx, mongoClient, new JsonObject.put("collection", "my_sessions" ).setHandler(r->{
      if(r.succeeded())
        sessionHandler = SessionHandler.create(r.result());
      else
        <... handle error ...>
    });


## Dependencies

This module is build against *vertx-web* and *vertx-mongo-client*. Make sure you add them to your *pom.xml*:

    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-web</artifactId>
      <version>3.3.2</version>
    </dependency>
    
    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-mongo-client</artifactId>
      <version>3.3.3</version>
    </dependency>

## License

Since *vertx-web* is provided under dual [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html) 
and [Apache License v2.0](http://www.opensource.org/licenses/apache2.0.php) this code is released under same conditions.  

[![](https://jitpack.io/v/mrnejc/mongo-session-store.svg)](https://jitpack.io/#mrnejc/mongo-session-store)
