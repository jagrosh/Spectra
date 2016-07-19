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
package spectra.tempdata;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import spectra.entities.Tuple;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Statistics {
    private long messagesReceived;
    private long commandsRun;
    private long commandSuccess;
    private long feedsSent;
    private final HashMap<String,Long> commandUsage;
    private final HashMap<String,Long> guildMessages;
    private final HashMap<String,Long> guildCommands;
    private final HashMap<String,Long> guildFeeds;
    private final OffsetDateTime start = OffsetDateTime.now();
    
    public Statistics()
    {
        messagesReceived = 0;
        commandsRun = 0;
        commandSuccess = 0;
        feedsSent = 0;
        commandUsage = new HashMap<>();
        guildMessages = new HashMap<>();
        guildCommands = new HashMap<>();
        guildFeeds = new HashMap<>();
    }
    
    public synchronized void ranCommand(String guildId, String command, boolean success)
    {
        commandsRun++;
        if(success)
        {
            commandUsage.put(command, commandUsage.getOrDefault(command, 0L)+1);
            commandSuccess++;
        }
        guildCommands.put(guildId, guildCommands.getOrDefault(guildId, 0L)+1);
    }
    
    public synchronized void sentMessage(String guildId)
    {
        messagesReceived++;
        guildMessages.put(guildId, guildMessages.getOrDefault(guildId, 0L)+1);
    }
    
    public synchronized void sentFeed(String guildId)
    {
        feedsSent++;
        guildFeeds.put(guildId, guildFeeds.getOrDefault(guildId, 0L)+1);
    }
    
    public synchronized int messagesPerHour()
    {
        return (int)(messagesReceived / (getUptime()/3600.0));
    }
    
    public synchronized int commandsPerHour()
    {
        return (int)(commandsRun / (getUptime()/3600.0));
    }
    
    public synchronized int feedsPerHour()
    {
        return (int)(feedsSent / (getUptime()/3600.0));
    }
    
    public synchronized List<Tuple<String,Integer>> mostUsedCommands(int num)
    {
        List<String> keys = new ArrayList<>(commandUsage.keySet());
        Collections.sort(keys, (String a, String b) -> commandUsage.get(b).compareTo(commandUsage.get(a)));
        List<Tuple<String,Integer>> list = new ArrayList<>();
        for(int i=0; i<num && i<keys.size(); i++)
            list.add(new Tuple<>(keys.get(i),(int)((double)commandUsage.get(keys.get(i))*100/commandSuccess)));
        return list;
    }
    
    public synchronized Tuple<String,Integer> mostMessagesGuild()
    {
        long max = 0;
        String maxId=null;
        for(String id : guildMessages.keySet())
            if(guildMessages.get(id) > max)
            {
                maxId = id;
                max = guildMessages.get(id);
            }
        return new Tuple<>(maxId,(int)((double)max*100/messagesReceived));
    }
    
    public synchronized Tuple<String,Integer> mostCommandsGuild()
    {
        long max = 0;
        String maxId="0";
        for(String id : guildCommands.keySet())
            if(guildCommands.get(id) > max)
            {
                maxId = id;
                max = guildCommands.get(id);
            }
        return new Tuple<>(maxId,(int)((double)max*100/commandsRun));
    }
    
    public synchronized Tuple<String,Integer> mostFeedsGuild()
    {
        long max = 0;
        String maxId="0";
        for(String id : guildFeeds.keySet())
            if(guildFeeds.get(id) > max)
            {
                maxId = id;
                max = guildFeeds.get(id);
            }
        return new Tuple<>(maxId,(int)((double)max*100/feedsSent));
    }
    
    public long getUptime()
    {
        return start.until(OffsetDateTime.now(), ChronoUnit.SECONDS);
    }
}
