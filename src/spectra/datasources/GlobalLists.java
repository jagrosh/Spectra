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
public class GlobalLists extends DataSource {
    
    public GlobalLists()
    {
        this.filename = "discordbot.lists";
        this.size = 4;
        this.generateKey = item -> item[ID];
    }
    
    public boolean isUserBlacklisted(String id)
    {
        String[] entry = get(id);
        if(entry==null)
            return false;
        return entry[LISTTYPE].equalsIgnoreCase("blacklist");
    }
    
    /*public boolean isWhitelisted(String id)
    {
        String[] entry = get(id);
        if(entry==null)
            return false;
        return entry[LISTTYPE].equalsIgnoreCase("whitelist") || entry[LISTTYPE].equalsIgnoreCase("goldlist");
    }
    
    public boolean isGoldlisted(String id)
    {
        String[] entry = get(id);
        if(entry==null)
            return false;
        return entry[LISTTYPE].equalsIgnoreCase("goldlist");
    }*/
    
    /*public boolean isAuthorized(String id)
    {
        String[] entry = get(id);
        if(entry==null)
            return false;
        return entry[LISTTYPE].equalsIgnoreCase("authorized") || entry[LISTTYPE].equalsIgnoreCase("whitelist") || entry[LISTTYPE].equalsIgnoreCase("goldlist");
    }*/
    
    public ListState getState(String guildid)
    {
        String[] entry = get(guildid);
        if(entry==null)
            return ListState.NONE;
        for(ListState state: ListState.values())
            if(entry[LISTTYPE].equalsIgnoreCase(state.name()))
                return state;
        return ListState.NONE;
    }
    
    public String getBlacklistReason(String id)
    {
        String[] entry = get(id);
        if(entry==null)
            return "No entities with ID `"+id+"` are blacklisted";
        if(!entry[LISTTYPE].equalsIgnoreCase("blacklist"))
            return "That ID is listed under `"+entry[LISTTYPE]+"`";
        return entry[REASON];
    }
    
    public List<String> getList(ListState state)
    {
        ArrayList<String> list = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((entry) -> (entry[LISTTYPE].equalsIgnoreCase(state.name()))).forEach((entry) -> {
                list.add(entry[IDTYPE]+" `"+entry[ID]+"` "+entry[REASON]);
            });
        }
        return list;
    }
    
    /*public void authorize(String id, String details)
    {
        set(new String[]{id, "guild", "authorized", details});
    }*/
    
    final public static int ID   = 0;
    final public static int IDTYPE = 1;
    final public static int LISTTYPE  = 2;
    final public static int REASON = 3;
    
    
    //server blacklist - no commands by the server
    //user blacklist - no commands by the user
    //server whitelist - no pruning
    
    public enum ListState {
        NONE, BLACKLIST, WHITELIST, GOLDLIST
    }
}
