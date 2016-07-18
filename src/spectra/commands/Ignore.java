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
public class Ignore extends Command {
    private final Settings settings;
    public Ignore(Settings settings)
    {
        this.settings = settings;
        this.command = "ignore";
        this.availableInDM = false;
        this.help = "makes "+SpConst.BOTNAME+" ignore a user, role, or channel";
        this.longhelp = "This command is used to make "+SpConst.BOTNAME+" ignore a user, a role, or a channel. "
                + ""+SpConst.BOTNAME+" will not respond to commands or text by these users, users with these roles, "
                + "or messages in these channels. However, Admins will not be ignored.";
        this.level = PermLevel.ADMIN;
        this.arguments = new Argument[]{
            new Argument("add|list|remove",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new IgnoreAdd(),
            new IgnoreList(),
            new IgnoreRemove()
        };
    }
    
    private class IgnoreList extends Command
    {
        private IgnoreList()
        {
            this.command = "list";
            this.availableInDM = false;
            this.help = "shows the users, roles, and channels ignored on the server";
            this.longhelp = "This command lists the users, roles, and channels that are ignored on the server.";
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String ignores = settings.getSettingsForGuild(event.getGuild().getId())[Settings.IGNORELIST];
        StringBuilder builder = new StringBuilder();
        if(ignores==null || ignores.equals(""))
        {
            builder.append(SpConst.WARNING+"Nothing is currently ignored on the server!");
        }
        else
        {
            builder.append("\uD83D\uDEAB Current ignores on **").append(event.getGuild().getName()).append("**:");
            for(String id: ignores.split("\\s+"))
            {
                if(id.startsWith("u"))
                {
                    User u = event.getJDA().getUserById(id.substring(1));
                    if(u!=null)
                        builder.append("\nUser: **").append(u.getUsername()).append("** #").append(u.getDiscriminator());
                    else
                        builder.append("\nUser with ID: ").append(id.substring(1));
                }
                else if (id.startsWith("c"))
                {
                    TextChannel chan = event.getJDA().getTextChannelById(id.substring(1));
                    if(chan!=null)
                        builder.append("\nChannel: <#").append(id.substring(1)).append(">");
                }
                else if (id.startsWith("r"))
                {
                    event.getGuild().getRoles().stream().filter((r) -> (r.getId().equals(id.substring(1)))).forEach((r) -> {
                        builder.append("\nRole: *").append(r.getName()).append("*");
                    });
                }
            }
        }
        builder.append("\nSee `"+SpConst.PREFIX+"ignore help` for how to add or remove ignores");
        Sender.sendResponse(builder.toString(), event);
        return true;
        }
        
    }
    
    private class IgnoreAdd extends Command
    {
        private IgnoreAdd()
        {
            this.command = "add";
            this.help = "adds a user, channel, or role to the ignore list";
            this.longhelp = "This command adds a user, channel, or role to the list that "+SpConst.BOTNAME+" ignores.";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("user|role|channel",Argument.Type.SHORTSTRING,true),
                new Argument("name",Argument.Type.LONGSTRING,true)
            };
            this.children = new Command[]{
                new IgnoreAddType("channel",Argument.Type.TEXTCHANNEL),
                new IgnoreAddType("role",Argument.Type.ROLE),
                new IgnoreAddType("user",Argument.Type.LOCALUSER)
            };
        }
    }
    
    private class IgnoreAddType extends Command
    {
        private IgnoreAddType(String command, Argument.Type type)
        {
            this.command = command;
            this.arguments = new Argument[]{
                new Argument(command+"name",type,true)
            };
            this.help = "adds a "+command+" to the ignore list";
            this.longhelp = "This command adds a "+command+" to the ignore list.";
            if(command.equals("role"))
                this.longhelp+=" If the @\u200Beveryone role is added, "+SpConst.BOTNAME+" will ignore all users without roles.";
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
                id = ((Role)args[0]).getId();
                name = "*"+((Role)args[0]).getName()+"*";
            }
            else if(args[0] instanceof TextChannel)
            {
                id = ((TextChannel)args[0]).getId();
                name = "<#"+id+">";
            }
            id = command.charAt(0)+id;
            String ignores = settings.getSettingsForGuild(event.getGuild().getId())[Settings.IGNORELIST];
            if(ignores==null)
                ignores="";
            if(ignores.contains(id))
            {
                Sender.sendResponse(SpConst.ERROR+"That "+command+" is already being ignored!", event);
                return false;
            }
            settings.setSetting(event.getGuild().getId(), Settings.IGNORELIST, (ignores+" "+id).trim());
            Sender.sendResponse(SpConst.SUCCESS+name+" is now being ignored on **"+event.getGuild().getName()+"**", event);
            return true;
        }
    }
    
    private class IgnoreRemove extends Command
    {
        private IgnoreRemove()
        {
            this.command = "remove";
            this.help = "removes a user, channel, or role from the ignore list";
            this.longhelp = "This command removes a user, channel, or role from the list that "+SpConst.BOTNAME+" ignores.";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("user|role|channel",Argument.Type.SHORTSTRING,true),
                new Argument("name",Argument.Type.LONGSTRING,true)
            };
            this.children = new Command[]{
                new IgnoreRemoveType("channel",Argument.Type.TEXTCHANNEL),
                new IgnoreRemoveType("role",Argument.Type.ROLE),
                new IgnoreRemoveType("user",Argument.Type.LOCALUSER)
            };
        }
    }
    
    private class IgnoreRemoveType extends Command
    {
        private IgnoreRemoveType(String command, Argument.Type type)
        {
            this.command = command;
            this.arguments = new Argument[]{
                new Argument(command+"name",type,true)
            };
            this.help = "removes a "+command+" from the ignore list";
            this.longhelp = "This command removes a "+command+" from the ignore list.";
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
                id = ((Role)args[0]).getId();
                name = "*"+((Role)args[0]).getName()+"*";
            }
            else if(args[0] instanceof TextChannel)
            {
                id = ((TextChannel)args[0]).getId();
                name = "<#"+id+">";
            }
            id = command.charAt(0)+id;
            String ignores = settings.getSettingsForGuild(event.getGuild().getId())[Settings.IGNORELIST];
            if(ignores==null)
                ignores="";
            if(!(" "+ignores+" ").contains(" "+id+" "))
            {
                Sender.sendResponse(SpConst.ERROR+"That "+command+" is not being ignored!", event);
                return false;
            }
            settings.setSetting(event.getGuild().getId(), Settings.IGNORELIST, (" "+ignores+" ").replace(" "+id+" ", " ").trim());
            Sender.sendResponse(SpConst.SUCCESS+name+" is no longer ignored on **"+event.getGuild().getName()+"**", event);
            return true;
        }
    }
}
