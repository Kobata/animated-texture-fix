package animfix.asm;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ListIterator;

public class TextureMapTransformer implements IClassTransformer {

    public TextureMapTransformer() {
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if(bytes == null || bytes.length == 0) {
            return bytes;
        }

        if("net.minecraft.client.renderer.texture.TextureMap".equals(transformedName)) {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode(Opcodes.ASM5);
            ClassWriter cw = new ClassWriter(0);


            cr.accept(cn, 0);

            for(MethodNode method : cn.methods) {
                if("func_94245_a".equals(method.name) || ("registerIcon".equals(method.name) && "(Ljava/lang/String;)Lnet/minecraft/util/IIcon;".equals(method.desc))) {
                    InsnList code = method.instructions;

                    for(ListIterator<AbstractInsnNode> iterator = code.iterator(); iterator.hasNext(); ) {
                        AbstractInsnNode insn = iterator.next();

                        if(insn.getOpcode() == Opcodes.NEW) {
                            TypeInsnNode typeNode = (TypeInsnNode)insn;
                            if("net/minecraft/client/renderer/texture/TextureAtlasSprite".equals(typeNode.desc)) {
                                typeNode.desc = "animfix/FastTextureAtlasSprite";
                            }
                        } else if(insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                            MethodInsnNode methodNode = (MethodInsnNode)insn;
                            if("net/minecraft/client/renderer/texture/TextureAtlasSprite".equals(methodNode.owner)) {
                                methodNode.owner = "animfix/FastTextureAtlasSprite";
                            }
                        }
                    }
                }
            }

            cn.accept(cw);
            return cw.toByteArray();
        }

        return bytes;
    }
}
