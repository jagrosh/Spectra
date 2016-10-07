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

import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Welcome extends Command {
    private final Settings settings;
    public Welcome(Settings settings)
    {
        this.settings = settings;
        this.command = "welcome";
        this.availableInDM= false;
        this.help = "sets the welcome message for the server";
        this.longhelp = "This command sets the welcome message for the server (that is sent when users join). "
                + "This message supports JagTag and can include things like the user's name, server "
                + "information, and more. See `"+SpConst.PREFIX+"tag help` for more information "
                + "about JagTag.";
        this.level = PermLevel.ADMIN;
        this.arguments = new Argument[]{
            new Argument("message",Argument.Type.LONGSTRING,true)
        };
        this.children = new Command[]{
            new WelcomeChannel(),
            new WelcomeClear()
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String message = (String)args[0];
        String current = settings.getSettingsForGuild(event.getGuild().getId())[Settings.WELCOMEMSG];
        String[] parts = new String[]{null,""};
        if(current!=null && !current.equals(""))
        {
            parts = Settings.parseWelcomeMessage(current);
        }
        if(parts[0]!=null)
            message = parts[0]+": "+message;
        settings.setSetting(event.getGuild().getId(), Settings.WELCOMEMSG, message);
        Sender.sendResponse(SpConst.SUCCESS+"The welcome message on **"+event.getGuild().getName()+"** has been set", event);
        return true;
    }
    
    private class WelcomeClear extends Command
    {
        private WelcomeClear()
        {
            this.command = "clear";
            this.aliases = new String[]{"remove","delete"};
            this.availableInDM = false;
            this.help = "clears the server's welcome message";
            this.longhelp = "This command clears the server's welcome message.";
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            settings.setSetting(event.getGuild().getId(), Settings.WELCOMEMSG, "");
            Sender.sendResponse(SpConst.SUCCESS+"The welcome message has been cleared", event);
            return true;
        }
    }
    
    private class WelcomeChannel extends Command
    {
        private WelcomeChannel()
        {
            this.command = "channel";
            this.availableInDM = false;
            this.help = "sets the channel for the server welcome message";
            this.longhelp = "This command sets the channel that the welcome message will send to.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel tchan = (TextChannel)args[0];
            String current = settings.getSettingsForGuild(event.getGuild().getId())[Settings.WELCOMEMSG];
            if(current==null || current.equals(""))
            {
                Sender.sendResponse(SpConst.WARNING+"There is no welcome message set for the server!", event);
                return false;
            }
            String[] parts = Settings.parseWelcomeMessage(current);
            settings.setSetting(event.getGuild().getId(), Settings.WELCOMEMSG, "<#"+tchan.getId()+">:"+parts[1]);
            Sender.sendResponse(SpConst.SUCCESS+"The welcome message will now be sent to <#"+tchan.getId()+">", event);
            return true;
        }
    }
}
