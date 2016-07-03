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

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.entities.Guild;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Rooms extends DataSource{
    public Rooms()
    {
        this.filename = "discordbot.rooms";
        this.size = 4;
        this.generateKey = (item) -> {return item[CHANNELID];};
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
