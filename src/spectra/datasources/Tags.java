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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import spectra.DataSource;
import spectra.SpConst;
import spectra.utils.FinderUtil;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Tags extends DataSource{
    private static final Tags tags = new Tags();
    
    private Tags()
    {
        filename = "discordbot.tags";
        size = 3;
    }
    
    public static Tags getInstance()
    {
        return tags;
    }

    @Override
    protected String generateKey(String[] item) {
        return item[TAGNAME].toLowerCase();
    }
    
    public String[] findTag(String name)
    {
        return findTag(name,null,false,true);
    }
    
    public String[] findTag(String name, Guild guild, boolean local, boolean nsfw)
    {
        synchronized(data)
        {
            String[] tag = data.get(name.toLowerCase());
            if(tag!=null)
            {
                if(!nsfw && isNSFW(tag))
                    return new String[]{tag[OWNERID],tag[TAGNAME],SpConst.WARNING+"This tag has been marked as **Not Safe For Work** and is not available in this channel."};
                if(local && guild!=null)
                {
                    User u = guild.getJDA().getUserById(tag[OWNERID]);
                    if(u!=null && guild.isMember(u))
                        return tag;
                    return new String[]{tag[OWNERID],tag[TAGNAME],SpConst.WARNING+"This tag does not belong to a user on this server."};
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
                if(tag[TAGNAME].toLowerCase().contains(search) && (nsfw || !isNSFW(tag)))
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
            data.values().stream().filter((tag) -> (tag[OWNERID].equals(owner.getId()) && (nsfw || !isNSFW(tag))))
                .forEach((tag) -> {
                results.add(tag[TAGNAME]);
            });
        }
        return results;
    }
    
    public void setTag(String[] newTag)
    {
        synchronized(data)
        {
            data.put(generateKey(newTag), newTag);
        }
        setToWrite();
    }
    
    public void removeTag(String name)
    {
        synchronized(data)
        {
            data.remove(name.toLowerCase());
        }
        setToWrite();
    }
    
    final public static int OWNERID   = 0;
    final public static int TAGNAME   = 1;
    final public static int CONTENTS  = 2;
    
    public static boolean isNSFW(String[] tag)
    {
        return tag[CONTENTS].toLowerCase().contains("{nsfw}");
    }
    
}
