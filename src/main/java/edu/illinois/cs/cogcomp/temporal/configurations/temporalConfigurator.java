package edu.illinois.cs.cogcomp.temporal.configurations;

import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Property;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class temporalConfigurator extends Configurator {
    public static Property EVENT_DETECTOR_WINDOW = new Property("EVENT_DETECTOR_WINDOW", "2");
    public static Property EVENT_TEMPREL_WINDOW = new Property("EVENT_TEMPREL_WINDOW", "3");
    public static Property EVENT_TIMEX_TEMPREL_WINDOW = new Property("EVENT_TIMEX_TEMPREL_WINDOW", "3");

    public static Property useAxisRules = new Property("useAxisRules", "false");
    public static Property useLongDist = new Property("useLongDist", "false");
    public static Property useSoftGroup = new Property("useSoftGroup", "false");
    public static Property useTemProb = new Property("useTemProb", "false");
    public static Property performET = new Property("usePerformET", "false");
    public static Property useILP = new Property("useILP", "false");
    public static Property useGoldTimex = new Property("useGoldTimex", "false");
    public static Property useGoldEvent = new Property("useGoldEvent", "false");
    public static Property respectExistingTempRels = new Property("respectExistingTempRels", "false");
    public static Property useHardConstraint = new Property("useHardConstraint", "false");
    @Override
    public ResourceManager getDefaultConfig() {
        Property[] props = {EVENT_DETECTOR_WINDOW,EVENT_TEMPREL_WINDOW,EVENT_TIMEX_TEMPREL_WINDOW,useAxisRules,useLongDist,useSoftGroup,performET,useTemProb,useILP,useGoldTimex,useGoldEvent,respectExistingTempRels,useHardConstraint};
        return new ResourceManager(generateProperties(props));
    }

    public ResourceManager getConfig(String... config_fname){
        try{
            List<ResourceManager> rms = new ArrayList<>();
            for(String config:config_fname)
                rms.add(new ResourceManager(config));
            ResourceManager rm = Configurator.mergeProperties(rms);
            return super.getConfig(rm);
        }
        catch (Exception e){
            e.printStackTrace();
            return getDefaultConfig();
        }
    }
}
