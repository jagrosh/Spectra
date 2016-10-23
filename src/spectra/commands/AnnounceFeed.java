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

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Feeds;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AnnounceFeed extends Command {
    private final FeedHandler handler;
    private final Feeds feeds;
    public AnnounceFeed(FeedHandler handler, Feeds feeds)
    {
        this.handler = handler;
        this.feeds = feeds;
        this.level = PermLevel.JAGROSH;
        this.command = "announcefeed";
        this.arguments = new Argument[]{
            new Argument("text",Argument.Type.LONGSTRING,true)
        };
        this.help = "send to all announcements feeds";
        this.longhelp = "This command sends a message to all existing announcements feeds (across all servers). "
                + "Make sure to check for spelling mistakes first!";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String text = (String)args[0];
        List<Guild> list = new ArrayList<>();
        feeds.findGuildsForFeedType(Feeds.Type.ANNOUNCEMENTS).stream().filter((id) -> (event.getJDA().getGuildById(id)!=null)).forEach((id) -> {
            list.add(event.getJDA().getGuildById(id));
        });
        handler.submitText(Feeds.Type.ANNOUNCEMENTS, list, text);
        Sender.sendResponse(SpConst.SUCCESS+"Sent to **"+list.size()+"** guilds", event);
        return true;
    }
}
