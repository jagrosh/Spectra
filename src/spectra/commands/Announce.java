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
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Announce extends Command {
    public Announce()
    {
        this.command = "announce";
        this.help = "makes an announcement to a role group";
        this.longhelp = "This command makes an announcement that mentions all users with the provided role, "
                + "even if the role is not normally mentionable. The message will be appended with the role mention, unless "
                + "`{role}` is included, which will be replaced by the mention.";
        this.level = PermLevel.ADMIN;
        this.availableInDM = false;
        this.arguments = new Argument[]{
            new Argument("channel",Argument.Type.TEXTCHANNEL,true),
            new Argument("rolename",Argument.Type.ROLE,true,"|"),
            new Argument("announcement text",Argument.Type.LONGSTRING,true)
        };
        this.requiredPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.cooldown = 10;
        this.cooldownKey = event -> event.getGuild().getId()+"|announce";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        TextChannel tc = (TextChannel)args[0];
        Role role = (Role)args[1];
        String content = (String)args[2];
        if(!tc.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
        {
            Sender.sendResponse(String.format(SpConst.NEED_PERMISSION,Permission.MESSAGE_READ+", "+Permission.MESSAGE_WRITE), event);
            return false;
        }
        boolean mentionable = role.isMentionable();
        if(!mentionable)
        {
            if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))
            {
                Sender.sendResponse(SpConst.ERROR+"I cannot make the role *"+role.getName()+"* mentionable because I cannot edit it. Make sure it is listed below my highest role.", event);
                return false;
            }
            role.getManager().setMentionable(true).update();
        }
        if(content.contains("{role}"))
            content = content.replace("{role}", role.getAsMention());
        else
            content = role.getAsMention()+": "+content;
        Sender.sendMsg(content, tc, m -> {
            if(!mentionable)
                role.getManager().setMentionable(false).update();
            Sender.sendResponse(SpConst.SUCCESS+"Announcement sent to <#"+tc.getId()+">!", event);
        });
        return true;
    }
}
