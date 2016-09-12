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
    
    public Settings()
    {
        filename = "discordbot.serversettings";
        size = 13;
        generateKey = (item) -> {return item[SERVERID];};
    }

    public String[] getSettingsForGuild(String id)
    {
        synchronized(data)
        {
            return data.get(id)==null ? null : data.get(id).clone();
        }
    }
    
    public String[] makeNewSettingsForGuild(String id)
    {
        String[] newSettings = new String[]{
            id, //serverid
            "", //welcome message
            "NORMAL", //room settings
            "speakerphone room", //restricted commands
            "", // mod ids
            "false", //keeproles
            "", // leave message
            SpConst.ALTPREFIX, //prefixes
            "", // ignore list
            "hug hi5 pat punch btth 8ball choose meme rate ship", // tag commands
            "global", // tag mode
            "", //autorole
            "" //tag mirrors
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
        if(prefixList==null || prefixList.equals(""))
            return new String[]{SpConst.PREFIX};
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
    
    public static String[] tagMirrorsFromList(String tagMirrorList)
    {
        if(tagMirrorList==null || tagMirrorList.trim().equals(""))
            return new String[0];
        return tagMirrorList.trim().split("\\s+");
    }
    
    //returns an id or null in the first index, and the message in the second
    public static String[] parseWelcomeMessage(String message)
    {
        String[] parts = message.split(":",2);
        if(parts.length>1)
        {
            parts[0] = parts[0].trim();
            if(parts[0].startsWith("<#") && parts[0].endsWith(">"))
                parts[0]= parts[0].substring(2,parts[0].length()-1);
            if(parts[0].matches("\\d+"))
                return new String[]{parts[0],parts[1].trim()};
        }
        return new String[]{null,message};
    }
    
    final public static int SERVERID   = 0;
    final public static int WELCOMEMSG = 1;
    final public static int ROOMSETTING= 2;
    final public static int BANNEDCMDS = 3;
    final public static int MODIDS     = 4;
    final public static int KEEPROLES  = 5;
    final public static int LEAVEMSG   = 6;
    final public static int PREFIXES   = 7;
    final public static int IGNORELIST = 8;
    final public static int TAGIMPORTS = 9;
    final public static int TAGMODE    = 10;
    final public static int AUTOROLE   = 11;
    final public static int TAGMIRRORS = 12;
}
