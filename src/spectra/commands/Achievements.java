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

import java.time.OffsetDateTime;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Donators;
import spectra.datasources.Profiles;
import spectra.datasources.SavedNames;
import spectra.datasources.Tags;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Achievements extends Command {
    private final Profiles profiles;
    private final Tags tags;
    private final Donators donators;
    private final SavedNames names;
    
    public Achievements(Profiles profiles, Tags tags, Donators donators, SavedNames names)
    {
        this.profiles = profiles;
        this.tags = tags;
        this.donators = donators;
        this.names = names;
        this.command = "achievements";
        this.aliases = new String[]{"achieve","awards"};
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.USER,false)
        };
        this.children = new Command[]{
            new AchieveList()
        };
        this.help = "shows a user's "+SpConst.BOTNAME+" achievements";
        this.longhelp = "This command shows the specified user's (or your, with no input) achievements. These are small badges "
                + "to show off your use and mastery of "+SpConst.BOTNAME;
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User user = args[0]==null ? event.getAuthor() : (User)args[0];
        StringBuilder builder = new StringBuilder();
        int num = 0;
        for(Achievement ac : list)
            if(ac.hasAchievement(user, num))
            {
                builder.append("\n**").append(ac.name).append("**");
                num++;
            }
        Sender.sendResponse("**"+user.getUsername()+"**'s Achievements ("+num+"/"+list.length+"):"+builder.toString(), event);
        return true;
    }
    
    private class AchieveList extends Command
    {
        private AchieveList()
        {
            this.command = "list";
            this.help = "shows the list of all achievements";
            this.longhelp = "This command shows the full achievements list, with short descriptions of how to get each one.";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            StringBuilder builder = new StringBuilder("__**"+SpConst.BOTNAME+"** Achievements:__\n");
            for(Achievement ac : list)
                builder.append("\n**").append(ac.name).append("** - ").append(ac.description);
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private final Achievement[] list = new Achievement[]{
        new Achievement("\uD83D\uDCDD Profiler","Set up a profile"){
        @Override
        boolean hasAchievement(User user, int num) {
            return profiles.get(user.getId())!=null;
        }},
        new Achievement("\uD83D\uDD70 Timekeeper","Have a valid timezone set"){
        @Override
        boolean hasAchievement(User user, int num) {
            return profiles.get(user.getId())!=null && Timefor.formatTimezone(profiles.get(user.getId())[Profiles.indexForField("timezone")])!=null;
        }},
        new Achievement("\uD83C\uDFF7 Tagmaker","Create at least one tag"){
        @Override
        boolean hasAchievement(User user, int num) {
            return !tags.findTagsByOwner(user, true).isEmpty();
        }},
        new Achievement("\uD83D\uDCDF JAGzone","Join jagrosh's bot server"){
        @Override
        boolean hasAchievement(User user, int num) {
            return user.getJDA().getGuildById(SpConst.JAGZONE_ID).isMember(user);
        }},
        new Achievement("\uD83D\uDCB5 Donator","Become a donator"){
        @Override
        boolean hasAchievement(User user, int num) {
            return donators.get(user.getId())!=null;
        }},
        new Achievement("\uD83D\uDCDB Renamed","Have at least one recorded username change"){
        @Override
        boolean hasAchievement(User user, int num) {
            return names.getNames(user.getId())!=null;
        }},
        new Achievement("\uD83C\uDFC5 "+SpConst.BOTNAME+" Pro","Share 30 days of server time, and have at least 3 other achievements"){
        @Override
        boolean hasAchievement(User user, int num) {
            if(num<3)
                return false;
            return user.getJDA().getGuilds().stream().anyMatch((g) -> 
                    (g.isMember(user) && g.getJoinDateForUser(user).plusDays(30).isBefore(OffsetDateTime.now()) 
                     && g.getJoinDateForUser(user.getJDA().getSelfInfo()).plusDays(30).isBefore(OffsetDateTime.now())));
        }},
    };
    
    private abstract class Achievement
    {
        String name;
        String description;
        public Achievement(String name, String description)
        {
            this.name = name;
            this.description = description;
        }
        abstract boolean hasAchievement(User user, int num);
    }
}