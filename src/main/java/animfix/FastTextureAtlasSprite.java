package animfix;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.ARBCopyImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL43;

import java.util.List;

public class FastTextureAtlasSprite extends TextureAtlasSprite {
    private int textureId = -1;
    private int mipLevels = 0;

    public FastTextureAtlasSprite(String p_i1282_1_) {
        super(p_i1282_1_);
    }

    @Override
    public void updateAnimation() {
        ++tickCounter;

        if (tickCounter >= animationMetadata.getFrameTimeSingle(frameCounter))
        {
            int i = animationMetadata.getFrameIndex(frameCounter);
            int j = animationMetadata.getFrameCount() == 0 ? framesTextureData.size() : animationMetadata.getFrameCount();
            frameCounter = (frameCounter + 1) % j;
            tickCounter = 0;
            int k = animationMetadata.getFrameIndex(frameCounter);

            if (i != k && k >= 0 && k < framesTextureData.size()) {
                int[][] frameData = (int[][])this.framesTextureData.get(k);

                if(AnimfixModContainer.copyImageEnabled && textureId != -1) {
                    int destTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

                    // Unbinding texture for safety, since copy image has an explicit destination.
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                    for (int mip = 0; mip < frameData.length; ++mip) {
                        ARBCopyImage
                                .glCopyImageSubData(textureId, GL11.GL_TEXTURE_2D, mip, (width * k) >> mip, 0, 0, destTex, GL11.GL_TEXTURE_2D, mip, originX >> mip,
                                                    originY >> mip,
                                                    0, width >> mip, height >> mip, 1);
                    }

                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, destTex);
                } else {
                    TextureUtil.uploadTextureMipmap(frameData, width, height, originX, originY, false, false);
                }
            }
        }
    }

    @Override
    public void setFramesTextureData(List p_110968_1_) {
        if(textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }

        super.setFramesTextureData(p_110968_1_);

        // No need for extra texture if there's only one frame.
        if(p_110968_1_.size() > 1 && AnimfixModContainer.copyImageEnabled) {
            textureId = GL11.glGenTextures();

            int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            // Set up holding texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            if (mipLevels > 0) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float) mipLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
            }

            // Reserve memory for texture
            for (int i = 0; i <= mipLevels; ++i) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, (width * framesTextureData.size()) >> i, height >> i, 0, GL12.GL_BGRA,
                                  GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            }

            // Copy animation frames to holding texture
            for (int i = 0; i < framesTextureData.size(); ++i) {
                TextureUtil.uploadTextureMipmap((int[][]) framesTextureData.get(i), width, height, width * i, 0, false, false);
            }

            // Restore old texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        }
    }

    @Override
    public void clearFramesTextureData() {
        super.clearFramesTextureData();

        if(textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
    }

    @Override
    public void generateMipmaps(int p_147963_1_) {
        mipLevels = p_147963_1_;

        super.generateMipmaps(p_147963_1_);
    }
}
