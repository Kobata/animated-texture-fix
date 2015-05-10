package animfix;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.io.File;
import java.util.Arrays;

public class AnimfixModContainer extends DummyModContainer {
    public static boolean copyImageSupported = false;
    public static boolean copyImageEnabled = false;
    public static int maxUpdateMip = -1;
    public static Logger log = null;

    public AnimfixModContainer() {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId="animfix";
        meta.name="Animated Texture Fix";
        meta.version="0.3";
        meta.authorList= Arrays.asList("Kobata");
        meta.description="Makes animated textures faster.";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent evt) {
        log = evt.getModLog();

        ContextCapabilities caps = GLContext.getCapabilities();
        copyImageSupported = caps.OpenGL43 || caps.GL_ARB_copy_image;
        if(!copyImageSupported) {
            log.warn("Fast animated textures require OpenGL 4.3 or ARB_copy_image extension, which were not detected. Using original slow path.");
        } else {
            log.info("Using fast animated textures.");
        }

        File configFile = evt.getSuggestedConfigurationFile();
        Configuration config = new Configuration(configFile);

        boolean enableFastAnimation = config.getBoolean("enableFastAnimation", "animfix", true, "Enable the faster animation mode. Set to false only if true causes issues.");
        maxUpdateMip = config.getInt("maxUpdateMipLevel", "animfix", -1, -1, 4, "Set to a number to disable animation updates past that mip level. -1 means update all. Higher numbers update more levels.");

        if(config.hasChanged()) {
            config.save();
        }

        copyImageEnabled = copyImageSupported && enableFastAnimation;
    }
}
