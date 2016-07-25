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

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Nick extends Command {
    public Nick()
    {
        this.command = "nick";
        this.hidden = true;
        this.availableInDM = false;
        this.help = "sets nickname";
        this.longhelp = "this command is used to set a user's nickname if the user has the "
                + "Change Nickname permission and "+SpConst.BOTNAME+" has the Manage Nicknames "
                + "permissions (for mobile users)";
        this.arguments = new Argument[]{
            new Argument("nickname",Argument.Type.LONGSTRING,false)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        //this command only exists for mobile users to set their nickname; we want to silently fail unless
        //the bot actually has power to manage nicknames on the server
        if(!PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.NICKNAME_MANAGE, event.getGuild()))
            return false;
        if(!PermissionUtil.checkPermission(event.getAuthor(), Permission.NICKNAME_CHANGE, event.getGuild()))
            return false;
        if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), event.getAuthor(), event.getGuild()))
            return false;
        String nick = args[0]==null ? null : (String)args[0];
        if(nick==null)
        {
            event.getGuild().getManager().setNickname(event.getAuthor(), null);
            Sender.sendResponse(SpConst.SUCCESS+"Your nickname on this server has been reset.", event);
            return true;
        }
        else if (nick.length()<1 || nick.length()>32)
        {
            Sender.sendResponse(SpConst.ERROR+"Your nickname could not be changed on this server.", event);
            return false;
        }
        else
        {
            event.getGuild().getManager().setNickname(event.getAuthor(), nick);
            Sender.sendResponse(SpConst.SUCCESS+"Your nickname on this server has been changed to **"+nick+"**", event);
            return true;
        }
    }
    
}
