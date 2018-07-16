package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TempRelAnnotator;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@RestController
public class ResourceController{
    @CrossOrigin
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String renderidx(){
        String str = "";
        try{
        File file = new File("index.html");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        str = new String(data, "UTF-8");
        }
        catch(Exception e){
        }
        return str;
    }

    @CrossOrigin
    @RequestMapping(value = "/api", method = RequestMethod.GET)
    public String annotate(@RequestParam(value = "text", defaultValue = "") String text, 
                           @RequestParam(value = "dct", defaultValue = "") String dct){
        String ret = "ERROR";
        try{
            System.out.println("Got DCT:" + dct);
            ret = TempRelAnnotator.processRawText(text, dct);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

}
