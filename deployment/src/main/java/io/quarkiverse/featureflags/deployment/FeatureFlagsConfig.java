package io.quarkiverse.featureflags.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class FeatureFlagsConfig {

    /**
     * bla
     */
    @ConfigItem(defaultValue = "true")
    public boolean logAtStartup;

    /**
     * bla
     */
    @ConfigItem(defaultValue = "false")
    public boolean exposeRest;

    /**
     * bla
     */
    @ConfigItem(defaultValue = "featureflags")
    public String exportPath;
}
