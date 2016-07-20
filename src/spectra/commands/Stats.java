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
import javafx.util.Pair;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.tempdata.Statistics;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Stats extends Command {
    private final Statistics statistics;
    public Stats(Statistics statistics)
    {
        this.statistics = statistics;
        this.command = "stats";
        this.aliases = new String[]{"statistics"};
        this.help = "shows "+SpConst.BOTNAME+" statistics";
        this.longhelp = "This command shows various statistics, including uptime, command usage, and global activity.";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("\uD83D\uDCC8 **General Statistics**:\n")
                .append(SpConst.LINESTART).append("Uptime: ").append(FormatUtil.secondsToTime(statistics.getUptime())).append("\n")
                .append(SpConst.LINESTART).append("Servers: **").append(event.getJDA().getGuilds().size()).append("**\n")
                .append(SpConst.LINESTART).append("Channels: **").append(event.getJDA().getTextChannels().size()).append("** Text, **").append(event.getJDA().getVoiceChannels().size()).append("** Voice\n")
                .append(SpConst.LINESTART).append("Unique Users: **").append(event.getJDA().getUsers().size()).append("**\n")
                .append(SpConst.LINESTART).append("Messages/Hr: **").append(statistics.messagesPerHour()).append("**\n")
                .append(SpConst.LINESTART).append("Feeds/Hr: **").append(statistics.feedsPerHour()).append("**\n\u2328 **Command Statistics**\n")
                .append(SpConst.LINESTART).append("Commands/Hr: **").append(statistics.commandsPerHour()).append("**\n");
        List<Pair<String,Integer>> list = statistics.mostUsedCommands(4);
        if(!list.isEmpty())
            builder.append(SpConst.LINESTART).append("Most used: **").append(list.get(0).getKey()).append("** (").append(list.get(0).getValue()).append("% overall)\n");
        if(list.size()>=4)
            builder.append(SpConst.LINESTART).append("Then: **").append(list.get(1).getKey()).append("** (").append(list.get(1).getValue()).append("%), **")
                    .append(list.get(2).getKey()).append("** (").append(list.get(2).getValue()).append("%), **")
                    .append(list.get(3).getKey()).append("** (").append(list.get(3).getValue()).append("%)\n");
        int size = 0;
        String name = null;
        for(Guild g: event.getJDA().getGuilds())
            if(g.getUsers().size()>size)
            {
                name = g.getName();
                size = g.getUsers().size();
            }
        Pair<String,Integer> loudest = statistics.mostMessagesGuild();
        String loudname = event.getJDA().getGuildById(loudest.getKey())==null ? "???" : event.getJDA().getGuildById(loudest.getKey()).getName();
        Pair<String,Integer> dependent = statistics.mostCommandsGuild();
        String dependname = dependent.getKey().equals("0") ? "Direct Messages" : (event.getJDA().getGuildById(dependent.getKey())==null ? "???" : event.getJDA().getGuildById(dependent.getKey()).getName());
        Pair<String,Integer> mostFeeds = statistics.mostFeedsGuild();
        String mostFeedsName = event.getJDA().getGuildById(mostFeeds.getKey())==null ? "???" : event.getJDA().getGuildById(mostFeeds.getKey()).getName();
        builder.append("\uD83D\uDDA5 **Server Statistics**:\n")
                .append(SpConst.LINESTART).append("Largest: **").append(name).append("** (").append(size).append(" users)\n")
                .append(SpConst.LINESTART).append("Loudest: **").append(loudname).append("** (").append(loudest.getValue()).append("% of messages)\n")
                .append(SpConst.LINESTART).append("Most Used: **").append(dependname).append("** (").append(dependent.getValue()).append("% of commands)\n")
                .append(SpConst.LINESTART).append("Most Feed: **").append(mostFeedsName).append("** (").append(mostFeeds.getValue()).append("% of feed messages)")
                ;
        Sender.sendResponse(builder.toString(), event);
        return true;
    }
}
