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
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.GlobalLists;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class WhiteList extends Command {
    private final GlobalLists lists;
    public WhiteList(GlobalLists lists)
    {
        this.lists = lists;
        this.command = "whitelist";
        this.level = PermLevel.JAGROSH;
        this.help = "modifies the whitelist";
        this.children = new Command[]{
            new WLAdd(),
            //new WLCheck(),
            new WLList(),
            new WLRemove()
        };
        this.longhelp = "This command is used to whitelist servers";
    }
    
    private class WLAdd extends Command {
        private WLAdd()
        {
            this.command = "add";
            this.level = PermLevel.JAGROSH;
            this.help = "adds a server to the whitelist";
            this.longhelp = "This command adds a server to the whitelist. Being on the whitelist provides extra commands and shorter cooldowns.";
            this.arguments = new Argument[]{
                new Argument("ID",Argument.Type.SHORTSTRING,true),
                new Argument("details",Argument.Type.LONGSTRING,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            String details = (String)args[1];
            Guild guild = event.getJDA().getGuildById(id);
            if(lists.getState(id)==GlobalLists.ListState.WHITELIST)
            {
                Sender.sendResponse(SpConst.WARNING+(guild==null?"[???]":"**"+guild.getName()+"**")+" is already whitelisted!", event);
                return false;
            }
            String[] entry = new String[lists.getSize()];
            entry[GlobalLists.ID] = id;
            entry[GlobalLists.LISTTYPE] = "WHITELIST";
            entry[GlobalLists.REASON] = guild==null ? details : guild.getName();
            entry[GlobalLists.IDTYPE] = "GUILD";
            lists.set(entry);
            Sender.sendResponse(SpConst.SUCCESS+entry[GlobalLists.IDTYPE]+" with ID `"+id+"` ("+(guild==null ? "???" : guild.getName())+") added to whitelist", event);
            return true;
        }
    }
    
    private class WLRemove extends Command {
        private WLRemove()
        {
            this.command = "remove";
            this.level = PermLevel.JAGROSH;
            this.help = "removes a guild the whitelist";
            this.longhelp = "This command removes a guild from the whitelist";
            this.arguments = new Argument[]{
                new Argument("ID",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            Guild guild = event.getJDA().getGuildById(id);
            if(lists.getState(id)!=GlobalLists.ListState.WHITELIST)
            {
                Sender.sendResponse(SpConst.WARNING+"**ID `"+id+"` is not whitelisted!**:", event);
                return false;
            }
            String type = lists.get(id)[GlobalLists.IDTYPE];
            lists.remove(id);
            Sender.sendResponse(SpConst.SUCCESS+type+" with ID `"+id+"` ("+(guild==null ? "[???]" : guild.getName())+") removed from whitelist", event);
            return true;
        }
    }
    
    private class WLList extends Command {
        private WLList()
        {
            this.command = "list";
            this.level = PermLevel.JAGROSH;
            this.help = "lists the whitelist";
            this.longhelp = "This command lists the entities on the whitelist";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** whitelist:");
            lists.getList(GlobalLists.ListState.WHITELIST).stream().forEach((str) -> {
                builder.append("\n").append(str);
            });
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    /*private class WLCheck extends Command {
        private WLCheck()
        {
            this.command = "check";
            this.level = PermLevel.JAGROSH;
            this.help = "checks an id on the blacklist";
            this.longhelp = "This command displays the reason an entitiy is on the blacklist";
            this.arguments = new Argument[]{
                new Argument("ID",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            if(!id.matches("\\d+"))
            {
                Sender.sendResponse(SpConst.ERROR+"Not a valid ID", event);
                return false;
            }
            if(!lists.isBlacklisted(id))
            {
                Sender.sendResponse(SpConst.WARNING+"**ID `"+id+"` is not blacklisted!**:\n"+lists.getBlacklistReason(id), event);
                return false;
            }
            String type = lists.get(id)[GlobalLists.IDTYPE];
            String reason = lists.getBlacklistReason(id);
            Sender.sendResponse(SpConst.SUCCESS+type+" with ID `"+id+"`:\n"+reason, event);
            return true;
        }
    }*/
}
