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

import java.util.List;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Settings;
import spectra.tempdata.LogInfo;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Ban extends Command {
    private final Settings settings;
    private final LogInfo loginfo;
    public Ban(Settings settings, LogInfo loginfo)
    {
        this.settings = settings;
        this.loginfo = loginfo;
        this.command = "ban";
        this.help = "bans a user";
        this.longhelp = "This command bans a user from the server, and deletes any of their messages from within the past 7 days.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.USER,true,"for"),
            new Argument("reason",Argument.Type.LONGSTRING,false)
        };
        this.availableInDM=false;
        this.level = PermLevel.MODERATOR;
        this.requiredPermissions = new Permission[] {
            Permission.BAN_MEMBERS
        };
        this.children = new Command[]{
            new BanHack(),
            new BanList()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        String reason = args[1]==null?null:(String)(args[1]);
        if(reason==null)
            reason = "[no reason specified]";
        PermLevel targetLevel = PermLevel.getPermLevelForUser(target, event.getGuild(), settings.getSettingsForGuild(event.getGuild().getId()));
        //check perm level of other user
        if(targetLevel.isAtLeast(level))
        {
            Sender.sendResponse(SpConst.WARNING+"**"+target.getUsername()+"** cannot be banned because they are listed as "+targetLevel, event);
            return false;
        }
        
        //check if bot can interact with the other user
        if(event.getGuild().isMember(target) && !PermissionUtil.canInteract(event.getJDA().getSelfInfo(), target, event.getGuild()))
        {
            Sender.sendResponse(SpConst.WARNING+"I cannot ban **"+target.getUsername()+"** due to permission hierarchy", event);
            return false;
        }
        
        //attempt to ban
        try
        {
            loginfo.addInfo(target.getId(), LogInfo.Type.BAN, event.getAuthor().getUsername(), event.getAuthor().getDiscriminator(), reason);
            event.getGuild().getManager().ban(target.getId(), 7);
            Sender.sendResponse(SpConst.SUCCESS+"**"+target.getUsername()+"** was banned from the server \uD83D\uDD28", event);
            //handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                    //"\uD83D\uDD28 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()+" banned **"+target.getUsername()+"** for "+reason);
            return true;
        }
        catch(Exception e)
        {
            Sender.sendResponse(SpConst.ERROR+"Failed to ban **"+target.getUsername()+"**", event);
            return false;
        }
        finally
        {
            try{Thread.sleep(5000);}catch(InterruptedException e){}
            loginfo.removeInfo(target.getId(), LogInfo.Type.BAN);
        }
    }
    
    private class BanHack extends Command {
        private BanHack()
        {
            this.command = "byid";
            this.aliases = new String[]{"hack"};
            this.help = "bans a user by id, even if they aren't on the server";
            this.longhelp = "This command bans a user by their ID, even if they are not on the "
                    + "server or "+SpConst.PREFIX+" cannot directly see their account";
            this.arguments = new Argument[]{
                new Argument("id",Argument.Type.SHORTSTRING,true,"for"),
                new Argument("reason",Argument.Type.LONGSTRING,false)
            };
            this.availableInDM=false;
            this.level = PermLevel.MODERATOR;
            this.requiredPermissions = new Permission[] {
                Permission.BAN_MEMBERS
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)(args[0]);
            if(!id.matches("\\d+"))
            {
                Sender.sendResponse(SpConst.ERROR+"`"+id+"` is not a valid ID.", event);
                return false;
            }
            String reason = args[1]==null?null:(String)(args[1]);
            if(reason==null)
                reason = "[no reason specified]";
            else if(reason.toLowerCase().startsWith("for"))
                reason = reason.substring(3).trim();
            User target = event.getJDA().getUserById(id);
            if(target!=null && event.getGuild().isMember(target))
            {
                PermLevel targetLevel = PermLevel.getPermLevelForUser(target, event.getGuild(),settings.getSettingsForGuild(event.getGuild().getId()));
                //check perm level of other user
                if(targetLevel.isAtLeast(level))
                {
                    Sender.sendResponse(SpConst.WARNING+"**"+target.getUsername()+"** cannot be banned because they are listed as "+targetLevel, event);
                    return false;
                }

                //check if bot can interact with the other user
                if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), target, event.getGuild()))
                {
                    Sender.sendResponse(SpConst.WARNING+"I cannot ban **"+target.getUsername()+"** due to permission hierarchy", event);
                    return false;
                }
            }
            //attempt to ban
            try
            {
                loginfo.addInfo(id, LogInfo.Type.BAN, event.getAuthor().getUsername(), event.getAuthor().getDiscriminator(), reason);
                event.getGuild().getManager().ban(id, 7);
                Sender.sendResponse(SpConst.SUCCESS+(target==null ? "User with ID:"+id : "**"+target.getUsername()+"**")+" was banned from the server \uD83D\uDD28", event);
                //handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                        //"\uD83D\uDD28 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()+" banned "+(target==null ? "User with ID:"+id : "**"+target.getUsername()+"**")+" for "+reason);
                return true;
            }
            catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"Failed to ban "+(target==null ? "User with ID:"+id : "**"+target.getUsername()+"**"), event);
                return false;
            }
            finally
            {
                try{Thread.sleep(5000);}catch(InterruptedException e){}
                loginfo.removeInfo(id, LogInfo.Type.BAN);
            }
        }
    }
    
    private class BanList extends Command {
        private BanList()
        {
            this.command = "list";
            this.help = "lists the users currently banned from the server";
            this.longhelp = "This command references the server's banlist and lsits the users.";
            this.availableInDM=false;
            this.level = PermLevel.MODERATOR;
            this.requiredPermissions = new Permission[] {
                Permission.BAN_MEMBERS
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            List<User> list = event.getGuild().getManager().getBans();
            if(list.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There are no banned users!", event);
                return true;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS).append("**").append(list.size()).append("** users banned on **").append(event.getGuild().getName()).append("**:");
            list.stream().forEach((u) -> {
                builder.append("\n**").append(u.getUsername()).append("** (ID:").append(u.getId()).append(")");
            });
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
        
    }
}
