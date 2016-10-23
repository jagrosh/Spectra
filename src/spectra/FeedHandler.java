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
package spectra;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javafx.util.Pair;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.impl.JDAImpl;
import spectra.datasources.Feeds;
import spectra.datasources.GlobalLists;
import spectra.tempdata.Statistics;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FeedHandler {
    private final ScheduledExecutorService bufferTimers = Executors.newScheduledThreadPool(30);
    private final HashMap<String,Buffer> buffers = new HashMap<>();
    private final Feeds feeds;
    private final Statistics statistics;
    private final GlobalLists lists;
    
    private final HashMap<String,OffsetDateTime> limits = new HashMap<>();
    
    public FeedHandler(Feeds feeds, Statistics statistics, GlobalLists lists)
    {
        this.feeds = feeds;
        this.statistics = statistics;
        this.lists = lists;
    }
    
    public void submitText(Feeds.Type type, Guild guild, String text)
    {
        submitText(type,Collections.singletonList(guild),text);
    }
    
    public void submitFile(Feeds.Type type, Guild guild, Supplier<Pair<String,File>> message, String alternative)
    {
        submitFile(type, Collections.singletonList(guild), message, alternative);
    }
    
    public void submitText(Feeds.Type type, List<Guild> guilds, String text)
    {
        if(type==Feeds.Type.MODLOG || type== Feeds.Type.SERVERLOG || type==Feeds.Type.TAGLOG)
            text = logFormat(text);
        else if (type==Feeds.Type.BOTLOG)
            text = botlogFormat(text);
        for(Guild guild : guilds)
        {
            if(lists.getState(guild.getId())==GlobalLists.ListState.BLACKLIST)
                continue;
            String[] matching = feeds.feedForGuild(guild, type);
            if(matching==null)
                continue;
            TextChannel target = guild.getJDA().getTextChannelById(matching[Feeds.CHANNELID]);
            if(target==null)
            {
                if(guild.isAvailable())//channel was deleted
                {
                    feeds.removeFeed(matching);
                }
                continue;
            }
            if(!target.checkPermission(target.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
            {
                feeds.removeFeed(matching);
                Sender.sendPrivate(SpConst.WARNING+"Feed `"+matching[Feeds.FEEDTYPE]+"` has been removed from <#"
                        +target.getId()+"> because I cannot send messages there.", guild.getOwner().getPrivateChannel());
                continue;
            }
            synchronized(buffers){
                Buffer buffer = buffers.get(guild.getId());
                if(buffer==null)
                {
                    Long ratelimit = ((JDAImpl)guild.getJDA()).getMessageLimit(guild.getId());
                    if(ratelimit!=null)
                    {
                        buffer = new Buffer(text);
                        buffers.put(guild.getId(), buffer);
                        bufferTimers.schedule(() -> {
                            synchronized(buffers)
                            {
                                buffers.remove(guild.getId());
                                Buffer thisbuffer = buffers.get(guild.getId());
                                String txt = thisbuffer.getBuffer();
                                if(txt!=null)
                                    Sender.sendMsg(thisbuffer.getBuffer(), target);
                                if(thisbuffer.getFile()!=null)
                                {
                                    Sender.sendMsgFile(thisbuffer.getFileText(), thisbuffer.getFile(), thisbuffer.getFileAltText(), target);
                                }
                                statistics.sentFeed(guild.getId());
                            }
                        }, Math.min(ratelimit-System.currentTimeMillis()+10, 10000), TimeUnit.MILLISECONDS);
                    }
                    else
                    {
                        Sender.sendMsg(text, target);
                        statistics.sentFeed(guild.getId());
                    }
                }
                else
                {
                    buffer.append(text);
                }
            }
        }
    }
    
    public void submitFile(Feeds.Type type, List<Guild> guilds, Supplier<Pair<String,File>> message, String alternative)
    {
        if(type==Feeds.Type.MODLOG || type== Feeds.Type.SERVERLOG || type==Feeds.Type.TAGLOG)
            alternative = logFormat(alternative);
        else if (type==Feeds.Type.BOTLOG)
            alternative = botlogFormat(alternative);
        File file = null;
        String normal = null;
        for(Guild guild : guilds)
        {
            if(lists.getState(guild.getId())==GlobalLists.ListState.BLACKLIST)
                continue;
            String[] matching = feeds.feedForGuild(guild, type);
            if(matching==null)
                continue;
            TextChannel target = guild.getJDA().getTextChannelById(matching[Feeds.CHANNELID]);
            if(target==null)
            {
                if(guild.isAvailable())//channel was deleted
                {
                    feeds.removeFeed(matching);
                }
                continue;
            }
            if(limits.get(target.getId())!=null && limits.get(target.getId()).plusSeconds(3).isAfter(OffsetDateTime.now()))
                continue;
            if(!target.checkPermission(target.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
            {
                feeds.removeFeed(matching);
                Sender.sendPrivate(SpConst.WARNING+"Feed `"+matching[Feeds.FEEDTYPE]+"` has been removed from <#"
                        +target.getId()+"> because I cannot send messages there.", guild.getOwner().getPrivateChannel());
                continue;
            }
            if(file==null)
            {
                Pair<String,File> item = message.get();
                file = item.getValue();
                normal = item.getKey();
                if(type==Feeds.Type.MODLOG || type== Feeds.Type.SERVERLOG || type==Feeds.Type.TAGLOG)
                    normal = logFormat(normal);
                else if (type==Feeds.Type.BOTLOG)
                    normal = botlogFormat(normal);
            }
            synchronized(buffers)
            {
                Buffer buffer = buffers.get(guild.getId());
                if(buffer==null)
                {
                    Long ratelimit = ((JDAImpl)guild.getJDA()).getMessageLimit(guild.getId());
                    if(ratelimit!=null && ratelimit>0)
                    {
                        buffer = new Buffer(file,normal,alternative);
                        buffers.put(guild.getId(), buffer);
                        bufferTimers.schedule(() -> {
                            synchronized(buffers)
                            {
                                Buffer thisbuffer = buffers.get(guild.getId());
                                String txt = thisbuffer.getBuffer();
                                if(txt!=null)
                                    Sender.sendMsg(thisbuffer.getBuffer(), target);
                                if(thisbuffer.getFile()!=null)
                                {
                                    limits.put(target.getId(), OffsetDateTime.now());
                                    Sender.sendMsgFile(thisbuffer.getFileText(), thisbuffer.getFile(), thisbuffer.getFileAltText(), target);
                                }
                                statistics.sentFeed(guild.getId());
                                buffers.remove(guild.getId());
                            }
                        }, ratelimit+10, TimeUnit.MILLISECONDS);
                    }
                    else
                    {
                        limits.put(target.getId(), OffsetDateTime.now());
                        Sender.sendMsgFile(normal, file, alternative, target);
                        statistics.sentFeed(guild.getId());
                    }
                }
                else
                {
                    buffer.setFile(file, normal, alternative);
                }
            }
        }
    }
    
    public static String logFormat(String text)
    {
        return "`["+OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0,8)+"]` "+FormatUtil.demention(text);
    }
    
    public static String botlogFormat(String text)
    {
        return "`["+OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0,8)+"]` "+text;
    }
    
    public void shutdown()
    {
        bufferTimers.shutdown();
    }
    
    /*
        a ChannelFeed object 
    */
    private class Buffer {
        private StringBuilder buffer;
        private File file;
        private String fileText;
        private String fileAltText;
        
        public Buffer(String start)
        {
            this.buffer = new StringBuilder(start);
        }
        
        public Buffer(File file, String filetext, String alt)
        {
            this.file = file;
            this.fileText = filetext;
            this.fileAltText = alt;
        }
        
        private void append(String str)
        {
            if(buffer==null)
                buffer = new StringBuilder(str);
            else
                buffer.append("\n").append(str);
        }
        
        private void setFile(File file, String message, String alt)
        {
            this.file = file;
            this.fileText = message;
            this.fileAltText = alt;
        }
        
        public String getBuffer()
        {
            return buffer==null ? null : buffer.toString();
        }
        
        public File getFile()
        {
            return file;
        }
        
        public String getFileText()
        {
            return this.fileText;
        }
        
        public String getFileAltText()
        {
            return this.fileAltText;
        }
    }
}
