package animfix;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.util.Arrays;

public class AnimfixModContainer extends DummyModContainer {
    public static boolean copyImageSupported = false;

    public AnimfixModContainer()
    {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId="animfix";
        meta.name="Animated Texture Fix";
        meta.version="0.1";
        meta.authorList= Arrays.asList("Kobata");
        meta.description="Makes animated textures faster.";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent evt) {
        ContextCapabilities caps = GLContext.getCapabilities();
        copyImageSupported = caps.OpenGL43 || caps.GL_ARB_copy_image;
        if(!copyImageSupported) {
            evt.getModLog().warn("Fast animated textures require OpenGL 4.3 or ARB_copy_image extension, which were not detected. Using original slow path.");
        } else {
            evt.getModLog().info("Using fast animated textures.");
        }
    }
}
