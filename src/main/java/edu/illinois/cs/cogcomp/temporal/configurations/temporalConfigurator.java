package edu.illinois.cs.cogcomp.temporal.configurations;

import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Property;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;

import java.io.IOException;
import java.util.Map;

public class temporalConfigurator extends Configurator {
    @Override
    public ResourceManager getDefaultConfig() {
        Property[] props = {};
        return new ResourceManager(generateProperties(props));
    }

    public ResourceManager getConfig(String config_fname)  throws IOException {
        return super.getConfig(new ResourceManager(config_fname));
    }
}
