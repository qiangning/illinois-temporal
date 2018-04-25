package edu.illinois.cs.cogcomp.temporal.configurations;


import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SignalWordSet {
    public Set<String> temporalConnectiveSet = new HashSet<>(),modalVerbSet = new HashSet<>(),axisSignalWordSet = new HashSet<>(),reportingVerbSet = new HashSet<>();
    public static SignalWordSet instance;
    private static Set<String> parseRmProperty(ResourceManager rm, String propName){
        String tmp = rm.getString(propName);
        return new HashSet<String>(Arrays.asList(tmp.split(",")));
    }
    public static SignalWordSet getInstance(){
        if(instance!=null)
            return instance;
        try {
            return getInstance(new ResourceManager("config/SignalWordSet.properties"));
        }
        catch (Exception e){
            e.printStackTrace();
            return new SignalWordSet();
        }
    }
    public static SignalWordSet getInstance(ResourceManager rm){
        if(instance!=null)
            return instance;
        instance = new SignalWordSet();
        instance.temporalConnectiveSet = parseRmProperty(rm,"temporalConnectiveSet");
        instance.modalVerbSet = parseRmProperty(rm,"modalVerbSet");
        instance.axisSignalWordSet = parseRmProperty(rm,"axisSignalWordSet");
        instance.reportingVerbSet = parseRmProperty(rm,"reportingVerbSet");
        return instance;
    }
}
