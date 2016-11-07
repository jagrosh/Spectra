/*
 * Copyright 2016 jagrosh.
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
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import spectra.DataSource;
import spectra.utils.TagUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Tags extends DataSource{
    
    public Tags()
    {
        filename = "discordbot.tags";
        size = 3;
        generateKey = (item) -> {return item[TAGNAME].toLowerCase();};
    }
    
    public String[] findTag(String name, Guild guild, boolean local, boolean nsfw)
    {
        synchronized(data)
        {
            String[] tag = data.get(name.toLowerCase());
            if(tag!=null)
            {
                if(!nsfw && TagUtil.isNSFWTag(tag))
                    return new String[]{tag[OWNERID],tag[TAGNAME],"\uD83D\uDD1E This tag has been marked as **Not Safe For Work** and is not available in this channel."};
                if(local && guild!=null)
                {
                    User u = guild.getJDA().getUserById(tag[OWNERID]);
                    if(u!=null && guild.isMember(u))
                        return tag;
                    return null;
                    //return new String[]{tag[OWNERID],tag[TAGNAME],SpConst.WARNING+"This tag does not belong to a user on this server."};
                }
                return tag.clone();
            }
        }
        return null;
    }
    
    public ArrayList<String[]> findTags(String search, Guild guild, boolean local, boolean nsfw)
    {
        if(search==null)
            search="";
        ArrayList<String[]> results = new ArrayList<>();
        search = search.toLowerCase();
        synchronized(data)
        {
            for(String[] tag: data.values())
            {
                if(tag[TAGNAME].toLowerCase().contains(search) && (nsfw || !TagUtil.isNSFWTag(tag)))
                {
                    if(local && guild!=null)
                    {
                        User owner = guild.getJDA().getUserById(tag[OWNERID]);
                        if(owner!=null && guild.isMember(owner))
                            results.add(tag.clone());
                    }
                    else results.add(tag.clone());
                }
            }
        }
                    
        return results;
    }
    
    public ArrayList<String> findTagsByOwner(User owner, boolean nsfw)
    {
        ArrayList<String> results = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((tag) -> (tag[OWNERID].equals(owner.getId()) && (nsfw || !TagUtil.isNSFWTag(tag))))
                .forEach((tag) -> {
                results.add(tag[TAGNAME]);
            });
        }
        return results;
    }
    
    public void removeTag(String name)
    {
        remove(name.toLowerCase());
    }
    
    final public static int OWNERID   = 0;
    final public static int TAGNAME   = 1;
    final public static int CONTENTS  = 2;
}
