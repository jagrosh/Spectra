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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.entities.Guild;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Rooms extends DataSource{
    private final HashMap<String,OffsetDateTime> lastActivity;
    private final ArrayList<String> warnings;
    public Rooms()
    {
        this.filename = "discordbot.rooms";
        this.size = 4;
        this.generateKey = (item) -> {return item[CHANNELID];};
        lastActivity = new HashMap<>();
        warnings = new ArrayList<>();
    }
    
    public void setLastActivity(String channelid, OffsetDateTime last)
    {
        if(get(channelid)!=null)
        {
            synchronized(lastActivity)
            {
                lastActivity.put(channelid, last);
            }
            synchronized(warnings)
            {
                warnings.remove(channelid);
            }
        }
    }
    
    public OffsetDateTime getLastActivity(String channelid)
    {
        synchronized(lastActivity)
        {
            return lastActivity.get(channelid);
        }
    }
    
    public void setWarned(String channelid)
    {
        synchronized(warnings)
        {
            if(!warnings.contains(channelid))
                warnings.add(channelid);
        }
    }
    
    public boolean isWarned(String channelid)
    {
        synchronized(warnings)
        {
            return warnings.contains(channelid);
        }
    }
    
    public List<String> getAllRoomIds()
    {
        ArrayList<String> ids = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().forEach((room) -> {
                ids.add(room[CHANNELID]);
            });
        }
        return ids;
    }
    
    public List<String[]> getTextRoomsOnGuild(Guild guild)
    {
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.keySet().stream().filter((key) -> (guild.getJDA().getTextChannelById(key)!=null 
                    && guild.getJDA().getTextChannelById(key).getGuild().equals(guild))).forEach((key) -> {
                list.add(data.get(key));
            });
        }
        return list;
    }
    
    public List<String[]> getVoiceRoomsOnGuild(Guild guild)
    {
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.keySet().stream().filter((key) -> (guild.getJDA().getVoiceChannelById(key)!=null 
                    && guild.getJDA().getVoiceChannelById(key).getGuild().equals(guild))).forEach((key) -> {
                list.add(data.get(key));
            });
        }
        return list;
    }
    
    final public static int SERVERID   = 0;
    final public static int CHANNELID = 1;
    final public static int OWNERID  = 2;
    final public static int LOCKED = 3;
}
