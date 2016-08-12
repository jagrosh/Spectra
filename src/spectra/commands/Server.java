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
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Guild.VerificationLevel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.MiscUtil;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Server extends Command {
    private final Settings settings;
    public Server(Settings settings)
    {
        this.settings = settings;
        this.command = "server";
        this.aliases = new String[]{"serverinfo","srvr","guildinfo"};
        this.help = "gets information about the current server";
        this.longhelp = "This command provides basic information about the current server";
        this.availableInDM = false;
        this.children = new Command[]{
            new ServerSettings()
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        int onlineCount = 0;
        onlineCount = guild.getUsers().stream().filter((u) -> 
                (u.getOnlineStatus()==OnlineStatus.ONLINE || u.getOnlineStatus()==OnlineStatus.AWAY))
                .map((_item) -> 1).reduce(onlineCount, Integer::sum);
        String str = "\uD83D\uDDA5 Information about **"+guild.getName()+"**:\n"
                +SpConst.LINESTART+"ID: **"+guild.getId()+"**\n"
                +SpConst.LINESTART+"Owner: **"+guild.getOwner().getUsername()+"** #"+guild.getOwner().getDiscriminator()+"\n"
            
                +SpConst.LINESTART+"Location: **"+guild.getRegion().getName()+"**\n"
                +SpConst.LINESTART+"Creation: **"+MiscUtil.getCreationTime(guild.getId()).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n"
            
                +SpConst.LINESTART+"Users: **"+guild.getUsers().size()+"** ("+onlineCount+" online)\n"
                +SpConst.LINESTART+"Channels: **"+guild.getTextChannels().size()+"** Text, **"+guild.getVoiceChannels().size()+"** Voice\n"
                +SpConst.LINESTART+"Verification: **"+(guild.getVerificationLevel().equals(VerificationLevel.HIGH)?"(╯°□°）╯︵ ┻━┻":guild.getVerificationLevel())+"**";
        if(!event.getGuild().getEmotes().isEmpty())
        {
            StringBuilder builder = new StringBuilder();
            event.getGuild().getEmotes().forEach( e -> builder.append(" ").append(e.getAsEmote()));
            str+="\n"+SpConst.LINESTART+"Emotes:"+builder.toString();
        }
        if(guild.getIconUrl()!=null)
            str+="\n"+SpConst.LINESTART+"Server Icon: "+guild.getIconUrl();
        
        Sender.sendResponse(str, event);
        return true;
    }
    
    private class ServerSettings extends Command
    {
        private ServerSettings()
        {
            this.command = "settings";
            this.help = "shows the settings on the server";
            this.longhelp = "";
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] current = settings.getSettingsForGuild(event.getGuild().getId());
            StringBuilder builder = new StringBuilder("\u2699 Settings on **"+event.getGuild().getName()+"**:\n"
                    + SpConst.LINESTART+"**Welcome**: "+current[Settings.WELCOMEMSG]+"\n"
                    + SpConst.LINESTART+"**Leave**: "+current[Settings.LEAVEMSG]+"\n"
                    + SpConst.LINESTART+"**Prefixes**:");
            for(String prfx: Settings.prefixesFromList(current[Settings.PREFIXES]))
                builder.append(" `").append(prfx).append("`");
            builder.append("\n").append(SpConst.LINESTART).append("**Tag Commands**: ").append(current[Settings.TAGIMPORTS]);
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
}
