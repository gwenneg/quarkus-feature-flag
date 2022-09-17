package io.quarkiverse.featureflags.deployment;

import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class FeatureFlagsClassVisitor implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final String GENERATED_METHOD_NAME = "logFeaturesStatusAtStartup";
    private static final String LOGGER_NAME = Logger.class.getName().replace('.', '/');

    private final String className;
    private final List<FeatureFlag> featureFlagNames;

    public FeatureFlagsClassVisitor(DotName className, List<FeatureFlag> featureFlagNames) {
        this.className = className.toString().replace('.', '/');
        this.featureFlagNames = featureFlagNames;
    }

    @Override
    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
        return new ClassVisitor(ASM_API_VERSION, classVisitor) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("<init>")) {
                    // TODO Restrict to default constructor only.
                    // If the visited method is a constructor...
                    return new FeatureFlagsConstructorVisitor(methodVisitor, className, GENERATED_METHOD_NAME);
                } else {
                    return methodVisitor;
                }
            }

            @Override
            public void visitEnd() {
                /*
                 * private static final org.jboss.logging.Logger LOGGER;
                 */
                FieldVisitor fieldVisitor = super.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "LOGGER",
                        "L" + LOGGER_NAME + ";", null, null);
                fieldVisitor.visitEnd();

                /*
                 * static {
                 * LOGGER = Logger.getLogger((Class){className}.class);
                 * }
                 */
                MethodVisitor methodVisitor = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                methodVisitor.visitCode();
                methodVisitor.visitLdcInsn(Type.getType("L" + className + ";"));
                methodVisitor.visitMethodInsn(INVOKESTATIC, LOGGER_NAME, "getLogger",
                        "(Ljava/lang/Class;)L" + LOGGER_NAME + ";", false);
                methodVisitor.visitFieldInsn(PUTSTATIC, className, "LOGGER", "L" + LOGGER_NAME + ";");
                methodVisitor.visitInsn(RETURN);
                methodVisitor.visitMaxs(1, 0);
                methodVisitor.visitEnd();

                /*
                 * private void {GENERATED_METHOD_NAME}() {
                 * {className}.LOGGER.info((Object)"Feature flags startup status:");
                 * {className}.LOGGER.infof("The behavior groups unique name constraint is %s", (Object)(this.myFlagEnabled ?
                 * "enabled" : "disabled"));
                 * }
                 */
                methodVisitor = super.visitMethod(ACC_PRIVATE, GENERATED_METHOD_NAME, "()V", null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                methodVisitor.visitLabel(label0);
                methodVisitor.visitLineNumber(17, label0);
                methodVisitor.visitFieldInsn(GETSTATIC, className, "LOGGER", "L" + LOGGER_NAME + ";");
                methodVisitor.visitLdcInsn("Feature flags startup status:");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, LOGGER_NAME, "info", "(Ljava/lang/Object;)V", false);

                for (FeatureFlag featureFlag : featureFlagNames) {
                    methodVisitor.visitFieldInsn(GETSTATIC, className, "LOGGER", "L" + LOGGER_NAME + ";");
                    methodVisitor.visitLdcInsn(
                            "{ field: \"" + featureFlag.fieldName + "\", configKey: \"" + featureFlag.configKey
                                    + "\", value: \"%s\" }");
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    methodVisitor.visitFieldInsn(GETFIELD, className, featureFlag.fieldName, "Z");
                    methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
                            false);
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, LOGGER_NAME, "infof",
                            "(Ljava/lang/String;Ljava/lang/Object;)V", false);
                }

                methodVisitor.visitInsn(RETURN);
                Label label3 = new Label();
                methodVisitor.visitLabel(label3);
                methodVisitor.visitLocalVariable("this", "L" + className + ";", null, label0, label3, 0);
                methodVisitor.visitMaxs(3, 1);
                methodVisitor.visitEnd();

                super.visitEnd();
            }
        };
    }
}
