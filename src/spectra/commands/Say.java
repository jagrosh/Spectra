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
public class Say extends Command {
    public Say()
    {
        this.command = "say";
        this.availableInDM = false;
        this.help = "say text and delete the call";
        this.level = PermLevel.ADMIN;
        this.longhelp = "This command can be used by server admins to make "+SpConst.BOTNAME+" say text. "
                + "If the first word in the provided text is a channel mention, "+SpConst.BOTNAME+" will "
                + "say the text there instead.";
        this.arguments = new Argument[]{
            new Argument("text",Argument.Type.LONGSTRING,true)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String text = (String)args[0];
        TextChannel channel=null;
        if(text.matches("<#\\d+>.*"))
        {
            String id = text.substring(2,text.indexOf(">"));
            text = text.substring(text.indexOf(">")+1).trim();
            channel = event.getJDA().getTextChannelById(id);
            if(channel!=null && !channel.getGuild().equals(event.getGuild()))
                channel = null;
        }
        if(channel==null)
            channel = event.getTextChannel();
        if(PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE, channel))
            event.getMessage().deleteMessage();
        if(!Sender.sendMsg(text, channel))
        {
            Sender.sendResponse(SpConst.ERROR, event);
            return false;
        }
        return true;
    }
}
