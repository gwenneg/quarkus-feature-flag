package io.quarkiverse.featureflags.deployment.config;

import java.util.function.BooleanSupplier;

public class IsExposeRestEnabled implements BooleanSupplier {

    private FeatureFlagsConfig config;

    @Override
    public boolean getAsBoolean() {
        return config.exposeRest;
    }
}
