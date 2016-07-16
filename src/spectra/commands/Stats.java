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
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.entities.Tuple;
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
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("\uD83D\uDCC8 **General Statistics**:\n")
                .append(SpConst.LINESTART).append("Uptime: ").append(FormatUtil.secondsToTime(statistics.getUptime())).append("\n")
                .append(SpConst.LINESTART).append("Servers: **").append(event.getJDA().getGuilds().size()).append("**\n")
                .append(SpConst.LINESTART).append("Channels: **").append(event.getJDA().getTextChannels().size()).append("** Text, **").append(event.getJDA().getVoiceChannels().size()).append("** Voice\n")
                .append(SpConst.LINESTART).append("Unique Users: **").append(event.getJDA().getUsers().size()).append("**\n")
                .append(SpConst.LINESTART).append("Messages/Hr: **").append(statistics.messagesPerHour()).append("**\n\n\u2328 **Command Statistics**\n")
                .append(SpConst.LINESTART).append("Commands/Hr: **").append(statistics.commandsPerHour()).append("**\n");
        List<Tuple<String,Integer>> list = statistics.mostUsedCommands(4);
        if(!list.isEmpty())
            builder.append(SpConst.LINESTART).append("Most used: **").append(list.get(0).getFirst()).append("** (").append(list.get(0).getSecond()).append("% overall)\n");
        if(list.size()>=4)
            builder.append(SpConst.LINESTART).append("Then: **").append(list.get(1).getFirst()).append("** (").append(list.get(1).getSecond()).append("%), **")
                    .append(list.get(2).getFirst()).append("** (").append(list.get(2).getSecond()).append("%), **")
                    .append(list.get(3).getFirst()).append("** (").append(list.get(3).getSecond()).append("%)\n");
        int size = 0;
        String name = null;
        for(Guild g: event.getJDA().getGuilds())
            if(g.getUsers().size()>size)
            {
                name = g.getName();
                size = g.getUsers().size();
            }
        Tuple<String,Integer> loudest = statistics.mostMessagesGuild();
        String loudname = event.getJDA().getGuildById(loudest.getFirst())==null ? "???" : event.getJDA().getGuildById(loudest.getFirst()).getName();
        Tuple<String,Integer> dependent = statistics.mostCommandsGuild();
        String dependname = dependent.getFirst().equals("0") ? "Direct Messages" : (event.getJDA().getGuildById(dependent.getFirst())==null ? "???" : event.getJDA().getGuildById(dependent.getFirst()).getName());
        builder.append("\n\uD83D\uDDA5 **Server Statistics**:\n")
                .append(SpConst.LINESTART).append("Largest: **").append(name).append("** (").append(size).append(" users)\n")
                .append(SpConst.LINESTART).append("Loudest: **").append(loudname).append("** (").append(loudest.getSecond()).append("% of messages)\n")
                .append(SpConst.LINESTART).append("Most Used: **").append(dependname).append("** (").append(dependent.getSecond()).append("% of commands)\n")
                ;
        Sender.sendResponse(builder.toString(), event);
        return true;
    }
}
