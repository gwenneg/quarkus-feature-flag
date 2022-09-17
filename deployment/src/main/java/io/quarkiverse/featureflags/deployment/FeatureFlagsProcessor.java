package io.quarkiverse.featureflags.deployment;

import static io.quarkus.arc.processor.BuiltinScope.SINGLETON;
import static org.jboss.jandex.DotName.createSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkiverse.featureflags.FeatureFlags;
import io.quarkiverse.featureflags.FlagDescriptor;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.Startup;

class FeatureFlagsProcessor {

    public static final DotName FEATURE_FLAGS_NAME = createSimple(FeatureFlags.class.getName());
    public static final DotName CONFIG_PROPERTY_NAME = createSimple(ConfigProperty.class.getName());
    public static final DotName BOOLEAN_PRIMITIVE_NAME = createSimple(boolean.class.getName());
    public static final DotName FLAG_DESCRIPTOR_NAME = createSimple(FlagDescriptor.class.getName());
    public static final DotName STARTUP_NAME = createSimple(Startup.class.getName());

    private static final String FEATURE = "feature-flags";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationsTransformer() {
        return new AnnotationsTransformerBuildItem(new FeatureFlagsAnnotationsTransformer());
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope() {
        return AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(FEATURE_FLAGS_NAME)
                .defaultScope(SINGLETON)
                .build();
    }

    @BuildStep
    Optional<BytecodeTransformerBuildItem> bytecodeTransformer(CombinedIndexBuildItem combinedIndex) {
        for (AnnotationInstance featureFlagsAnnotation : combinedIndex.getIndex().getAnnotations(FEATURE_FLAGS_NAME)) {
            ClassInfo classInfo = featureFlagsAnnotation.target().asClass();
            List<FeatureFlag> featureFlagNames = new ArrayList<>();
            for (FieldInfo fieldInfo : classInfo.fields()) {
                // TODO Support Boolean.class.
                if (fieldInfo.type().name().equals(BOOLEAN_PRIMITIVE_NAME) && fieldInfo.hasAnnotation(CONFIG_PROPERTY_NAME)) {
                    FeatureFlag f = new FeatureFlag();
                    f.fieldName = fieldInfo.name();
                    f.configKey = fieldInfo.annotation(CONFIG_PROPERTY_NAME).value("name").asString();
                    if (fieldInfo.hasAnnotation(FLAG_DESCRIPTOR_NAME)
                            && !fieldInfo.annotation(FLAG_DESCRIPTOR_NAME).value("description").asString().isBlank()) {
                        f.description = fieldInfo.annotation(FLAG_DESCRIPTOR_NAME).value("description").asString();
                    }
                    featureFlagNames.add(f);
                }
            }
            BytecodeTransformerBuildItem bytecodeTransformer = new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(classInfo.toString())
                    .setVisitorFunction(new FeatureFlagsClassVisitor(classInfo.name(), featureFlagNames)).build();
            return Optional.of(bytecodeTransformer);
        }
        return Optional.empty();
    }

    // TODO Forbid flags without a default value.
    // TODO Forbid multiple @FeatureFlags annotations.
}
