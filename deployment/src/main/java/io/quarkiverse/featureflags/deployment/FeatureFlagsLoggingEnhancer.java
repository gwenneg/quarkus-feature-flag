package io.quarkiverse.featureflags.deployment;

import static io.quarkiverse.featureflags.deployment.FeatureFlagsProcessor.CONFIG_PROPERTY_NAME;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

/**
 * Generates
 *
 * <pre>
 * final FeatureFlipper featureFlipper = (FeatureFlipper) Arc.container()
 *         .instance((Class) FeatureFlipper.class, (Annotation[]) null).get();
 * final Logger logger = Logger.getLogger((Class) FeatureFlipper.class);
 * logger.info((Object) "=== Quarkus Feature Flags ===");
 * logger.infof("hugeFeatureEnabled | huge-feature.enabled | %s", (Object) featureFlipper.hugeFeatureEnabled);
 * logger.infof("riskyFeatureEnabled | risky-feature.enabled | %s", (Object) featureFlipper.riskyFeatureEnabled);
 * </pre>
 */
public class FeatureFlagsLoggingEnhancer implements Consumer<MethodCreator> {

    private final String className;
    private final List<FieldInfo> flags;

    public FeatureFlagsLoggingEnhancer(String className, List<FieldInfo> flags) {
        this.className = className;
        this.flags = flags;
    }

    @Override
    public void accept(MethodCreator mc) {
        ResultHandle featureFlagsBeanClass = mc.loadClass(className);

        ResultHandle containerHandle = mc
                .invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = mc.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class,
                        Class.class, Annotation[].class),
                containerHandle, featureFlagsBeanClass, mc.loadNull());
        ResultHandle getHandle = mc.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
        AssignableResultHandle featureFlagsClassHandle = mc
                .createVariable("L" + className.replace('.', '/') + ";");
        mc.assign(featureFlagsClassHandle, getHandle);

        ResultHandle getLoggerHandle = mc.invokeStaticMethod(
                ofMethod(Logger.class, "getLogger", Logger.class, Class.class), featureFlagsBeanClass);
        AssignableResultHandle loggerHandle = mc
                .createVariable("L" + Logger.class.getName().replace('.', '/') + ";");
        mc.assign(loggerHandle, getLoggerHandle);

        ResultHandle titleHandle = mc.load("=== Quarkus Feature Flags ===");
        mc.invokeVirtualMethod(ofMethod(Logger.class, "info", void.class, Object.class), loggerHandle,
                titleHandle);

        for (FieldInfo flag : flags) {
            ResultHandle patternHandle = mc.load(getFlagMessageFormat(flag));
            ResultHandle featureFlagFieldHandle = mc.readInstanceField(FieldDescriptor.of(flag),
                    featureFlagsClassHandle);
            mc.invokeVirtualMethod(
                    ofMethod(Logger.class, "infof", void.class, String.class, Object.class),
                    loggerHandle, patternHandle, featureFlagFieldHandle);
        }

        mc.returnValue(null);
    }

    private String getFlagMessageFormat(FieldInfo flag) {
        String fieldName = flag.name();
        String configKey = flag.annotation(CONFIG_PROPERTY_NAME).value("name").asString();
        return fieldName + " | " + configKey + " | %s";
    }
}
