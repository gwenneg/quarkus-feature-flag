package io.quarkiverse.featureflags.deployment;

import static io.quarkiverse.featureflags.deployment.FeatureFlagsProcessor.CONFIG_PROPERTY;
import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Unremovable;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class FeatureFlagsBytecodeGenerator {

    public static Consumer<MethodCreator> logAtStartup(FeatureFlagsBuildItem featureFlags) {
        return methodCreator -> {

            String className = featureFlags.getClassInfo().toString();

            ResultHandle featureFlagsBeanClass = methodCreator.loadClass(className);

            AssignableResultHandle featureFlagsBeanHandle = injectFeatureFlagsBean(methodCreator, featureFlagsBeanClass,
                    className);

            ResultHandle getLoggerHandle = methodCreator.invokeStaticMethod(
                    ofMethod(Logger.class, "getLogger", Logger.class, Class.class), featureFlagsBeanClass);
            AssignableResultHandle loggerHandle = methodCreator.createVariable(Logger.class);
            methodCreator.assign(loggerHandle, getLoggerHandle);

            ResultHandle titleHandle = methodCreator.load("=== Quarkus Feature Flags ===");
            methodCreator.invokeVirtualMethod(ofMethod(Logger.class, "info", void.class, Object.class), loggerHandle,
                    titleHandle);

            for (FieldInfo flag : featureFlags.getFieldInfos()) {
                ResultHandle formatHandle = methodCreator.load(getFlagMessageFormat(flag));
                ResultHandle fieldHandle = methodCreator.readInstanceField(FieldDescriptor.of(flag), featureFlagsBeanHandle);
                methodCreator.invokeVirtualMethod(
                        ofMethod(Logger.class, "infof", void.class, String.class, Object.class),
                        loggerHandle, formatHandle, fieldHandle);
            }

            methodCreator.returnValue(null);
        };
    }

    public static void exportFeatureFlags(ClassCreator classCreator, FeatureFlagsBuildItem featureFlags) {

        String className = featureFlags.getClassInfo().toString();

        classCreator.addAnnotation(ApplicationScoped.class);
        classCreator.addAnnotation(Unremovable.class);

        MethodCreator getFlagsHandle = classCreator
                .getMethodCreator(ofMethod(className, "getFlags", Map.class));

        ResultHandle featureFlagsBeanClass = getFlagsHandle.loadClass(className);

        AssignableResultHandle featureFlagsBeanHandle = injectFeatureFlagsBean(getFlagsHandle, featureFlagsBeanClass,
                className);

        ResultHandle hashMapHandle = getFlagsHandle.newInstance(ofConstructor(HashMap.class));
        AssignableResultHandle mapHandle = getFlagsHandle.createVariable(Map.class);
        getFlagsHandle.assign(mapHandle, hashMapHandle);

        for (FieldInfo flag : featureFlags.getFieldInfos()) {
            String configKey = getConfigKey(flag);
            ResultHandle keyHandle = getFlagsHandle.load(configKey);
            ResultHandle valueHandle = getFlagsHandle.readInstanceField(of(flag), featureFlagsBeanHandle);

            getFlagsHandle.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class), mapHandle, keyHandle,
                    valueHandle);
        }

        getFlagsHandle.returnValue(mapHandle);
    }

    private static AssignableResultHandle injectFeatureFlagsBean(MethodCreator methodCreator, ResultHandle beanClassHandle,
            String className) {

        ResultHandle containerHandle = methodCreator
                .invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));

        ResultHandle instanceHandle = methodCreator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                containerHandle, beanClassHandle, methodCreator.loadNull());

        ResultHandle getHandle = methodCreator.invokeInterfaceMethod(ofMethod(InstanceHandle.class, "get", Object.class),
                instanceHandle);
        AssignableResultHandle beanHandle = methodCreator.createVariable("L" + className.replace('.', '/') + ";");
        methodCreator.assign(beanHandle, getHandle);

        return beanHandle;
    }

    private static String getFlagMessageFormat(FieldInfo flag) {
        return flag.name() + " | " + getConfigKey(flag) + " | %s";
    }

    private static String getConfigKey(FieldInfo flag) {
        return flag.annotation(CONFIG_PROPERTY).value("name").asString();
    }
}
