package io.quarkiverse.featureflags.runtime;

import java.util.Map;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class FeatureFlagsHandler implements Handler<RoutingContext> {

    private FeatureFlagsExporter exporter;

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext
                .response()
                .putHeader("Content-Type", "application/json")
                .end(Buffer.buffer(export()));
    }

    private String export() {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, Object> flag : getExporter().getFlags().entrySet()) {
            jsonObject.put(flag.getKey(), flag.getValue());
        }
        return jsonObject.encode();
    }

    private FeatureFlagsExporter getExporter() {
        if (exporter == null) {
            exporter = Arc.container().instance(FeatureFlagsExporter.class).get();
        }
        return exporter;
    }
}
