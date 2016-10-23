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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.InviteUtil;
import net.dv8tion.jda.utils.MiscUtil;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.Spectra;
import spectra.datasources.Feeds;
import spectra.tempdata.Statistics;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SystemCmd extends Command {
    final Spectra spectra;
    final Feeds feeds;
    final Statistics statistics;
    public SystemCmd(Spectra spectra, Feeds feeds, Statistics statistics)
    {
        this.spectra = spectra;
        this.feeds = feeds;
        this.statistics = statistics;
        this.command = "system";
        this.help = "commands for controlling the bot";
        this.longhelp = "These commands are for controlling the inner-workings of the bot";
        this.level = PermLevel.JAGROSH;
        this.children = new Command[]{
            new SystemDebug(),
            new SystemFind(),
            new SystemGuildinfo(),
            new SystemIdle(),
            new SystemReady(),
            new SystemInvite(),
            new SystemSafe(),
            new SystemShutdown()
        };
    }
    
    private class SystemFind extends Command
    {
        private SystemFind()
        {
            this.command = "find";
            this.level = PermLevel.JAGROSH;
            this.help = "finds user info on an ID";
            this.longhelp = "This command finds a user by id and displays information on them (shortcut from eval).";
            this.arguments = new Argument[]{
                new Argument("id",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            User u = event.getJDA().getUserById((String)args[0]);
            if(u==null)
            {
                Sender.sendResponse(SpConst.ERROR+"User with ID `"+args[0]+"` not found!", event);
                return false;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Found **"+u.getUsername()+"** #"+u.getDiscriminator()+":");
            event.getJDA().getGuilds().stream().filter(g -> g.isMember(u))
                    .forEach(g -> builder.append("\n").append(g.getId()).append(" **").append(g.getName()).append("**").append(g.getOwner().equals(u) ? " [Own]" : ""));
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private class SystemGuildinfo extends Command
    {
        private SystemGuildinfo()
        {
            this.command = "guildinfo";
            this.aliases = new String[]{"ginfo"};
            this.level = PermLevel.JAGROSH;
            this.help = "finds info on an ID";
            this.longhelp = "This command finds a guild by id and displays information on them (shortcut from eval).";
            this.arguments = new Argument[]{
                new Argument("id",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Guild g = event.getJDA().getGuildById((String)args[0]);
            if(g==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Guild with ID `"+args[0]+"` not found!", event);
                return false;
            }
            long botcount = g.getUsers().stream().filter(User::isBot).count();
            long usercount = g.getUsers().stream().filter(u -> {
                    return !u.isBot() && u.getAvatarId()!=null && MiscUtil.getCreationTime(u.getId()).plusDays(7).isBefore(OffsetDateTime.now());
                }).count();
            int requirements = spectra.meetsRequirements(g);
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Found **"+g.getName()+"**:");
            builder.append("\nOwner: ").append(FormatUtil.fullUser(g.getOwner()))
                   .append("\nCreated: ").append(MiscUtil.getCreationTime(g.getId()).format(DateTimeFormatter.RFC_1123_DATE_TIME))
                   .append("\nTotal users: ").append(g.getUsers().size())
                   .append("\nReal users: ").append(usercount)
                   .append("\nBots: ").append(botcount)
                   .append("\nRequirements? ").append(requirements > 0 ? "No" : "Yes").append(" (").append(requirements).append(")");
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private class SystemInvite extends Command
    {
        private SystemInvite()
        {
            this.command = "serverinvite";
            this.help = "gets an invite for a server";
            this.longhelp = "";
            this.level = PermLevel.JAGROSH;
            this.arguments = new Argument[]{
                new Argument("id",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String id = (String)args[0];
            Guild guild = event.getJDA().getGuildById(id);
            if(guild==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Guild not found", event);
                return false;
            }
            String code = null;
            try{
                code = guild.getInvites().get(0).getCode();
            } catch (Exception e){}
            if(code==null)
            {
                for(TextChannel tc : guild.getTextChannels())
                    try{
                        code = InviteUtil.createInvite(tc).getCode();
                        break;
                    }catch(Exception e){}
            }
            if(code==null)
            {
                for(VoiceChannel tc : guild.getVoiceChannels())
                    try{
                        code = InviteUtil.createInvite(tc).getCode();
                        break;
                    }catch(Exception e){}
            }
            if(code==null)
            {
                Sender.sendResponse(SpConst.WARNING+"Invites could not be found nor created.", event);
                return false;
            }
            else
            {
                Sender.sendResponse("http://discord.gg/"+code, event);
                return true;
            }
        }
    }
    
    private class SystemIdle extends Command
    {
        private SystemIdle()
        {
            this.command = "idle";
            this.help = "prevents the bot from receiving commands";
            this.longhelp = "This command prevents the bot from using any commands except those by the bot owner.";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(spectra.isIdling())
            {
                Sender.sendResponse(SpConst.ERROR+"**"+SpConst.BOTNAME+"** is already `IDLING`", event);
                return false;
            }
            spectra.setIdling(true);
            event.getJDA().getAccountManager().setStatus(OnlineStatus.DO_NOT_DISTURB);
            Sender.sendResponse("\uD83D\uDED1 **"+SpConst.BOTNAME+"** is now `IDLING`", event);
            return true;
        }
    }
    
    private class SystemReady extends Command
    {
        private SystemReady()
        {
            this.command = "ready";
            this.help = "allows the bot to recieve commands";
            this.longhelp = "This disables idle mode";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(!spectra.isIdling())
            {
                Sender.sendResponse(SpConst.ERROR+"**"+SpConst.BOTNAME+"** is already `READY`", event);
                return false;
            }
            spectra.setIdling(false);
            event.getJDA().getAccountManager().setStatus(OnlineStatus.ONLINE);
            Sender.sendResponse(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** is now `READY`", event);
            return true;
        }
    }
    
    private class SystemDebug extends Command
    {
        private SystemDebug()
        {
            this.command = "debug";
            this.help = "sets debug mode";
            this.longhelp = "This command toggles debug mode in console";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(spectra.isDebug())
            {
                spectra.setDebug(false);
                Sender.sendResponse(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** is no longer in Debug Mode", event);
                return true;
            }
            else
            {
                spectra.setDebug(true);
                Sender.sendResponse("\uD83D\uDCDF **"+SpConst.BOTNAME+"** is now in Debug Mode", event);
                return true;
            }
        }
    }
    
    private class SystemSafe extends Command
    {
        private SystemSafe()
        {
            this.command = "safe";
            this.help = "sets safe mode";
            this.longhelp = "This command toggles safe mode";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(spectra.isSafety())
            {
                spectra.setSafeMode(false);
                Sender.sendResponse(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** is no longer in Safe Mode", event);
                return true;
            }
            else
            {
                spectra.setSafeMode(true);
                Sender.sendResponse("\uD83D\uDCDF **"+SpConst.BOTNAME+"** is now in Safe Mode", event);
                return true;
            }
        }
    }
    
    private class SystemShutdown extends Command
    {
        private SystemShutdown()
        {
            this.command = "shutdown";
            this.help = "shuts down the bot safely";
            this.longhelp = "This command shuts the bot down safely, closing any threadpools. Must be idling first.";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(!spectra.isIdling())
            {
                Sender.sendResponse(SpConst.ERROR+"Cannot shutdown if not idling!", event);
                return false;
            }
            try{
            event.getJDA().getTextChannelById(feeds.feedForGuild(event.getJDA().getGuildById(SpConst.JAGZONE_ID), Feeds.Type.BOTLOG)[Feeds.CHANNELID])
                    .sendMessage(FeedHandler.botlogFormat(SpConst.ERROR+"**"+SpConst.BOTNAME+"** is going <@&182294168083628032>"+"\nRuntime: "+FormatUtil.secondsToTime(statistics.getUptime())));
            event.getChannel().sendMessage("\uD83D\uDCDF Shutting down...");}catch(Exception e){System.err.println(e);}
            spectra.shutdown();
            return true;
        }
    }
    }
