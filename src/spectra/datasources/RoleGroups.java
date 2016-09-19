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
public class RoleGroups extends DataSource {
    
    public RoleGroups()
    {
        this.filename = "discordbot.rolegroups";
        this.size = 4;
        this.generateKey = item -> item[SERVERID]+"|"+item[GROUPNAME].toLowerCase();
    }
    
    public String[] getIdsForGroup(String guildid, String groupname)
    {
        String[] item = get(guildid+"|"+groupname.toLowerCase());
        if(item==null || item[IDS]==null || item[IDS].equals(""))
            return new String[0];
        return item[IDS].split("\\s+");
    }
    
    public boolean addIdToGroup(String guildid, String groupname, String id)
    {
        String[] item = get(guildid+"|"+groupname.toLowerCase());
        if(item==null)
        {
            set(new String[]{guildid,groupname,id,""});
            return true;
        }
        String ids = item[IDS]==null ? "" : " "+item[IDS]+" ";
        if(ids.contains(" "+id+" "))
            return false;
        ids = (ids+id).trim();
        set(new String[]{item[SERVERID],item[GROUPNAME],ids,item[SETTINGS]});
        return true;
    }
    
    public boolean removeIdFromGroup(String guildid, String groupname, String id)
    {
        String[] item = get(guildid+"|"+groupname.toLowerCase());
        if(item==null)
            return false;
        String ids = item[IDS]==null ? "" : " "+item[IDS]+" ";
        if(!ids.contains(" "+id+" "))
        {
            return false;
        }
        ids = ids.replace(" "+id+" ", " ").trim();
        set(new String[]{item[SERVERID],item[GROUPNAME],ids,item[SETTINGS]});
        return true;
    }
    
    public void setSettings(String guildid, String groupname, String settings)
    {
        String[] item = get(guildid+"|"+groupname.toLowerCase());
        if(item==null)
            return;
        item[SETTINGS] = settings;
        set(item);
    }
    
    public String getSettings(String guildid, String groupname)
    {
        String[] item = get(guildid+"|"+groupname.toLowerCase());
        return item==null ? null : (item.length<SETTINGS+1 || item[SETTINGS]==null ? "" : item[SETTINGS]);
    }
    
    final public static int SERVERID   = 0;
    final public static int GROUPNAME = 1;
    final public static int IDS  = 2;
    final public static int SETTINGS = 3;
}
