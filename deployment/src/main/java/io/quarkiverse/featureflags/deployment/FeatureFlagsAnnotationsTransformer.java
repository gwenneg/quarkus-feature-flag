package io.quarkiverse.featureflags.deployment;

import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class FeatureFlagsAnnotationsTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(Kind kind) {
        return kind == CLASS;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        ClassInfo classInfo = transformationContext.getTarget().asClass();
        if (classInfo.classAnnotation(FeatureFlagsProcessor.FEATURE_FLAGS_NAME) != null) {
            AnnotationInstance startupAnnotation = AnnotationInstance.create(FeatureFlagsProcessor.STARTUP_NAME, classInfo,
                    new AnnotationValue[0]);
            transformationContext.transform().add(startupAnnotation).done();
        }
    }
}
