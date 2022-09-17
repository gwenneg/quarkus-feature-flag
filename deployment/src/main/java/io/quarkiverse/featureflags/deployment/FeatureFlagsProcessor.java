package io.quarkiverse.featureflags.deployment;

import static io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import static io.quarkus.arc.processor.DotNames.SINGLETON;
import static java.util.stream.Collectors.toList;
import static org.jboss.jandex.DotName.createSimple;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkiverse.featureflags.FeatureFlags;
import io.quarkiverse.featureflags.deployment.config.FeatureFlagsConfig;
import io.quarkiverse.featureflags.deployment.config.IsExposeRestEnabled;
import io.quarkiverse.featureflags.deployment.config.IsLogAtStartupEnabled;
import io.quarkiverse.featureflags.deployment.config.ShouldFindFeatureFlags;
import io.quarkiverse.featureflags.runtime.FeatureFlagsExporter;
import io.quarkiverse.featureflags.runtime.FeatureFlagsHandler;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

class FeatureFlagsProcessor {

    public static final DotName CONFIG_PROPERTY = createSimple(ConfigProperty.class.getName());

    private static final String FEATURE = "feature-flags";
    private static final DotName FEATURE_FLAGS = createSimple(FeatureFlags.class.getName());
    private static final List<DotName> BOOLEAN = List.of(createSimple(boolean.class.getName()),
            createSimple(Boolean.class.getName()));

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem beanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(FEATURE_FLAGS, SINGLETON);
    }

    @BuildStep(onlyIf = ShouldFindFeatureFlags.class)
    void featureFlags(CombinedIndexBuildItem combinedIndex, BuildProducer<FeatureFlagsBuildItem> featureFlagsProducer) {

        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(FEATURE_FLAGS);
        switch (annotations.size()) {
            case 0:
                // Do nothing.
                break;
            case 1:
                ClassInfo classInfo = annotations.iterator().next().target().asClass();
                List<FieldInfo> fieldInfos = classInfo.fields().stream()
                        .filter(fieldInfo -> {
                            // TODO Forbid flags without a default value.
                            return BOOLEAN.contains(fieldInfo.type().name()) && fieldInfo.hasAnnotation(CONFIG_PROPERTY);
                        })
                        .collect(toList());
                featureFlagsProducer.produce(new FeatureFlagsBuildItem(classInfo, fieldInfos));
                break;
            default:
                throw new DeploymentException("@" + FeatureFlags.class.getName() + " is only allowed on one class");
        }
    }

    @BuildStep(onlyIf = IsLogAtStartupEnabled.class)
    void observerConfigurator(Optional<FeatureFlagsBuildItem> featureFlags,
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem> observerConfiguratorProducer) {

        if (featureFlags.isEmpty()) {
            return;
        }
        observerConfiguratorProducer.produce(new ObserverConfiguratorBuildItem(
                observerRegistrationPhase.getContext()
                        .configure()
                        .beanClass(featureFlags.get().getClassInfo().name())
                        .observedType(StartupEvent.class)
                        .notify(FeatureFlagsBytecodeGenerator.logAtStartup(featureFlags.get()))));
    }

    @BuildStep(onlyIf = IsExposeRestEnabled.class)
    void generatedBeanAndRoute(Optional<FeatureFlagsBuildItem> featureFlags, FeatureFlagsConfig config,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer, NonApplicationRootPathBuildItem nonApplicationRootPath,
            BuildProducer<RouteBuildItem> routeProducer) {

        if (featureFlags.isEmpty()) {
            return;
        }

        String generatedExporterClass = featureFlags.get().getClassInfo().toString() + "_GeneratedExporter";
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(generatedExporterClass)
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanProducer))
                .interfaces(FeatureFlagsExporter.class)
                .build()) {
            FeatureFlagsBytecodeGenerator.exportFeatureFlags(classCreator, featureFlags.get());
        }

        produceRoute(routeProducer, nonApplicationRootPath, config.exportPath);
    }

    private void produceRoute(BuildProducer<RouteBuildItem> routeProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPath, String route) {

        routeProducer.produce(nonApplicationRootPath.routeBuilder()
                .route(route)
                .routeConfigKey("quarkus.feature-flags.export-path")
                .handler(new FeatureFlagsHandler())
                .blockingRoute()
                .build());
    }
}
