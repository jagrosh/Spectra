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
public class Feeds extends DataSource {
    
    public Feeds()
    {
        filename = "discordbot.feeds";
        size = 4;
        generateKey = (item) -> {return item[GUILDID]+"|"+item[FEEDTYPE];};
    }
    
    public String[] feedForGuild(Guild guild, Type type)
    {
        synchronized(data)
        {
            return data.get(guild.getId()+"|"+type) == null ? null : data.get(guild.getId()+"|"+type).clone();
        }
    }
    
    public void removeFeed(String[] feed)
    {
        synchronized(data)
        {
            data.remove(generateKey.apply(feed));
            setToWrite();
        }
    }
    
    public List<String[]> findFeedsForGuild(Guild guild)
    {
        ArrayList<String[]> fds = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((feed) -> (feed[GUILDID].equals(guild.getId()))).forEach((feed) -> {
                fds.add(feed);
            });
        return fds;
        }
    }
    
    public List<String> findGuildsForFeedType(Type type)
    {
        ArrayList<String> ids = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((feed) -> (feed[FEEDTYPE].equals(type.toString()))).forEach((feed) -> {
                ids.add(feed[GUILDID]);
            });
        }
        return ids;
    }
    
    final public static int CHANNELID = 0;
    final public static int FEEDTYPE  = 1;
    final public static int GUILDID   = 2;
    final public static int DETAILS   = 3;
    
    public enum Type {
        MODLOG, SERVERLOG, TAGLOG, ANNOUNCEMENTS, BOTLOG, TWITCH
    }
}
