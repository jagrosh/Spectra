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
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Entries extends DataSource {
    
    public Entries()
    {
        this.filename = "discordbot.entries";
        this.size = 4;
        this.generateKey = item -> item[SERVERID]+"|"+item[USERID]+"|"+item[CONTESTNAME].toLowerCase();
    }
    
    public List<String[]> getEntries(String guildid, String contestname)
    {
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((entry) -> (entry[SERVERID].equals(guildid) && entry[CONTESTNAME].equalsIgnoreCase(contestname))).forEach((entry) -> {
                list.add(entry);
            });
        }
        return list;
    }
    
    final public static int SERVERID    = 0;
    final public static int CONTESTNAME = 1;
    final public static int USERID   = 2;
    final public static int ENTRY   = 3;
}
