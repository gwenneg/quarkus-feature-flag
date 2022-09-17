package io.quarkiverse.featureflags.deployment;

import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.MethodVisitor;

public class FeatureFlagsConstructorVisitor extends MethodVisitor {

    private final String className;
    private final String generatedMethodName;

    public FeatureFlagsConstructorVisitor(MethodVisitor methodVisitor, String className, String generatedMethodName) {
        super(ASM_API_VERSION, methodVisitor);
        this.className = className;
        this.generatedMethodName = generatedMethodName;
    }

    @Override
    public void visitInsn(int opcode) {
        // When the end of the constructor is reached...
        if (opcode == RETURN) {
            /*
             * this.{generatedMethodName}();
             */
            super.visitVarInsn(ALOAD, 0);
            super.visitMethodInsn(INVOKEVIRTUAL, className, generatedMethodName, "()V", false);
        }
        super.visitInsn(opcode);
    }
}
