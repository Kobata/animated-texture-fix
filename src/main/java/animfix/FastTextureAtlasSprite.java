package animfix;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.ARBCopyImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.glu.GLU;

import java.util.Arrays;
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

                if(AnimfixModContainer.copyImageEnabled && textureId != -1) {
                    int destTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    checkGLError("updateAnimation | fastPath getPreviousTexture");

                    // Unbinding texture for safety, since copy image has an explicit destination.
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    checkGLError("updateAnimation | fastPath unbindTex");

                    for (int mip = 0; mip <= mipLevels; ++mip) {
                        ARBCopyImage
                                .glCopyImageSubData(textureId, GL11.GL_TEXTURE_2D, mip, (width * k) >> mip, 0, 0, destTex, GL11.GL_TEXTURE_2D, mip, originX >> mip,
                                                    originY >> mip,
                                                    0, width >> mip, height >> mip, 1);
                        checkGLError("updateAnimation | fastPath mip="+mip);
                    }

                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, destTex);
                    checkGLError("updateAnimation | fastPath rebindTex");
                } else {
                    int[][] frameData = (int[][])this.framesTextureData.get(k);
                    uploadTextureMaxMips(mipLevels, frameData, width, height, originX, originY, false, false, frameData.length > 1);
                    checkGLError("updateAnimation | slowPath");
                }
            }
        }
    }

    @Override
    public void setFramesTextureData(List p_110968_1_) {
        if(textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
            checkGLError("setFramesTextureData | deleteTexture");
        }

        super.setFramesTextureData(p_110968_1_);

        // No need for extra texture if there's only one frame.
        if(p_110968_1_.size() > 1 && AnimfixModContainer.copyImageEnabled) {
            textureId = GL11.glGenTextures();
            checkGLError("setFramesTextureData | createTexture");

            int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            checkGLError("setFramesTextureData | getPreviousTexture");

            // Set up holding texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            checkGLError("setFramesTextureData | bindTexture");

            if (mipLevels > 0) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipLevels);
                checkGLError("setFramesTextureData | setTextureProperties[Mip Levels]");
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                checkGLError("setFramesTextureData | setTextureProperties[LOD Min]");
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float) mipLevels);
                checkGLError("setFramesTextureData | setTextureProperties[LOD Max]");
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
                checkGLError("setFramesTextureData | setTextureProperties[LOD Bias]");
            }

            // Reserve memory for texture
            for (int i = 0; i <= mipLevels; ++i) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, (width * framesTextureData.size()) >> i, height >> i, 0, GL12.GL_BGRA,
                                  GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
                checkGLError("setFramesTextureData | createMip " + i);
            }

            // Copy animation frames to holding texture
            for (int i = 0; i < framesTextureData.size(); ++i) {
                uploadTextureMaxMips(mipLevels, (int[][])framesTextureData.get(i), width, height, width * i, 0, false, false, mipLevels > 0);
                checkGLError("setFramesTextureData | uploadFrame " + i);
            }

            // Restore old texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            checkGLError("setFramesTextureData | restoreTexture");
        }
    }

    private static void uploadTextureMaxMips(int maxMips, int[][] data, int width, int height, int originX, int originY, boolean linearFiltering, boolean clamped, boolean mipFiltering) {
        int mips = maxMips >= 0 ? Math.min(maxMips, data.length-1) : data.length-1;
        for (int mip = 0; mip <= mips; ++mip) {
            TextureUtil.uploadTextureSub(mip, data[mip], width >> mip, height >> mip, originX >> mip, originY >> mip, linearFiltering,
                                         clamped, mipFiltering);
            checkGLError("uploadTextureMaxMips mip="+mip);
        }
    }

    private static void checkGLError(String desc) {
        int error = GL11.glGetError();

        while(error != GL11.GL_NO_ERROR) {
            String errorString = GLU.gluErrorString(error);

            AnimfixModContainer.log.error("GL Error: " + errorString + "(" + error + ") @ " + desc);

            error = GL11.glGetError();
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
        mipLevels = AnimfixModContainer.maxUpdateMip >= 0 ? Math.min(AnimfixModContainer.maxUpdateMip, p_147963_1_) : p_147963_1_;

        super.generateMipmaps(p_147963_1_);
    }
}
