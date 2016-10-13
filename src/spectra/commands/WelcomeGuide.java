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

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Guides;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class WelcomeGuide extends Command {
    private final Guides guides;
    public WelcomeGuide(Guides guides)
    {
        this.guides = guides;
        this.command = "welcomeguide";
        this.aliases = new String[]{"newuserguide","welcomeme"};
        this.cooldown = 300;
        this.cooldownKey = (event) -> event.getGuild().getId()+"|"+event.getAuthor().getId()+"|welcomeme";
        this.help = "re-sends the server's welcome guide";
        this.longhelp = "This command sends the server's current welcome DM to the user who uses the command. "
                + "This is useful if and admin changes the DM, or if the user misses the DM.";
        this.availableInDM = false;
        this.cooldown = 60;
        this.cooldownKey = event -> event.getAuthor().getId()+"|"+event.getGuild().getId()+"|welcomeguide";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String[] guide = guides.get(event.getGuild().getId());
        if(guide==null)
        {
            Sender.sendResponse(SpConst.WARNING+"No Welcome Guide exists for **"+event.getGuild().getName()+"**", event);
            return false;
        }
        boolean exists = false;
        for(int i=1; i<guide.length; i++)
            if(guide[i]!=null && !guide[i].equals(""))
            {
                Sender.sendPrivate(guide[i], event.getAuthor().getPrivateChannel());
                exists = true;
            }
        if(exists)
        {
            Sender.sendResponse(SpConst.SUCCESS+"Please check your Direct Messages for the Welcome Guide", event);
            return true;
        }
        else
        {
            Sender.sendResponse(SpConst.WARNING+"No Welcome Guide pages have been set for **"+event.getGuild().getName()+"**", event);
            return false;
        }
    }
}
