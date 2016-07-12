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
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;
import spectra.DataSource;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Contests extends DataSource {
    
    public Contests()
    {
        this.filename = "discordbot.contests";
        this.size = 7;
        this.generateKey = item -> item[SERVERID]+"|"+item[CONTESTNAME].toLowerCase();
    }
    
    public String[] getContest(String guildid, String contestname)
    {
        return get(guildid+"|"+contestname.toLowerCase());
    }
    
    public List<String[]> getUpcomingContestsForGuild(String guildid)
    {
        ArrayList<String[]> list = new ArrayList<>();
        long now = OffsetDateTime.now().toInstant().toEpochMilli();
        synchronized(data)
        {
            for(String[] contest : data.values())
                if(contest[SERVERID].equals(guildid) && Long.parseLong(contest[STARTTIME])>now)
                    list.add(contest);
        }
        return list;
    }
    
    public List<String[]> getCurrentContestsForGuild(String guildid)
    {
        ArrayList<String[]> list = new ArrayList<>();
        long now = OffsetDateTime.now().toInstant().toEpochMilli();
        synchronized(data)
        {
            for(String[] contest : data.values())
                if(contest[SERVERID].equals(guildid) && Long.parseLong(contest[STARTTIME])<now && Long.parseLong(contest[ENDTIME])>now)
                    list.add(contest);
        }
        return list;
    }
    
    public List<String[]> getEndedContestsForGuild(String guildid)
    {
        ArrayList<String[]> list = new ArrayList<>();
        long now = OffsetDateTime.now().toInstant().toEpochMilli();
        synchronized(data)
        {
            for(String[] contest : data.values())
                if(contest[SERVERID].equals(guildid) && Long.parseLong(contest[ENDTIME])<now)
                    list.add(contest);
        }
        return list;
    }
    
    public void notifications(JDA jda)
    {
        Long now = OffsetDateTime.now().toInstant().toEpochMilli();
        boolean changed = false;
        synchronized(data)
        {
            for(String[] contest : data.values())
            {
                if(contest[STATUS].equals("4"))
                    continue;
                TextChannel tc = jda.getTextChannelById(contest[CHANNELID]);
                if(tc==null)
                    continue;
                Long start = Long.parseLong(contest[STARTTIME]);
                Long end = Long.parseLong(contest[ENDTIME]);
                if(contest[STATUS].equals("0") && start-now<((long)1000*60*60))
                {
                    Sender.sendMsg("\uD83C\uDFC6 **"+contest[CONTESTNAME]+"** will be starting in "+FormatUtil.secondsToTime((start-now)/1000), tc);
                    contest[STATUS]="1";
                    changed=true;
                }
                if(contest[STATUS].equals("1") && now>=start)
                {
                    Sender.sendMsg("\uD83C\uDFC6 **"+contest[CONTESTNAME]+"** has begun! Use `"+SpConst.PREFIX+"contest rules "+contest[CONTESTNAME]+"` to see details!", tc);
                    contest[STATUS]="2";
                    changed=true;
                }
                if(contest[STATUS].equals("2") && end-now<((long)1000*60*60))
                {
                    Sender.sendMsg("\uD83C\uDFC6 **"+contest[CONTESTNAME]+"** will be ending in "+FormatUtil.secondsToTime((end-now)/1000), tc);
                    contest[STATUS]="3";
                    changed=true;
                }
                if(contest[STATUS].equals("3") && now>=end)
                {
                    Sender.sendMsg("\uD83C\uDFC6 **"+contest[CONTESTNAME]+"** has ended! Keep an eye out for a results announcement!", tc);
                    contest[STATUS]="4";
                    changed=true;
                }
            }
            if(changed)
                setToWrite();
        }
    }
    
    
    public static boolean isLive(String[] contest)
    {
        long now = OffsetDateTime.now().toInstant().toEpochMilli();
        return Long.parseLong(contest[STARTTIME])<now && Long.parseLong(contest[ENDTIME])>now;
    }
    
    final public static int SERVERID    = 0;
    final public static int CONTESTNAME = 1;
    final public static int CHANNELID   = 2;
    final public static int STARTTIME   = 3;
    final public static int ENDTIME     = 4;
    final public static int DETAILS     = 5;
    final public static int STATUS      = 6;
}
