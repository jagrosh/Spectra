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
import java.util.List;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Mutes extends DataSource {
    
    public Mutes()
    {
        this.filename = "discordbot.muted";
        this.size = 3;
        this.generateKey = (item)->{return item[USERID]+"|"+item[SERVERID];};
    }
    
    public String[] getMute(String userid, String guildid)
    {
        return get(userid+"|"+guildid);
    }
    
    public List<String[]> getMutesForGuild(String guildid)
    {
        List<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((mute) -> (mute[SERVERID].equals(guildid))).forEach((mute) -> {
                list.add(mute.clone());
            });
        }
        return list;
    }
    
    public List<String[]> getExpiredMutes()
    {
        OffsetDateTime now = OffsetDateTime.now();
        List<String[]> expired = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((mute) -> (now.isAfter(OffsetDateTime.parse(mute[UNMUTETIME])))).forEach((mute) -> {
                expired.add(mute);
            });
        }
        return expired;
    }
    
    public void removeAll(List<String[]> mutes)
    {
        mutes.stream().forEach((mute) -> {
            remove(generateKey.apply(mute));
        });
    }
    
    final public static int USERID   = 0;
    final public static int SERVERID = 1;
    final public static int UNMUTETIME  = 2;
}
