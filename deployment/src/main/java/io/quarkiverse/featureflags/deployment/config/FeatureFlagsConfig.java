package io.quarkiverse.featureflags.deployment.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * TODO
 */
@ConfigRoot
public class FeatureFlagsConfig {

    /**
     * TODO
     */
    @ConfigItem(defaultValue = "true")
    public boolean logAtStartup;

    /**
     * TODO
     */
    @ConfigItem(defaultValue = "false")
    public boolean exposeRest;

    /**
     * TODO
     */
    @ConfigItem(defaultValue = "featureflags")
    public String exportPath;
}
