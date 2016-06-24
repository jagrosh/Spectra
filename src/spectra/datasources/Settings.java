/*
 * Copyright 2016 jagrosh.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        generateKey = (item) -> {return item[SERVERID];};
    }
    
    public static Settings getInstance()
    {
        return settings;
    }

    public String[] getSettingsForGuild(String id)
    {
        synchronized(data)
        {
            return data.get(id).clone();
        }
    }
    
    public String[] makeNewSettingsForGuild(String id)
    {
        String[] newSettings = new String[]{
            id, //serverid
            "", //welcome message
            "0", //talk level
            "speakerphone room", //restricted commands
            "", // mod ids
            "false", //mhroles
            "", // leave message
            SpConst.ALTPREFIX, //prefixes
            "", // ignore list
            "hug hi5 pat punch btth 8ball choose meme rate ship", // tag commands
            "global" // tag mode
        };
        synchronized(data)
        {
            data.put(id,newSettings);
            setToWrite();
            return Arrays.copyOf(newSettings, size);
        }
    }
    
    public void setSetting(String guildid, int settingNum, String value)
    {
        synchronized(data)
        {
            data.get(guildid)[settingNum] = value;
            setToWrite();
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
    
    public static String[] tagCommandsFromList(String tagCommandList)
    {
        if(tagCommandList==null || tagCommandList.trim().equals(""))
            return new String[0];
        return tagCommandList.trim().split("\\s+");
    }
    
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
