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
import java.util.Set;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class KeptRoles extends DataSource {
    
    public KeptRoles()
    {
        this.filename = "discordbot.keptroles";
        this.size = 4;
        this.generateKey = item -> item[USERID]+"|"+item[SERVERID];
    }
    
    public String[] getRoleIds(String userid, String guildid)
    {
        if(get(userid+"|"+guildid)==null)
            return null;
        String str = get(userid+"|"+guildid)[ROLEIDS];
        if(str.equals(""))
            return new String[0];
        return str.split("\\s+");
    }
    
    public void userLeave(String userid, String guildid, String roleids)
    {
        set(new String[]{userid,guildid,roleids,OffsetDateTime.now().toEpochSecond()+""});
    }
    
    public void clearOldRoles()
    {
        synchronized(data)
        {
            long timeBeforeNow = OffsetDateTime.now().minusDays(7).toEpochSecond();
            Set<String> keyset = data.keySet();
            for(String key : keyset)
            {
                long leaveTime = Long.parseLong(data.get(key)[LEAVETIME]);
                if(leaveTime < timeBeforeNow)
                    data.remove(key);
            }
        }
    }
    
    final public static int USERID   = 0;
    final public static int SERVERID = 1;
    final public static int ROLEIDS  = 2;
    final public static int LEAVETIME = 3;
}
