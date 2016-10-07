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

import net.dv8tion.jda.entities.Guild;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutomodSettings extends DataSource {
    
    public AutomodSettings()
    {
        this.filename = "discordbot.amsettings";
        this.size = 8;
        this.generateKey = item -> item[GUILDID];
    }
    
    public void setSetting(Guild guild, int position, String value)
    {
        String[] item = get(guild.getId());
        if(item==null)
            item = getDefaults(guild.getId());
        item[position] = value;
        set(item);
    }
    
    public int getSetting(Guild guild, int position)
    {
        String[] item = get(guild.getId());
        if(item==null)
            item = getDefaults(guild.getId());
        return Integer.parseInt(item[position]);
    }
    
    private String[] getDefaults(String guildid)
    {
        return new String[]{
            guildid,
            "50",// starting strikes
            "30",// strike dropoff
            "60",// warn at
            "80",// mute at
            "30",// mute minutes
            "100",// kickat
            "120"// banat
        };
    }
    
    final public static int GUILDID   = 0;
    final public static int STARTSTRIKES = 1;
    final public static int STRIKEDROPOFF = 2;
    final public static int WARNAT = 3;
    final public static int MUTEAT = 4;
    final public static int MUTEMINUTES = 5;
    final public static int KICKAT = 6;
    final public static int BANAT = 7;
}
