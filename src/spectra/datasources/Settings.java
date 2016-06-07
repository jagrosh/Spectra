/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.datasources;

import java.util.Arrays;
import spectra.DataSource;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Settings extends DataSource {
    private static final Settings settings = new Settings();
    
    private Settings()
    {
        filename = "discordbot.serversettings";
        size = 11;
    }
    
    public static Settings getInstance()
    {
        return settings;
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
            id,"","0","speakerphone room","","false","",SpConst.ALTPREFIX,"",
            "hug hi5 pat punch btth 8ball choose meme","global"
        };
        synchronized(data)
        {
            data.add(newSettings);
            setToWrite();
            return Arrays.copyOf(newSettings, size);
        }
    }
    
    public static String[] restrCmdsFromList(String restrictedCommands)
    {
        if(restrictedCommands==null)
            return new String[0];
        restrictedCommands = restrictedCommands.trim();
        if(restrictedCommands.equals(""))
            return new String[0];
        return restrictedCommands.split("\\s+");
    }
    
    public static String[] prefixesFromList(String prefixList)
    {
        String[] list = (prefixList+(char)29+SpConst.PREFIX).split((char)29+"+");
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
