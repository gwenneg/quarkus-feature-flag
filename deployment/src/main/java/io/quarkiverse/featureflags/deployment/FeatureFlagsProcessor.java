package io.quarkiverse.featureflags.deployment;

import static io.quarkus.arc.processor.DotNames.SINGLETON;
import static java.util.stream.Collectors.toList;
import static org.jboss.jandex.DotName.createSimple;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkiverse.featureflags.FeatureFlags;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.StartupEvent;

class FeatureFlagsProcessor {

    public static final DotName CONFIG_PROPERTY_NAME = createSimple(ConfigProperty.class.getName());

    private static final String FEATURE = "feature-flags";
    private static final DotName FEATURE_FLAGS_NAME = createSimple(FeatureFlags.class.getName());
    private static final DotName BOOLEAN_PRIMITIVE_NAME = createSimple(boolean.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem beanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(FEATURE_FLAGS_NAME, SINGLETON);
    }

    @BuildStep
    void bytecodeTransformer(CombinedIndexBuildItem combinedIndex,
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem> observerConfigurationRegistry) {

        for (AnnotationInstance featureFlagsAnnotation : combinedIndex.getIndex().getAnnotations(FEATURE_FLAGS_NAME)) {
            ClassInfo classInfo = featureFlagsAnnotation.target().asClass();

            List<FieldInfo> flags = classInfo.fields().stream()
                    .filter(fieldInfo -> {
                        // TODO Support Boolean.class.
                        return fieldInfo.type().name().equals(BOOLEAN_PRIMITIVE_NAME)
                                && fieldInfo.hasAnnotation(CONFIG_PROPERTY_NAME);
                    })
                    .collect(toList());

            observerConfigurationRegistry.produce(
                    new ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem(observerRegistrationPhase.getContext()
                            .configure()
                            .beanClass(classInfo.name())
                            .observedType(StartupEvent.class)
                            .notify(new FeatureFlagsLoggingEnhancer(classInfo.toString(), flags))));

        }
    }

    // TODO Forbid flags without a default value.
    // TODO Forbid multiple @FeatureFlags annotations.
}
