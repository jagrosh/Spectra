/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class OtherUtil {
    
    public static ArrayList<String> readFileLines(String filename)
    {
        BufferedReader reader;
        try{
            reader = new BufferedReader(new FileReader(filename));
        }catch(FileNotFoundException e){return null;}
        ArrayList<String> items = new ArrayList<>();
        try{
            while(true)
            {
                String next = reader.readLine();
                if(next==null)
                    break;
                items.add(next.trim());
            }
            reader.close();
            return items;
        }catch(IOException e){
            return null;
        }
    }
    
    public static File writeArchive(String text, String txtname)
    {
        File f = new File("WrittenFiles"+File.separatorChar+txtname+".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) 
        {
                String lines[] = text.split("\n");
                for(String line: lines)
                {
                    writer.write(line+"\r\n");
                }
                writer.flush();
        }catch(IOException e){System.err.println("ERROR saving file: "+e);}
        return f;
    }
}
