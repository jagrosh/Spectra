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

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.misc.SafeEmote;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class BotScan extends Command{
    public BotScan()
    {
        this.command = "botscan";
        this.help = "scans for all bots on the server (may not be 100% accurate)";
        this.longhelp = "This command searches for all bots on the current server. It looks for "
                + "[BOT] accounts, as well as any account with the \"Bots\" role on the Discord"
                + "Bots server.";
        this.availableInDM = false;
        this.level = PermLevel.MODERATOR;
        this.cooldown = 300;
        this.cooldownKey = (event) -> {return event.getGuild().getId()+"|botscan";};
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String list = "";
        int count = 0;
        Guild discordbots = event.getJDA().getGuildById("110373943822540800");
        Role botsrole = null;
        if(discordbots!=null)
            for(Role r: discordbots.getRoles())
                if(r.getId().equals("110374777914417152"))
                    botsrole = r;
        String bot = SafeEmote.BOT.get(event.getJDA())+" ";
        for(User u: event.getGuild().getUsers())
            if(u.isBot() || (botsrole!=null && discordbots!=null && discordbots.isMember(u) && discordbots.getRolesForUser(u).contains(botsrole)))
            {
                list+="\n"+(u.isBot()?bot:"\uD83D\uDC68 ")+u.getUsername()+" #"+u.getDiscriminator();
                count ++;
            }
        String str = SpConst.SUCCESS+"**"+count+"** bots found on **"+event.getGuild().getName()+"**:"+list;
        if(str.length()>2000)
        {
            Sender.sendResponse(SpConst.WARNING+"**"+count+"** bots found, so the list is not shown.", event);
            return true;
        }
        Sender.sendResponse(str, event);
        return true;
    }
}
