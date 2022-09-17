package io.quarkiverse.featureflags.it;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.featureflags.FeatureFlags;
import io.quarkiverse.featureflags.FlagDescriptor;

@FeatureFlags
public class FeatureFlipper {

    @FlagDescriptor(description = "coucou")
    @ConfigProperty(name = "my-flag.enabled", defaultValue = "false")
    boolean myFlagEnabled;

    @ConfigProperty(name = "my-flag.enabled2", defaultValue = "false")
    boolean myFlagEnabledtototutututu;
}
