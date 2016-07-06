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

import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
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
public class ModCmd extends Command {
    private final Settings settings;
    public ModCmd(Settings settings)
    { 
        this.settings = settings;
        this.command = "mod";
        this.help = "adds/removes a role/user from the mod list";
        this.children = new Command[]{
            new ModAdd(),
            new ModList(),
            new ModRemove()
        };
        this.availableInDM = false;
        this.arguments = new Argument[]{
            new Argument("add|list|remove",Argument.Type.SHORTSTRING,true)
        };
        this.level = PermLevel.ADMIN;
    }
    
    private class ModAdd extends Command
    {
        private ModAdd()
        {
            this.command = "add";
            this.help = "adds a user or role to the mod list";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("user|role",Argument.Type.SHORTSTRING,true),
                new Argument("name",Argument.Type.LONGSTRING,true)
            };
            this.children = new Command[]{
                new ModAddType("role",Argument.Type.ROLE),
                new ModAddType("user",Argument.Type.LOCALUSER)
            };
        }
    }
    
    private class ModAddType extends Command
    {
        private ModAddType(String command, Argument.Type type)
        {
            this.command = command;
            this.arguments = new Argument[]{
                new Argument(command+"name",type,true)
            };
            this.help = "adds a "+command+" to the mod list";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = "";
            String name ="";
            if(args[0] instanceof User)
            {
                id = ((User)args[0]).getId();
                name = "**"+((User)args[0]).getUsername()+"**";
            }
            else if(args[0] instanceof Role)
            {
                id = "r"+((Role)args[0]).getId();
                name = "*"+((Role)args[0]).getName()+"*";
            }
            String mods = settings.getSettingsForGuild(event.getGuild().getId())[Settings.MODIDS];
            if(mods==null)
                mods="";
            if(mods.contains(id))
            {
                Sender.sendResponse(SpConst.ERROR+"That "+command+" is already a moderator!", event);
                return false;
            }
            settings.setSetting(event.getGuild().getId(), Settings.MODIDS, (mods+" "+id).trim());
            Sender.sendResponse(SpConst.SUCCESS+name+" is now a moderator on **"+event.getGuild().getName()+"**", event);
            return true;
        }
    }
    
    private class ModRemove extends Command
    {
        private ModRemove()
        {
            this.command = "remove";
            this.help = "removes a user or role from the mod list";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("user|role",Argument.Type.SHORTSTRING,true),
                new Argument("name",Argument.Type.LONGSTRING,true)
            };
            this.children = new Command[]{
                new ModRemoveType("role",Argument.Type.ROLE),
                new ModRemoveType("user",Argument.Type.LOCALUSER)
            };
        }
    }
    
    private class ModRemoveType extends Command
    {
        private ModRemoveType(String command, Argument.Type type)
        {
            this.command = command;
            this.arguments = new Argument[]{
                new Argument(command+"name",type,true)
            };
            this.help = "removes a "+command+" from the mod list";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = "";
            String name ="";
            if(args[0] instanceof User)
            {
                id = ((User)args[0]).getId();
                name = "**"+((User)args[0]).getUsername()+"**";
            }
            else if(args[0] instanceof Role)
            {
                id = "r"+((Role)args[0]).getId();
                name = "*"+((Role)args[0]).getName()+"*";
            }
            String mods = settings.getSettingsForGuild(event.getGuild().getId())[Settings.MODIDS];
            if(mods==null)
                mods="";
            if(!(" "+mods+" ").contains(" "+id+" "))
            {
                Sender.sendResponse(SpConst.ERROR+"That "+command+" is not a moderator!", event);
                return false;
            }
            settings.setSetting(event.getGuild().getId(), Settings.MODIDS, (" "+mods+" ").replace(" "+id+" ", " ").trim());
            Sender.sendResponse(SpConst.SUCCESS+name+" is no longer a moderator on **"+event.getGuild().getName()+"**", event);
            return true;
        }
    }
    
    private class ModList extends Command
    {
        private ModList()
        {
            this.command = "list";
            this.availableInDM = false;
            this.help = "shows the users and roles with moderator permissions";
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String mods = settings.getSettingsForGuild(event.getGuild().getId())[Settings.MODIDS];
        StringBuilder builder = new StringBuilder();
        if(mods==null || mods.equals(""))
        {
            builder.append(SpConst.WARNING+"There are no moderators on the server!");
        }
        else
        {
            builder.append("\uD83D\uDC6E Current moderators on **").append(event.getGuild().getName()).append("**:");
            for(String id: mods.split("\\s+"))
            {
                if (id.startsWith("r"))
                {
                    event.getGuild().getRoles().stream().filter((r) -> (r.getId().equals(id.substring(1)))).forEach((r) -> {
                        builder.append("\nRole: *").append(r.getName()).append("*");
                    });
                }
                else
                {
                    User u = event.getJDA().getUserById(id.substring(1));
                    if(u!=null)
                        builder.append("\nUser: **").append(u.getUsername()).append("** #").append(u.getDiscriminator());
                    else
                        builder.append("\nUser with ID: ").append(id.substring(1));
                }
            }
        }
        builder.append("\nSee `"+SpConst.PREFIX+"mod help` for how to add or remove moderators");
        Sender.sendResponse(builder.toString(), event);
        return true;
        }
        
    }
}
