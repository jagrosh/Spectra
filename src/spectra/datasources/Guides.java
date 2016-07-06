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

import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Guides extends DataSource {
    private final static int MAXPAGES = 5;
    public Guides()
    {
        this.filename = "discordbot.guides";
        this.size = MAXPAGES+1;
        this.generateKey = item -> item[SERVERID];
    }
    
    public void setPage(String guildid, int pagenum, String contents)
    {
        String[] guide = get(guildid);
        if(guide==null)
        {
            guide = new String[size];
            guide[SERVERID] = guildid;
        }
        guide[pagenum] = contents;
        set(guide);
    }
    
    final public static int SERVERID   = 0;
}
