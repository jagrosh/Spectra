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
package spectra.commands;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Profiles;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Timefor extends Command {
    private final Profiles profiles;
    public Timefor(Profiles profiles)
    {
        this.profiles = profiles;
        this.command = "timefor";
        this.aliases = new String[]{"tf","time"};
        this.help = "shows the current time for a user if they have set the `timezone` field in their profile";
        this.longhelp = "This command uses the current time in conjunction with the 'timezone' field in a user's "
                + "profile to determine what time it is for them.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,false)
        };
        this.children = new Command[]{
            new TimeforList()
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User user = args[0]==null ? event.getAuthor() : (User)args[0];
        String[] profile = profiles.get(user.getId());
        if(profile==null)
        {
            Sender.sendResponse(SpConst.WARNING+"**"+user.getUsername()+"** has not created a profile", event);
            return false;
        }
        String zone = profile[Profiles.indexForField("timezone")];
        if(zone==null || zone.equals(""))
        {
            Sender.sendResponse(SpConst.WARNING+"**"+user.getUsername()+"** has not set the `timezone` field in their profile", event);
            return false;
        }
        ZoneId zi = formatTimezone(zone);
        if(zi==null)
        {
            Sender.sendResponse(SpConst.ERROR+"The timezone for **"+user.getUsername()+"** could not be parsed: "+zone, event);
            return false;
        }
        ZonedDateTime t = event.getMessage().getTime().atZoneSameInstant(zi);
        String time = t.format(DateTimeFormatter.ofPattern("h:mma"));
        String time24 = t.format(DateTimeFormatter.ofPattern("HH:mm"));
        Sender.sendResponse("\u231A Current time for **"+user.getUsername()+"** is `"+time+"` (`"+time24+"`)", event);
        return true;
    }
    
    private class TimeforList extends Command
    {
        private TimeforList()
        {
            this.command = "timezones";
            this.aliases = new String[]{"list"};
            this.help = "lists the valid timezones";
            this.longhelp = "This command provides a list of all available timezones. Note that these are separated by spaces.";
            this.cooldown = 300;
            this.cooldownKey = event -> event.getAuthor().getId()+"|timezonelist";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"List of valid *timezones*:\n");
            ZoneId.getAvailableZoneIds().stream().forEach((str) -> {
                builder.append(str).append(" ");
            });
            Sender.sendResponse(builder.toString().trim(), event);
            return true;
        }
        
    }
    
    public static ZoneId formatTimezone(String zone)
    {
        if(zone==null)
            return null;
        int i1 = zone.indexOf(")");
        if(i1!=-1)
        {
        int i2 = zone.lastIndexOf("(",i1);
        if(i2!=-1)
            zone=zone.substring(i2+1,i1);
        }
        zone = zone.replace(" ", "")
                .replace("(?i)gmt", "GMT")
                .replace("(?i)utc", "UTC")
                .replace("([+-])(\\d:\\d\\d)", "$10$2")
                ;
        try {
            return ZoneId.of(zone);
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
