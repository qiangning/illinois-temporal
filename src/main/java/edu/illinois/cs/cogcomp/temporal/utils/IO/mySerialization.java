package edu.illinois.cs.cogcomp.temporal.utils.IO;

import java.io.*;

public class mySerialization {
    private boolean verbose;
    private PrintStream ps = System.out;

    public mySerialization() {
        verbose = true;
    }

    public mySerialization(boolean verbose) {
        this.verbose = verbose;
    }

    public mySerialization(PrintStream ps){
        verbose = true;
        this.ps = ps;
    }

    public void serialize(Object obj, String path) throws Exception{
        File serializedFile = new File(path);
        FileOutputStream fileOut = new FileOutputStream(serializedFile.getPath());
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(obj);
        out.close();
        fileOut.close();
        if(verbose)
            ps.println("Serialization of object has been saved to "+serializedFile.getPath());
    }
    public Object deserialize(String path) throws Exception{
        File serializedFile = new File(path);
        Object obj = null;
        if(serializedFile.exists()){
            if(verbose)
                ps.println("Serialization exists. Loading from "+serializedFile.getPath());
            FileInputStream fileIn = new FileInputStream(serializedFile.getPath());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            obj = in.readObject();
            in.close();
            fileIn.close();
        }
        else{
            if(verbose)
                ps.println("Serialization doesn't exist. Return null. ");
        }
        return obj;
    }
}
