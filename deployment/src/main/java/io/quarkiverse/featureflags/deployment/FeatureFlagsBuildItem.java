package io.quarkiverse.featureflags.deployment;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FeatureFlagsBuildItem extends SimpleBuildItem {

    private final ClassInfo classInfo;
    private final List<FieldInfo> fieldInfos;

    public FeatureFlagsBuildItem(ClassInfo classInfo, List<FieldInfo> fieldInfos) {
        this.classInfo = classInfo;
        this.fieldInfos = fieldInfos;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public List<FieldInfo> getFieldInfos() {
        return fieldInfos;
    }
}
