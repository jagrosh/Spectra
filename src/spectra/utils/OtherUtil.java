/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class OtherUtil {
    
    public static ArrayList<String> readFile(String filename)
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
}
