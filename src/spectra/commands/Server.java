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
import java.util.ArrayList;
import java.util.HashMap;
import javafx.util.Pair;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Guild.VerificationLevel;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.MiscUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Settings;
import spectra.misc.SafeEmote;
import spectra.utils.FormatUtil;
import spectra.utils.OtherUtil;

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
            new ServerMods(),
            new ServerPlaying(),
            new ServerPlot(),
            new ServerSettings()
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        long onlineCount = guild.getUsers().stream().filter((u) -> 
                (u.getOnlineStatus()==OnlineStatus.ONLINE || u.getOnlineStatus()==OnlineStatus.AWAY))
                .count();
        long botCount = guild.getUsers().stream().filter(User::isBot).count();
        String str = "\uD83D\uDDA5 Information about **"+guild.getName()+"**:\n"
                +SpConst.LINESTART+"ID: **"+guild.getId()+"**\n"
                +SpConst.LINESTART+"Owner: **"+guild.getOwner().getUsername()+"** #"+guild.getOwner().getDiscriminator()+"\n"
            
                +SpConst.LINESTART+"Location: **"+guild.getRegion().getName()+"**\n"
                +SpConst.LINESTART+"Creation: **"+MiscUtil.getCreationTime(guild.getId()).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n"
            
                +SpConst.LINESTART+"Users: **"+guild.getUsers().size()+"** ("+onlineCount+" online, "+botCount+" bots)\n"
                +SpConst.LINESTART+"Channels: **"+guild.getTextChannels().size()+"** Text, **"+guild.getVoiceChannels().size()+"** Voice\n"
                +SpConst.LINESTART+"Verification: **"+(guild.getVerificationLevel().equals(VerificationLevel.HIGH)?"(╯°□°）╯︵ ┻━┻":guild.getVerificationLevel())+"**";
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
            this.longhelp = "This command shows the settings for the current server, including welcome/leave messages, prefixes, and tag commands.";
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] current = settings.getSettingsForGuild(event.getGuild().getId());
            Role autorole = (current[Settings.AUTOROLE]!=null ? event.getGuild().getRoleById(current[Settings.AUTOROLE].split("\\|")[0]) : null);
            StringBuilder builder = new StringBuilder("\u2699 Settings on **"+event.getGuild().getName()+"**:\n"
                    + SpConst.LINESTART+"**Welcome**: "+current[Settings.WELCOMEMSG]+"\n"
                    + SpConst.LINESTART+"**Leave**: "+current[Settings.LEAVEMSG]+"\n"
                    + (autorole==null ? "" : SpConst.LINESTART+"**Auto Role**: "+autorole.getName()+"\n")
                    + SpConst.LINESTART+"**Prefixes**:");
            for(String prfx: Settings.prefixesFromList(current[Settings.PREFIXES]))
                builder.append(" `").append(prfx).append("`");
            builder.append("\n").append(SpConst.LINESTART).append("**Tag Commands**: ").append(current[Settings.TAGIMPORTS]);
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private class ServerPlaying extends Command
    {
        private ServerPlaying()
        {
            this.command = "playing";
            this.aliases = new String[]{"games"};
            this.help = "shows the games users are playing most";
            this.longhelp = "This command shows the most-played games on the server (currently), and can show who is playing a specific game.";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("game name",Argument.Type.LONGSTRING, false)
            };
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String game = args[0]==null ? null : (String)args[0];
            if(game==null)
            {
                HashMap<String,Integer> games = new HashMap<>();
                event.getGuild().getUsers().stream().filter(u -> u.getCurrentGame()!=null)
                        .forEach(u -> games.put(u.getCurrentGame().getName(), games.getOrDefault(u.getCurrentGame().getName(), 0)+1));
                ArrayList<String> list = new ArrayList<>(games.keySet());
                list.sort((game1, game2) -> games.get(game2) - games.get(game1));
                StringBuilder builder = new StringBuilder();
                for(int i=0; i<list.size() && i<10; i++)
                    builder.append("\n`[").append(games.get(list.get(i))).append("]` **").append(list.get(i)).append("**");
                if(builder.length()==0)
                {
                    Sender.sendResponse(SpConst.WARNING+"No one is playing anything!", event);
                    return false;
                }
                else
                {
                    Sender.sendResponse(SpConst.SUCCESS+"Most popular games on **"+event.getGuild().getName()+"**:"+builder.toString(),event);
                    return true;
                }
            }
            else
            {
                StringBuilder builder = new StringBuilder();
                event.getGuild().getUsers().stream().filter(u -> u.getCurrentGame()!=null && game.equals(u.getCurrentGame().getName()))
                        .forEach( u -> builder.append("\n").append(FormatUtil.fullUser(u)));
                if(builder.length()==0)
                {
                    Sender.sendResponse(SpConst.WARNING+"No users found playing **"+game+"**", event);
                    return false;
                }
                else
                {
                    Sender.sendResponse(SpConst.SUCCESS+"Users playing **"+game+"**:"+builder.toString(), event);
                    return true;
                }
            }
        }
        
    }
    
    private class ServerPlot extends Command
    {
        private ServerPlot()
        {
            this.command = "plot";
            this.aliases = new String[]{"plotjoins"};
            this.help = "plots the joins of the server";
            this.longhelp = "This command shows a plot of the joins of the users currently on the server. "
                    + "This is not accurate for a full history (since it only looks at current members), but is useful for showing trends.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{Permission.MESSAGE_ATTACH_FILES};
            this.cooldown = 90;
            this.cooldownKey = event -> event.getAuthor().getId()+"|serverplot";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Sender.sendFileResponse(() -> 
                    new Pair<>(SpConst.SUCCESS+"Plot of joins to **"+event.getGuild().getName()+"**:",OtherUtil.drawPlot(event.getGuild(), event.getMessage().getTime())),
                    event);
            return true;
        }
    }
    
    private class ServerMods extends Command
    {
        private ServerMods()
        {
            this.command = "mods";
            this.aliases = new String[]{"admins"};
            this.help = "shows mods/admins and status";
            this.longhelp = "This command shows which mods and admins are available, sorted by status.";
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            ArrayList<Pair<User,String>> list = new ArrayList<>();
            event.getGuild().getUsers().stream().filter(u -> !u.isBot()).forEach(u->{
                if(PermissionUtil.checkPermission(event.getGuild(), u, Permission.MANAGE_SERVER))
                {
                    list.add(new Pair<>(u,"ADMIN"));
                }
                else
                {
                    boolean isMod = false;
                    if(currentSettings[Settings.MODIDS].contains(u.getId()))
                        isMod = true;
                    else if(event.getGuild().isMember(u))
                    {
                        for(Role r : event.getGuild().getRolesForUser(u))
                            if(currentSettings[Settings.MODIDS].contains("r"+r.getId()))
                                isMod = true;
                    }
                    if(isMod)
                    {
                        list.add(new Pair<>(u,"MOD"));
                    }
                }
            });
            
            list.sort((pair1, pair2) -> {
                if(pair1.getKey().getOnlineStatus() == pair2.getKey().getOnlineStatus())
                    return pair1.getValue().compareTo(pair2.getValue());
                return pair1.getKey().getOnlineStatus().compareTo(pair2.getKey().getOnlineStatus());
                });
            
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Mods/Admins on **"+event.getGuild().getName()+"**:\n");
            list.stream().forEach(pair -> builder.append("\n").append(SafeEmote.map(pair.getKey().getOnlineStatus()).get(event.getJDA()))
                    .append(" ").append(FormatUtil.shortUser(pair.getKey())).append(" `[").append(pair.getValue()).append("]`"));
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
        
    }
    
}
