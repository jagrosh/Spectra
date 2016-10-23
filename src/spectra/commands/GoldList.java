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
public class GoldList extends Command {
    private final GlobalLists lists;
    public GoldList(GlobalLists lists)
    {
        this.lists = lists;
        this.command = "goldlist";
        this.level = PermLevel.JAGROSH;
        this.help = "modifies the goldlist";
        this.children = new Command[]{
            new GLAdd(),
            //new WLCheck(),
            new GLList(),
            new GLRemove()
        };
        this.longhelp = "This command is used to goldlist servers";
    }
    
    private class GLAdd extends Command {
        private GLAdd()
        {
            this.command = "add";
            this.level = PermLevel.JAGROSH;
            this.help = "adds a server to the goldlist";
            this.longhelp = "This command adds a server to the goldlist. Being on the goldlist provides extra commands and shorter cooldowns.";
            this.arguments = new Argument[]{
                new Argument("ID",Argument.Type.SHORTSTRING,true),
                //new Argument("reason",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            //String reason = (String)args[1];
            Guild guild = event.getJDA().getGuildById(id);
            if(guild==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Not a valid Guild ID", event);
                return false;
            }
            if(lists.getState(id)==GlobalLists.ListState.GOLDLIST)
            {
                Sender.sendResponse(SpConst.WARNING+"**"+guild.getName()+"** is already goldlisted!", event);
                return false;
            }
            String[] entry = new String[lists.getSize()];
            entry[GlobalLists.ID] = id;
            entry[GlobalLists.LISTTYPE] = "GOLDLIST";
            entry[GlobalLists.REASON] = guild.getName();
            entry[GlobalLists.IDTYPE] = "GUILD";
            lists.set(entry);
            Sender.sendResponse(SpConst.SUCCESS+entry[GlobalLists.IDTYPE]+" with ID `"+id+"` ("+guild.getName()+") added to goldlist", event);
            return true;
        }
    }
    
    private class GLRemove extends Command {
        private GLRemove()
        {
            this.command = "remove";
            this.level = PermLevel.JAGROSH;
            this.help = "removes a guild the goldlist";
            this.longhelp = "This command removes a guild from the goldlist";
            this.arguments = new Argument[]{
                new Argument("ID",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            Guild guild = event.getJDA().getGuildById(id);
            if(lists.getState(id)!=GlobalLists.ListState.GOLDLIST)
            {
                Sender.sendResponse(SpConst.WARNING+"**ID `"+id+"` is not goldlisted!**:", event);
                return false;
            }
            String type = lists.get(id)[GlobalLists.IDTYPE];
            lists.remove(id);
            Sender.sendResponse(SpConst.SUCCESS+type+" with ID `"+id+"` ("+(guild==null ? "[???]" : guild.getName())+") removed from goldlist", event);
            return true;
        }
    }
    
    private class GLList extends Command {
        private GLList()
        {
            this.command = "list";
            this.level = PermLevel.JAGROSH;
            this.help = "lists the goldlist";
            this.longhelp = "This command lists the entities on the goldlist";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** goldlist:");
            lists.getList(GlobalLists.ListState.GOLDLIST).stream().forEach((str) -> {
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
