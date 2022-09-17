package io.quarkiverse.featureflags.deployment;

import static io.quarkus.arc.processor.DotNames.SINGLETON;
import static java.util.stream.Collectors.toList;
import static org.jboss.jandex.DotName.createSimple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkiverse.featureflags.FeatureFlags;
import io.quarkiverse.featureflags.runtime.FeatureFlagsExporter;
import io.quarkiverse.featureflags.runtime.FeatureFlagsHandler;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

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
    void bytecodeTransformer(FeatureFlagsConfig config, CombinedIndexBuildItem combinedIndex,
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem> observerConfigurationRegistry) {

        if (config.logAtStartup) {
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
                        new ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem(
                                observerRegistrationPhase.getContext()
                                        .configure()
                                        .beanClass(classInfo.name())
                                        .observedType(StartupEvent.class)
                                        .notify(new FeatureFlagsLoggerCreator(classInfo.toString(), flags))));

            }
        }
    }

    @BuildStep
    void route(FeatureFlagsConfig config, BuildProducer<RouteBuildItem> routes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        if (config.exposeRest) {
            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(config.exportPath)
                    .routeConfigKey("quarkus.feature-flags.export-path")
                    .handler(new FeatureFlagsHandler())
                    .blockingRoute()
                    .build());
        }
    }

    @BuildStep
    void registerProvidersFromAnnotations(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        String annotationRegisteredProvidersImpl = FeatureFlagsExporter.class.getName() + "Implementation";

        try (ClassCreator classCreator = ClassCreator.builder()
                .className(annotationRegisteredProvidersImpl)
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                .interfaces(FeatureFlagsExporter.class)
                .build()) {

            classCreator.addAnnotation(ApplicationScoped.class);

            MethodCreator getFlags = classCreator
                    .getMethodCreator(MethodDescriptor.ofMethod(annotationRegisteredProvidersImpl, "getFlags", Map.class));

            ResultHandle hashMap = getFlags.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

            AssignableResultHandle map = getFlags.createVariable(Map.class);
            getFlags.assign(map, hashMap);

            ResultHandle key = getFlags.load("hello");
            ResultHandle value = getFlags.load("world");

            getFlags.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class), map, key,
                    value);

            getFlags.returnValue(map);
        }

        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(annotationRegisteredProvidersImpl));
    }

    // TODO Forbid flags without a default value.
    // TODO Forbid multiple @FeatureFlags annotations.
}
