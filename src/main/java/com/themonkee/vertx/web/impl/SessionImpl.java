package com.themonkee.vertx.web.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Session;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * extension of {@link io.vertx.ext.web.sstore.impl.SessionImpl} because:
 * <ul>
 *   <li>we need to set <i>id</i> after creation of Session (we use MongoDB Document id)</li>
 *   <li>we need methods to convert Session to JSON</li>
 * </ul>
 */
public class SessionImpl extends io.vertx.ext.web.sstore.impl.SessionImpl {
    static final String FIELD_ID = "_id";
    static final String FIELD_EXPIRE = "_expires";

    private String id;
    private long sessionTimeoutAfter;

    SessionImpl(long sessionTimeoutAfter) {
        this(null, sessionTimeoutAfter);
    }

    SessionImpl(String id, long sessionTimeoutAfter) {
        this.id = id;
        this.sessionTimeoutAfter = sessionTimeoutAfter;
    }

    @Override
    public String id() {
        return this.id;
    }

    void setId(String id) {
        this.id = id;
    }

    @Override
    public Session put(String key, Object obj) {
        // a small defense
        if(key.equals(FIELD_ID) || key.equals(FIELD_EXPIRE)) {
            return this;
        } else {
            return super.put(key, obj);
        }
    }

    JsonObject toJsonObject() {
        JsonObject jo = new JsonObject();
        if (this.id() != null)
            jo.put(FIELD_ID, this.id());
        Map<String, Object> data = this.data();
        Object d;
        for (String key : data.keySet()) {
            d = data.get(key);
            if(d instanceof Instant) {
                // JsonObject only accepts Instant object for date
                // have to wrap it with $date object or it will be saved as String and not as ISODate
                // thnx to Milton Loayza in vert.x Google group
                // https://groups.google.com/d/msg/vertx/a0yeLL23GyQ/Dn7Gs8J47K0J
                jo.put(key, new JsonObject().put("$date", d));
            } else {
                jo.put(key, d);
            }
        }

        // see above
        jo.put(FIELD_EXPIRE,
                new JsonObject().put("$date", LocalDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(this.sessionTimeoutAfter)
                        .toInstant(ZoneOffset.UTC)));

        return jo;
    }

    /**
     * restore session object from JsonObject
     *
     * @param jsonObj data to restore
     * @return self for easy chaining
     */
    SessionImpl fromJsonObject(JsonObject jsonObj) {
        for(String f: jsonObj.fieldNames()) {
            if(f.equals(FIELD_ID)) {
                this.setId(jsonObj.getString(f));
            } else if(!f.equals(FIELD_EXPIRE)) {
                Object o = jsonObj.getValue(f);
                // see comment in toJsonObject
                if(o instanceof JsonObject && ((JsonObject)o).containsKey("$date")) {
                    this.put(f, ((JsonObject)o).getInstant("$date"));
                } else {
                    this.put(f, o);
                }
            }
        }
        return this;
    }
}
