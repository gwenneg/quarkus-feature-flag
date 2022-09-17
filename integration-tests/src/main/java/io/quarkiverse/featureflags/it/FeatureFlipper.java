package io.quarkiverse.featureflags.it;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.featureflags.FeatureFlags;

@FeatureFlags
public class FeatureFlipper {

    @ConfigProperty(name = "huge-feature.enabled", defaultValue = "false")
    boolean hugeFeatureEnabled;

    @ConfigProperty(name = "risky-feature.enabled", defaultValue = "false")
    boolean riskyFeatureEnabled;

    public boolean isHugeFeatureEnabled() {
        return hugeFeatureEnabled;
    }

    public boolean isRiskyFeatureEnabled() {
        return riskyFeatureEnabled;
    }
}
