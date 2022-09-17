package io.quarkiverse.featureflags.it;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.featureflags.FeatureFlags;

@FeatureFlags
public class FeatureFlipper {

    @ConfigProperty(name = "huge-feature.enabled", defaultValue = "false")
    boolean hugeFeatureEnabled;

    @ConfigProperty(name = "risky-feature.enabled", defaultValue = "false")
    boolean riskyFeatureEnabled;

    @ConfigProperty(name = "awesome-feature.enabled", defaultValue = "false")
    Boolean awesomeFeatureEnabled;

    public boolean isHugeFeatureEnabled() {
        return hugeFeatureEnabled;
    }

    public boolean isRiskyFeatureEnabled() {
        return riskyFeatureEnabled;
    }
}
