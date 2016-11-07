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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Feeds;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Note extends Command {
    private final FeedHandler handler;
    private final Feeds feeds;
    public Note(FeedHandler handler, Feeds feeds)
    {
        this.handler = handler;
        this.feeds = feeds;
        this.command = "note";
        this.help = "adds a note or checks for values in the modlog";
        this.longhelp = "This command adds a note (tied to a user) to the modlog.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,true,"for"),
            new Argument("reason",Argument.Type.LONGSTRING,true)
        };
        this.availableInDM=false;
        this.level = PermLevel.MODERATOR;
        this.children = new Command[]{
            new NoteCheck("check","user",Argument.Type.USER),
            new NoteCheck("checkid","userId",Argument.Type.SHORTSTRING)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        String reason = (String)(args[1]);

        String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
        if(feed==null)
        {
            Sender.sendResponse(SpConst.ERROR+"The modlog feed has not been set up on this server!",event);
            return false;
        }
        else
        {
            Sender.sendResponse(SpConst.SUCCESS+"Note added about **"+target.getUsername()+"** \uD83D\uDCDD", event);
            handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                    "\uD83D\uDCDD **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" noted **"+target.getUsername()+"** (ID:"+target.getId()+") for "+reason);
        }
        return true;
    }
    
    private class NoteCheck extends Command {
        Argument.Type type;
        private NoteCheck(String command, String argname, Argument.Type type)
        {
            this.command = command;
            this.type = type;
            this.help = "checks the modlog for logs about the provided user";
            this.longhelp = "This command searches the modlog for any actions related to the user.";
            this.arguments = new Argument[]{
                new Argument(argname,type,true)
            };
            this.availableInDM=false;
            this.level = PermLevel.MODERATOR;
            this.cooldown = 10;
            this.cooldownKey = event -> event.getGuild().getId()+"|notecheck";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = type==Argument.Type.SHORTSTRING ? (String)args[0] : ((User)args[0]).getId();
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed==null)
            {
                Sender.sendResponse(SpConst.ERROR+"The modlog feed has not been set up on this server!",event);
                return false;
            }
            TextChannel tc = event.getJDA().getTextChannelById(feed[Feeds.CHANNELID]);
            if(tc == null || !tc.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY))
            {
                Sender.sendResponse(SpConst.ERROR+"The modlog feed channel has been deleted or cannot be accessed.",event);
                return false;
            }
            event.getChannel().sendTyping();
            List<Message> logs = tc.getHistory().retrieve(500).stream().filter(m -> m.getRawContent().contains(id)).collect(Collectors.toList());
            if(logs.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"Could not find anything in the modlog for ID `"+id+"` in the past 500 messages!",event);
                return true;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"`"+logs.size()+"` actions found for ID `"+id+"`:");
            Collections.reverse(logs);
            logs.stream().forEach(m -> {
                builder.append("\n`[").append(m.getTime().format(DateTimeFormatter.ofPattern("d MMM uuuu"))).append("]` ").append(FormatUtil.appendAttachmentUrls(m).replaceAll("^`\\[[\\d:]+\\]`\\s+", ""));
            });
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
}
