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
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Prefix extends Command {
    private final Settings settings;
    public Prefix(Settings settings)
    {
        this.settings = settings;
        this.command = "prefix";
        this.availableInDM= false;
        this.help = "shows or sets prefixes";
        this.longhelp = "This command is for adding, removing, and viewing the prefixes for the server. "
                + "Multiple prefixes can be set. The prefix "+SpConst.PREFIX+" is permanent and cannot be "
                + "removed. The prefix "+SpConst.ALTPREFIX+" is enabled by default, but can be removed.";
        this.level = PermLevel.ADMIN;
        this.arguments = new Argument[]{
            new Argument("add|list|remove",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new PrefixAdd(),
            new PrefixList(),
            new PrefixRemove()
        };
    }
    
    private class PrefixAdd extends Command
    {
        private PrefixAdd()
        {
            this.command = "add";
            this.availableInDM = false;
            this.help = "add a prefix";
            this.longhelp = "This command adds a prefix. Prefixes already on the list "
                    + "cannot be added. Prefixes may contain spaces and other special characters.";
            this.arguments = new Argument[]{
                new Argument("prefix",Argument.Type.LONGSTRING,true,1,50)
            };
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String prefix = (String)args[0];
            if(prefix.equalsIgnoreCase(SpConst.PREFIX))
            {
                Sender.sendResponse(SpConst.ERROR+"`"+SpConst.PREFIX+"` is a permanent prefix", event);
                return false;
            }
            String[] prefixes = Settings.prefixesFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.PREFIXES]);
            StringBuilder builder = new StringBuilder();
            for(String prfx : prefixes)
            {
                if(prfx.equalsIgnoreCase(prefix))
                {
                    Sender.sendResponse(SpConst.ERROR+"`"+prfx+"` is already a prefix!", event);
                    return false;
                }
                if(!prfx.equalsIgnoreCase(SpConst.PREFIX))
                    builder.append(prfx).append((char)29);
            }
            builder.append(prefix);
            settings.setSetting(event.getGuild().getId(), Settings.PREFIXES, builder.toString());
            Sender.sendResponse(SpConst.SUCCESS+"`"+prefix+"` has been added as a prefix", event);
            return true;
        }
    }
    
    private class PrefixRemove extends Command
    {
        private PrefixRemove()
        {
            this.command = "remove";
            this.availableInDM = false;
            this.help = "removes a prefix";
            this.longhelp = "This command removes a prefix from the list. The prefix "+SpConst.PREFIX+" is permanent and cannot be removed.";
            this.arguments = new Argument[]{
                new Argument("prefix",Argument.Type.LONGSTRING,true)
            };
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String prefix = (String)args[0];
            if(prefix.equalsIgnoreCase(SpConst.PREFIX))
            {
                Sender.sendResponse(SpConst.ERROR+"`"+SpConst.PREFIX+"` is a permanent prefix", event);
                return false;
            }
            String[] prefixes = Settings.prefixesFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.PREFIXES]);
            StringBuilder builder = new StringBuilder();
            boolean found = false;
            for(String prfx : prefixes)
            {
                if(prfx.equalsIgnoreCase(prefix))
                    found = true;
                else if(!prfx.equalsIgnoreCase(SpConst.PREFIX))
                    builder.append((char)29).append(prfx);
            }
            if(!found)
            {
                Sender.sendResponse(SpConst.ERROR+"`"+prefix+"` is not a prefix, so it could not be removed", event);
                return false;
            }
            String newprefixes = builder.toString();
            if(!newprefixes.equals(""))
                newprefixes = newprefixes.substring(1);
            settings.setSetting(event.getGuild().getId(), Settings.PREFIXES, newprefixes);
            Sender.sendResponse(SpConst.SUCCESS+"Prefix `"+prefix+"` has been removed", event);
            return true;
        }
    }
    
    private class PrefixList extends Command
    {
        private PrefixList()
        {
            this.command = "list";
            this.availableInDM= false;
            this.help = "lists prefixes";
            this.longhelp = "This command lists all of the prefixes that "+SpConst.BOTNAME+" will respond to on the current server.";
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] prefixes = Settings.prefixesFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.PREFIXES]);
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Prefixes on **"+event.getGuild().getName()+"**:\n");
            for(String prfx: prefixes)
                builder.append(" `").append(prfx).append("`");
            builder.append("\nSee `"+SpConst.PREFIX+"prefix help` for how to add or remove prefixes");
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
}
