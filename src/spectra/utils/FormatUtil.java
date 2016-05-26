/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.utils;

import java.util.Arrays;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FormatUtil {
    
    //Cleanly splits on whitespace in 2
    public static String[] cleanSplit(String input){return cleanSplit(input,"\\s+",2);}
    
    //Cleanly splits on whitespace to specified size
    public static String[] cleanSplit(String input, int size){return cleanSplit(input,"\\s+",size);}
    
    //Cleanly splits on given to specified size, padding with null
    public static String[] cleanSplit(String input, String regex, int size)
    {
        return Arrays.copyOf(input.split(regex, size), size);
    }
}
