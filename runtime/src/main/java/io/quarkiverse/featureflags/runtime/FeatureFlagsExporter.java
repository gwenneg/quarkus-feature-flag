package io.quarkiverse.featureflags.runtime;

import java.util.Map;

public interface FeatureFlagsExporter {

    Map<String, Object> getFlags();
}
