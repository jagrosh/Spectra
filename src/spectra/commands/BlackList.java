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
import spectra.datasources.GlobalLists;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class BlackList extends Command {
    private final GlobalLists lists;
    public BlackList(GlobalLists lists)
    {
        this.lists = lists;
        this.command = "blacklist";
        this.level = PermLevel.JAGROSH;
        this.help = "modifies the blacklist";
        this.children = new Command[]{
            new BLAdd(),
            new BLCheck(),
            new BLList(),
            new BLRemove()
        };
        this.longhelp = "This command is used to blacklist users or servers from using "+SpConst.BOTNAME;
    }
    
    private class BLAdd extends Command {
        private BLAdd()
        {
            this.command = "add";
            this.level = PermLevel.JAGROSH;
            this.help = "adds to the blacklist";
            this.longhelp = "This command adds an entity to the blacklist";
            this.arguments = new Argument[]{
                new Argument("ID",Argument.Type.SHORTSTRING,true),
                new Argument("reason",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            String reason = (String)args[1];
            if(!id.matches("\\d+"))
            {
                Sender.sendResponse(SpConst.ERROR+"Not a valid ID", event);
                return false;
            }
            if(lists.getState(id)==GlobalLists.ListState.BLACKLIST)
            {
                Sender.sendResponse(SpConst.WARNING+"**ID `"+id+"` is already blacklisted!**:\n"+lists.getBlacklistReason(id), event);
                return false;
            }
            String[] entry = new String[lists.getSize()];
            entry[GlobalLists.ID] = id;
            entry[GlobalLists.LISTTYPE] = "BLACKLIST";
            entry[GlobalLists.REASON] = reason;
            if(event.getJDA().getUserById(id)!=null)
                entry[GlobalLists.IDTYPE] = "USER";
            else if (event.getJDA().getGuildById(id)!=null)
                entry[GlobalLists.IDTYPE] = "GUILD";
            else
                entry[GlobalLists.IDTYPE] = "UNKNOWN";
            lists.set(entry);
            Sender.sendResponse(SpConst.SUCCESS+entry[GlobalLists.IDTYPE]+" with ID `"+id+"` added to blacklist", event);
            return true;
        }
    }
    
    private class BLRemove extends Command {
        private BLRemove()
        {
            this.command = "remove";
            this.level = PermLevel.JAGROSH;
            this.help = "removes from the blacklist";
            this.longhelp = "This command removes an entity from the blacklist";
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
            if(lists.getState(id)!=GlobalLists.ListState.BLACKLIST)
            {
                Sender.sendResponse(SpConst.WARNING+"**ID `"+id+"` is not blacklisted!**:\n"+lists.getBlacklistReason(id), event);
                return false;
            }
            String type = lists.get(id)[GlobalLists.IDTYPE];
            lists.remove(id);
            Sender.sendResponse(SpConst.SUCCESS+type+" with ID `"+id+"` removed from blacklist", event);
            return true;
        }
    }
    
    private class BLList extends Command {
        private BLList()
        {
            this.command = "list";
            this.level = PermLevel.JAGROSH;
            this.help = "lists the blacklist";
            this.longhelp = "This command lists the entities on the blacklist";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** blacklist:");
            lists.getList(GlobalLists.ListState.BLACKLIST).stream().forEach((str) -> {
                builder.append("\n").append(str);
            });
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private class BLCheck extends Command {
        private BLCheck()
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
            if(lists.getState(id)!=GlobalLists.ListState.BLACKLIST)
            {
                Sender.sendResponse(SpConst.WARNING+"**ID `"+id+"` is not blacklisted!**:\n"+lists.getBlacklistReason(id), event);
                return false;
            }
            String type = lists.get(id)[GlobalLists.IDTYPE];
            String reason = lists.getBlacklistReason(id);
            Sender.sendResponse(SpConst.SUCCESS+type+" with ID `"+id+"`:\n"+reason, event);
            return true;
        }
    }
}
