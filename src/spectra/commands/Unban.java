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
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.tempdata.LogInfo;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Unban extends Command {
    private final LogInfo loginfo;
    public Unban(LogInfo loginfo)
    {
        this.loginfo = loginfo;
        this.command = "unban";
        this.help = "unbans a user";
        this.longhelp = "This command unbans a user from the server, allowing them to rejoin the server.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.BANNEDUSER,true,"for"),
            new Argument("reason",Argument.Type.LONGSTRING,false)
        };
        this.availableInDM=false;
        this.level = PermLevel.MODERATOR;
        this.requiredPermissions = new Permission[] {
            Permission.BAN_MEMBERS
        };
        this.children = new Command[]{
            new UnbanHack()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        String reason = args[1]==null?null:(String)(args[1]);
        if(reason==null)
            reason = "[no reason specified]";

        //attempt to unban
        try
        {
            loginfo.addInfo(target.getId(), LogInfo.Type.UNBAN, event.getAuthor().getUsername(), event.getAuthor().getDiscriminator(), reason);
            event.getGuild().getManager().unBan(target.getId());
            Sender.sendResponse(SpConst.SUCCESS+"**"+target.getUsername()+"** was unbanned from the server \uD83D\uDD27", event);
            //handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                //"\uD83D\uDD27 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        //+" unbanned **"+target.getUsername()+"** for "+reason);
            return true;
        } 
        catch(Exception e) 
        {
            Sender.sendResponse(SpConst.ERROR+"Failed to unban **"+target.getUsername()+"**", event);
            return false;
        }
        finally
        {
            try{Thread.sleep(5000);}catch(InterruptedException e){}
            loginfo.removeInfo(target.getId(), LogInfo.Type.UNBAN);
        }
    }
    
    private class UnbanHack extends Command {
        private UnbanHack()
        {
            this.command = "byid";
            this.aliases = new String[]{"hack"};
            this.help = "unbans a user by id";
            this.longhelp = "This command unbans a user, given their ID";
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
            String reason = args[1]==null?null:(String)(args[1]);
            if(reason==null)
                reason = "[no reason specified]";
            List<User> list = event.getGuild().getManager().getBans();
            for(User u: list)
                if(id.equals(u.getId()))
                {
                    try
                    {
                        loginfo.addInfo(id, LogInfo.Type.UNBAN, event.getAuthor().getUsername(), event.getAuthor().getDiscriminator(), reason);
                        event.getGuild().getManager().unBan(id);
                        Sender.sendResponse(SpConst.SUCCESS+(u.getUsername()==null ? "User with ID:"+id : "**"+u.getUsername()+"**")+" was unbanned from the server \uD83D\uDD27", event);
                        //handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                            //"\uD83D\uDD27 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                                    //+" unbanned "+(u.getUsername()==null ? "User with ID:"+id : "**"+u.getUsername()+"**")+" for "+reason);
                        return true;
                    } 
                    catch(Exception e) 
                    {
                        Sender.sendResponse(SpConst.ERROR+"Failed to unban "+(u.getUsername()==null ? "User with ID:"+id : "**"+u.getUsername()+"**"), event);
                        return false;
                    }
                    finally
                    {
                        try{Thread.sleep(5000);}catch(InterruptedException e){}
                        loginfo.removeInfo(id, LogInfo.Type.UNBAN);
                    }
                }
            Sender.sendResponse(SpConst.ERROR+"A user with that ID was not found on the ban list.", event);
            return false;
            }
    }
}
