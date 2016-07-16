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

import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.MiscUtil;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ChannelCmd extends Command {
    public ChannelCmd()
    {
        this.command = "channel";
        this.help = "gets information about a channel";
        this.longhelp = "This command displays information about the given channel, or the current channel "
                + "if none is provided. This can be used to show information about \"hidden\" channels as well. "
                + "This is public information ("+SpConst.BOTNAME+" doesn't need any permissions to view it), so please "
                + "keep important or sensitive information out of channel topics. Use pins instead.";
        this.arguments = new Argument[]{
            new Argument("channel",Argument.Type.TEXTCHANNEL,false)
        };
        this.availableInDM = false;
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        TextChannel channel = (TextChannel)(args[0]);
        if(channel==null)
            channel = event.getTextChannel();
        String info = "\uD83D\uDCFA Information about <#"+channel.getId()+">\n";
        info += SpConst.LINESTART+"Server: **"+event.getGuild().getName()+"**\n";
        info += SpConst.LINESTART+"Channel ID: **"+channel.getId()+"**\n";
        info += SpConst.LINESTART+"Creation: **"+MiscUtil.getCreationTime(channel.getId()).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n";
        info += SpConst.LINESTART+"Num Users: **"+channel.getUsers().size()+"**";
        if(channel.getTopic()!=null && !channel.getTopic().trim().equals(""))
            info += "\n"+SpConst.LINESTART+"__**Topic**__:\n"+FormatUtil.demention(channel.getTopic());
        Sender.sendResponse(info, event);
        return true;
    }
}
