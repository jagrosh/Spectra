/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.datasources;

import java.util.Arrays;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Settings extends DataSource {
    
    public Settings()
    {
        filename = "discordbot.serversettings";
        size = 11;
        save = true;
    }
    
    public String[] getSettingsForGuild(String id)
    {
        synchronized(data)
        {
            for(String[] setting: data)
                if(setting[0].equals(id))
                    return setting.clone();
        }
        return null;
    }
    
    public String[] makeNewSettingsForGuild(String id)
    {
        String[] newSettings = new String[]{
            id,"","0","speakerphone room","","false","","/","",
            "hug hi5 pat punch btth 8ball choose meme","global"
        };
        synchronized(data)
        {
            data.add(newSettings);
            return Arrays.copyOf(newSettings, size);
        }
        
    }
    
    public static String[] prefixesFromList(String prefixList)
    {
        String[] list = (prefixList+(char)20+"%").split((char)29+"+");
        Arrays.parallelSort(list);
        return list;
    }
    
    //public final String[] PRIVATESETTINGS;
    
    final public static int SERVERID   = 0;
    final public static int WELCOMEMSG = 1;
    final public static int TALKLEVEL  = 2;
    final public static int BANNEDCMDS = 3;
    final public static int MODIDS     = 4;
    final public static int MHROLES    = 5;
    final public static int LEAVEMSG   = 6;
    final public static int PREFIXES   = 7;
    final public static int IGNORELIST = 8;
    final public static int TAGIMPORTS = 9;
    final public static int TAGMODE    = 10;
}
