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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.utils.MiscUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import net.dv8tion.jda.utils.SimpleLog;
import spectra.DataSource;
import spectra.FeedHandler;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Rooms extends DataSource{
    private final HashMap<String,OffsetDateTime> lastActivity;
    private final Set<String> warnings;
    public Rooms()
    {
        this.filename = "discordbot.rooms";
        this.size = 4;
        this.generateKey = (item) -> {return item[CHANNELID];};
        lastActivity = new HashMap<>();
        warnings = new HashSet<>();
    }
    
    public void setLastActivity(String channelid, OffsetDateTime last)
    {
        if(get(channelid)!=null)
        {
            synchronized(lastActivity)
            {
                lastActivity.put(channelid, last);
            }
            synchronized(warnings)
            {
                warnings.remove(channelid);
            }
        }
    }
    
    public OffsetDateTime getLastActivity(String channelid)
    {
        synchronized(lastActivity)
        {
            return lastActivity.get(channelid);
        }
    }
    
    public void setWarned(String channelid)
    {
        synchronized(warnings)
        {
            if(!warnings.contains(channelid))
                warnings.add(channelid);
        }
    }
    
    public boolean isWarned(String channelid)
    {
        synchronized(warnings)
        {
            return warnings.contains(channelid);
        }
    }
    
    public List<String> getAllTextRoomIds()
    {
        ArrayList<String> ids = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((item) -> (!item[LOCKED].equalsIgnoreCase("voice"))).forEach((item) -> {
                ids.add(item[CHANNELID]);
            });
        }
        return ids;
    }
    
    public List<String[]> getTextRoomsOnGuild(Guild guild)
    {
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.keySet().stream().filter((key) -> (guild.getJDA().getTextChannelById(key)!=null 
                    && guild.getJDA().getTextChannelById(key).getGuild().equals(guild))).forEach((key) -> {
                list.add(data.get(key));
            });
        }
        return list;
    }
    
    public List<String[]> getVoiceRoomsOnGuild(Guild guild)
    {
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.keySet().stream().filter((key) -> (guild.getJDA().getVoiceChannelById(key)!=null 
                    && guild.getJDA().getVoiceChannelById(key).getGuild().equals(guild))).forEach((key) -> {
                list.add(data.get(key));
            });
        }
        return list;
    }
    
    public void checkExpires(JDA jda, FeedHandler handler)
    {
        if(jda.getStatus()!=JDA.Status.CONNECTED)
            return;
        int warn = 36;
        int delete = 12;
        List<String> allIds = getAllTextRoomIds();
        for(String id : allIds)
        {
            try{
                TextChannel tc = jda.getTextChannelById(id);
                if(tc==null || !PermissionUtil.checkPermission(tc, jda.getSelfInfo(), Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
                {
                    //Guild guild = jda.getGuildById(get(id)[Rooms.SERVERID]);
                    //if(guild==null)
                    //    remove(id); I could do this safely if Discord had 100% consistency
                    continue;
                }
                if(get(id)[OWNERID].equals(jda.getSelfInfo().getId()))
                    continue;
                boolean checked = false;
                if(getLastActivity(id)==null)
                {
                    MessageHistory mh = new MessageHistory(tc);
                    List<Message> messages = mh.retrieve(1);
                    checked = true;
                    if(messages==null || messages.isEmpty())
                        setLastActivity(id, MiscUtil.getCreationTime(id));
                    else
                    {
                        setLastActivity(id, messages.get(0).getTime());
                        if(messages.get(0).getAuthor().equals(jda.getSelfInfo()) && messages.get(0).getRawContent().startsWith("[<@"))
                            setWarned(id);
                    }
                }
                if(getLastActivity(id).isBefore(OffsetDateTime.now().minus(delete, ChronoUnit.HOURS)) && isWarned(id))
                {
                    if(!checked)
                    {
                        MessageHistory mh = new MessageHistory(tc);
                        List<Message> messages = mh.retrieve(1);
                        checked = true;
                        if(messages==null || messages.isEmpty())
                            setLastActivity(id, MiscUtil.getCreationTime(id));
                        else
                        {
                            setLastActivity(id, messages.get(0).getTime());
                            if(messages.get(0).getAuthor().equals(jda.getSelfInfo()) && messages.get(0).getRawContent().startsWith("[<@"))
                                setWarned(id);
                        }
                        if(getLastActivity(id).isBefore(OffsetDateTime.now().minus(delete, ChronoUnit.HOURS)) && isWarned(id))
                        {
                            remove(id);
                            handler.submitText(Feeds.Type.SERVERLOG, tc.getGuild(), "\uD83D\uDCFA Text channel **"+tc.getName()+
                                "** (ID:"+tc.getId()+") has been removed due to inactivity.");
                            tc.getManager().delete();
                            continue;
                        }
                    }
                    else
                    {
                        remove(id);
                        handler.submitText(Feeds.Type.SERVERLOG, tc.getGuild(), "\uD83D\uDCFA Text channel **"+tc.getName()+
                            "** (ID:"+tc.getId()+") has been removed due to inactivity.");
                        tc.getManager().delete();
                        continue;
                    }
                }
                if(getLastActivity(id).isBefore(OffsetDateTime.now().minus(warn, ChronoUnit.HOURS)))
                {
                    if(!checked)
                    {
                        MessageHistory mh = new MessageHistory(tc);
                        List<Message> messages = mh.retrieve(1);
                        //checked = true;
                        if(messages==null || messages.isEmpty())
                            setLastActivity(id, MiscUtil.getCreationTime(id));
                        else
                        {
                            setLastActivity(id, messages.get(0).getTime());
                            if(messages.get(0).getAuthor().equals(jda.getSelfInfo()) && messages.get(0).getRawContent().startsWith("[<@"))
                                setWarned(id);
                        }
                        if(getLastActivity(id).isBefore(OffsetDateTime.now().minus(warn, ChronoUnit.HOURS)))
                        {
                            //warn
                            Sender.sendMsg(String.format(SpConst.ROOM_WARNING, "<@"+get(id)[Rooms.OWNERID]+">"), tc);
                            setLastActivity(id, OffsetDateTime.now());
                            setWarned(id);
                            //continue;
                        }
                    }
                    else
                    {
                        //warn
                        Sender.sendMsg(String.format(SpConst.ROOM_WARNING, "<@"+get(id)[Rooms.OWNERID]+">"), tc);
                        setLastActivity(id, OffsetDateTime.now());
                        setWarned(id);
                        //continue;
                    }
                }
            }catch(Exception e){
                SimpleLog.getLog("Rooms").warn("Error checking room with id "+id+": "+e);
            }
        }
        
    }
    
    final public static int SERVERID   = 0;
    final public static int CHANNELID = 1;
    final public static int OWNERID  = 2;
    final public static int LOCKED = 3;
}
