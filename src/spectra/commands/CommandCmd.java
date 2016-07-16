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
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.Spectra;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class CommandCmd extends Command {
    private final Settings settings;
    private final Spectra spectra;
    public CommandCmd(Settings settings, Spectra spectra)
    {
        this.settings = settings;
        this.spectra = spectra;
        this.help = "enables or disables commands";
        this.longhelp = "This command (formerly known as \"toggle\"), is used to enable or disable commands on the server. "
                + "When a command is disabled, it can still be used in any channel with \"{commandname}\" in the topic. For "
                + "example, if speakerphone is disabled, it can still be used in a channel if the topic includes {speakerphone}.";
        this.command = "cmd";
        this.availableInDM = false;
        this.level = PermLevel.ADMIN;
        this.arguments = new Argument[]{
            new Argument("disable|enable|list",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new CmdDisable(),
            new CmdEnable(),
            new CmdList()
        };
    }
    
    private class CmdEnable extends Command
    {
        private CmdEnable()
        {
            this.command = "enable";
            this.help = "enables a command";
            this.longhelp = "This command enables a different command. Any command in the disabled-commands list can be enabled.";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("command",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String cmd = (String)args[0];
            String banlist = settings.getSettingsForGuild(event.getGuild().getId())[Settings.BANNEDCMDS];
            if(banlist==null || banlist.equals(""))
            {
                Sender.sendResponse(SpConst.ERROR+"There are no disabled commands!", event);
                return false;
            }
            banlist = " "+banlist+" ";
            if(!banlist.contains(" "+cmd.toLowerCase()+" "))
            {
                Sender.sendResponse(SpConst.ERROR+"There is no command matching \""+cmd.toLowerCase()+"\" that is disabled!", event);
                return false;
            }
            banlist = banlist.replace(" "+cmd.toLowerCase()+" ", " ").trim();
            settings.setSetting(event.getGuild().getId(), Settings.BANNEDCMDS, banlist);
            Sender.sendResponse(SpConst.SUCCESS+"Command `"+cmd.toLowerCase()+"` has been enabled.", event);
            return true;
        }
    }
    
    private class CmdDisable extends Command
    {
        private CmdDisable()
        {
            this.command = "disable";
            this.help = "disables a command";
            this.longhelp = "This command disables another command. Only regular and moderator commands can be disabled; "
                    + "Admin commands cannot. Any first-level command can be disabled (like tag, but not tag create).";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("command",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String cmd = (String)args[0];
            String banlist = settings.getSettingsForGuild(event.getGuild().getId())[Settings.BANNEDCMDS];
            Command match=null;
            for(Command comm : spectra.getCommandList())
                if(comm.isCommandFor(cmd))
                {
                    match = comm;
                    break;
                }
            if(match==null)
            {
                Sender.sendResponse(SpConst.ERROR+"No command matching \""+cmd+"\" found!", event);
                return false;
            }
            if(match.getLevel().isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"Only regular and moderator commands can be disabled", event);
                return false;
            }
            if(banlist==null)
                banlist="";
            banlist=" "+banlist+" ";
            if(banlist.contains(" "+match.getName()+" "))
            {
                Sender.sendResponse(SpConst.ERROR+"That command is already disabled!", event);
                return false;
            }
            banlist+=match.getName();
            settings.setSetting(event.getGuild().getId(), Settings.BANNEDCMDS, banlist.trim());
            Sender.sendResponse(SpConst.SUCCESS+"Command `"+match.getName()+"` has been disabled.", event);
            return true;
        }
    }
    
    private class CmdList extends Command
    {
        private CmdList()
        {
            this.command = "list";
            this.help = "lists disabled commands";
            this.longhelp = "This command lists the disabled commands on the server. If "
                    + "you see a command on the list that doesn't exist, you should still be "
                    + "able to \"enable\" it (to take it off the list). If you believe your disabled-commands "
                    + "list is corrupted, please contact jagrosh.";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String banlist = settings.getSettingsForGuild(event.getGuild().getId())[Settings.BANNEDCMDS];
            if(banlist==null || banlist.equals(""))
            {
                Sender.sendResponse(SpConst.WARNING+"There are no disabled commands on **"+event.getGuild().getName()+"**", event);
                return true;
            }
            Sender.sendResponse(SpConst.SUCCESS+"Disabled commands on **"+event.getGuild().getName()+"**:\n`"+banlist.replace(" ", "` `")+"`", event);
            return true;
        }
    }
}
