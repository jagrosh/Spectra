/*
 * Copyright 2016 John Grosh (jagrosh).
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

import net.dv8tion.jda.entities.TextChannel;
import spectra.DataSource;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutomodFilters extends DataSource {
    
    public AutomodFilters()
    {
        this.filename = "discordbot.amfilters";
        this.size = 6;
        this.generateKey = item -> item[GUILDID]+"|"+item[TYPE];
    }
    
    public boolean addChannelIgnore(TextChannel tc, FilterType type)
    {
        String[] filter = getFilter(tc.getGuild().getId(),type);
        if(filter==null)
            filter = getDefaultFilter(tc.getGuild().getId(),type);
        String ignores = filter[CHANNELIGNORES]==null ? "" : filter[CHANNELIGNORES];
        //if(ignores.contains("+c"))
        return false;
    }
    
    public String[] getFilter(String guildid, FilterType type)
    {
        return get(guildid+"|"+type.toString());
    }
    
    private String[] getDefaultFilter(String guildid, FilterType type)
    {
        String[] newfilter = new String[size];
        newfilter[GUILDID] = guildid;
        newfilter[TYPE] = type.toString();
        newfilter[CHANNELIGNORES] = "";
        newfilter[EXEMPTROLES] = "";
        String strikecost = "";
        String options = "";
        switch(type){
            case INVITE:
                strikecost = "75";
                options = guildid+" "+SpConst.JAGZONE_ID;
                break;
            case REGEX:
                strikecost = "30";
                break;
            case MENTION:
                strikecost = "75";
                options = "8";
                break;
            case WORD:
                strikecost = "40";
                break;
        }
        newfilter[STRIKECOST] = strikecost;
        newfilter[OPTIONS] = options;
        return newfilter;
    }
    
    final public static int GUILDID   = 0;
    final public static int TYPE = 1;
    final public static int STRIKECOST = 2;
    final public static int CHANNELIGNORES = 3;
    final public static int EXEMPTROLES = 4;
    final public static int OPTIONS = 5;
    
    public enum FilterType {
        REGEX, INVITE, MENTION, WORD
    }
}
