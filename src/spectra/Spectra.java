/*
 * Copyright 2016 jagrosh.
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
package spectra;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDA.Status;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.DisconnectEvent;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.ResumedEvent;
import net.dv8tion.jda.events.ShutdownEvent;
import net.dv8tion.jda.events.StatusChangeEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberBanEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberUnbanEvent;
import net.dv8tion.jda.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.events.user.UserAvatarUpdateEvent;
import net.dv8tion.jda.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.events.user.UserTypingEvent;
import net.dv8tion.jda.events.voice.VoiceLeaveEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.AvatarUtil;
import net.dv8tion.jda.utils.MiscUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import net.dv8tion.jda.utils.SimpleLog;
import spectra.commands.*;
import spectra.datasources.*;
import spectra.entities.Tuple;
import spectra.tempdata.CallDepend;
import spectra.tempdata.MessageCache;
import spectra.tempdata.PhoneConnections;
import spectra.utils.OtherUtil;
import spectra.utils.FormatUtil;
import spectra.web.BingImageSearcher;
import spectra.web.GoogleSearcher;
import spectra.web.YoutubeSearcher;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Spectra extends ListenerAdapter {
    
    private JDA jda;
    private Command[] commands;
    private boolean idling = false;
    private static final OffsetDateTime start = OffsetDateTime.now();
    private OffsetDateTime lastDisconnect;
    private Color currentColor;
    private int colorCounter;
    
    //datasources
    private final AFKs afks;
    private final Contests contests;
    private final Donators donators;
    private final Entries entries;
    private final Feeds feeds;
    private final Guides guides;
    private final Mutes mutes;
    private final Overrides overrides;
    private final Profiles profiles;
    private final Reminders reminders;
    private final Rooms rooms;
    private final SavedNames savednames;
    private final Settings settings;
    private final Tags tags;
    
    //handlers
    private final FeedHandler handler;
    
    //tempdata
    private final MessageCache messagecache;
    private final PhoneConnections phones;
    
    //searchers
    private final BingImageSearcher imagesearcher;
    private final GoogleSearcher googlesearcher;
    private final YoutubeSearcher youtubesearcher;
    
    //timers
    private final ScheduledExecutorService feedflusher;
    private final ScheduledExecutorService unmuter;
    private final ScheduledExecutorService roomchecker;
    private final ScheduledExecutorService reminderchecker;
    private final ScheduledExecutorService cachecleaner;
    private final ScheduledExecutorService avatarchanger;
    private final ScheduledExecutorService contestchecker;
    
    //eventhandler
    private final AsyncInterfacedEventManager eventmanager;
    
    public static void main(String[] args)
    {
        new Spectra().init();
    }
    
    public Spectra()
    {
        afks        = new AFKs();
        contests    = new Contests();
        donators    = new Donators();
        entries     = new Entries();
        feeds       = new Feeds();
        guides      = new Guides();
        mutes       = new Mutes();
        overrides   = new Overrides();
        profiles    = new Profiles();
        reminders   = new Reminders();
        rooms       = new Rooms();
        savednames  = new SavedNames();
        settings    = new Settings();
        tags        = new Tags();
        
        handler = new FeedHandler(feeds);
        messagecache = new MessageCache();
        phones = new PhoneConnections();
        
        imagesearcher = new BingImageSearcher(OtherUtil.readFileLines("bing.apikey"));
        googlesearcher = new GoogleSearcher();
        youtubesearcher = new YoutubeSearcher(OtherUtil.readFileLines("youtube.apikey").get(0));
        
        feedflusher = Executors.newSingleThreadScheduledExecutor();
        unmuter  = Executors.newSingleThreadScheduledExecutor();
        roomchecker = Executors.newSingleThreadScheduledExecutor();
        reminderchecker = Executors.newSingleThreadScheduledExecutor();
        cachecleaner  = Executors.newSingleThreadScheduledExecutor();
        avatarchanger = Executors.newSingleThreadScheduledExecutor();
        contestchecker = Executors.newSingleThreadScheduledExecutor();
        
        eventmanager = new AsyncInterfacedEventManager();
    }
    
    private void init()
    {
        afks.read();
        contests.read();
        donators.read();
        entries.read();
        feeds.read();
        guides.read();
        mutes.read();
        overrides.read();
        profiles.read();
        reminders.read();
        rooms.read();
        savednames.read();
        settings.read();
        tags.read();
        
        commands = new Command[]{
            new About(),
            new AFK(afks),
            new Archive(),
            new Avatar(),
            new ChannelCmd(),
            new Contest(contests, entries),
            new Draw(),
            new GoogleSearch(googlesearcher),
            new ImageSearch(imagesearcher),
            new Info(),
            new Invite(),
            new Names(savednames),
            new Ping(),
            new Profile(profiles),
            new Reminder(reminders),
            new Room(rooms, settings, handler),
            new Server(),
            new Speakerphone(phones),
            new Tag(tags, overrides, settings, handler),
            new Timefor(profiles),
            new WelcomeGuide(guides),
            new YoutubeSearch(youtubesearcher),
            
            new Ban(handler, settings),
            new BotScan(),
            new Clean(handler),
            new Kick(handler, settings),
            new Mute(handler, settings, mutes),
            new Softban(handler, settings, mutes),
            
            new CommandCmd(settings, this),
            new Feed(feeds),
            new Ignore(settings),
            new Leave(settings),
            new ModCmd(settings),
            new Prefix(settings),
            new RoleCmd(),
            new Say(),
            new Welcome(settings),
            new WelcomeDM(guides),
                
            new Announce(handler,feeds),
            new Eval(this),
            new SystemCmd(this,feeds),
        };
        
        try {
            jda = new JDABuilder()
                    .addListener(this)
                    .setBotToken(OtherUtil.readFileLines("discordbot.login").get(1))
                    .setBulkDeleteSplittingEnabled(false)
                    .setEventManager(eventmanager)
                    .buildAsync();
        } catch (LoginException | IllegalArgumentException ex) {
            System.err.println("ERROR - Building JDA : "+ex.toString());
            System.exit(1);
        }
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().getAccountManager().setGame("Type "+SpConst.PREFIX+"help");
        
        handler.submitText(Feeds.Type.BOTLOG, event.getJDA().getGuildById(SpConst.JAGZONE_ID),
                SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** is now <@&182294060382289921>\n"
                        + "Connected to **"+event.getJDA().getGuilds().size()+"** servers.\n"
                        + "Started up in "+FormatUtil.secondsToTime(start.until(OffsetDateTime.now(), ChronoUnit.SECONDS)));
        feedflusher.scheduleWithFixedDelay(()->{handler.flush(jda);}, 0, 20, TimeUnit.SECONDS);
        unmuter.scheduleWithFixedDelay(()-> {mutes.checkUnmutes(jda, handler);},0, 10, TimeUnit.SECONDS);
        roomchecker.scheduleWithFixedDelay(() -> {rooms.checkExpires(jda, handler);}, 0, 120, TimeUnit.SECONDS);
        reminderchecker.scheduleWithFixedDelay(() -> {reminders.checkReminders(jda);}, 0, 30, TimeUnit.SECONDS);
        contestchecker.scheduleWithFixedDelay(() -> {contests.notifications(jda);}, 0, 1, TimeUnit.MINUTES);
        cachecleaner.scheduleWithFixedDelay(() -> {
            imagesearcher.clearCache();
            googlesearcher.clearCache();}, 6, 3, TimeUnit.HOURS);
        avatarchanger.scheduleWithFixedDelay(()->{
            if(jda.getStatus()!=Status.CONNECTED)
                return;
            if(colorCounter<=0)
            {
                colorCounter=10;
                currentColor = Color.getHSBColor((float)Math.random(), 1.0f, .5f);
                ArrayList<Role> modifiable = new ArrayList<>();
                for(Guild g: jda.getGuilds())
                {
                    if(!g.isAvailable())
                        continue;
                    if(!PermissionUtil.checkPermission(jda.getSelfInfo(), Permission.MANAGE_ROLES, g))
                        continue;
                    List<Role> guildroles = g.getRolesForUser(jda.getSelfInfo());
                    for(int i=1; i<guildroles.size(); i++)
                        if(guildroles.get(i).getName().equalsIgnoreCase(jda.getSelfInfo().getUsername()))
                        {
                            modifiable.add(guildroles.get(i));
                            break;
                        }
                }
                jda.getAccountManager().setAvatar(AvatarUtil.getAvatar(OtherUtil.makeWave(currentColor))).update();
                modifiable.stream().forEach((r) -> {
                    r.getManager().setPermissionsRaw(r.getPermissionsRaw()).setColor(currentColor.brighter()).update();
                });
            }
            else
            {
                jda.getAccountManager().setAvatar(AvatarUtil.getAvatar(OtherUtil.makeWave(currentColor))).update();
            }
            colorCounter--;
        }, 3, 3, TimeUnit.MINUTES);
    }
    
    public void shutdown()
    {
        jda.shutdown();
    }
    
    @Override
    public void onShutdown(ShutdownEvent event) {
        eventmanager.shutdown();
        
        afks.shutdown();
        contests.shutdown();
        donators.shutdown();
        entries.shutdown();
        feeds.shutdown();
        guides.shutdown();
        mutes.shutdown();
        overrides.shutdown();
        profiles.shutdown();
        reminders.shutdown();
        rooms.shutdown();
        savednames.shutdown();
        settings.shutdown();
        tags.shutdown();
        
        feedflusher.shutdown();
        unmuter.shutdown();
        roomchecker.shutdown();
        reminderchecker.shutdown();
        cachecleaner.shutdown();
        avatarchanger.shutdown();
        contestchecker.shutdown();
    }
    
    @Override
    public void onDisconnect(DisconnectEvent event) {
        lastDisconnect = OffsetDateTime.now();
    }
    
    @Override
    public void onReconnect(ReconnectedEvent event) {
        handler.submitText(Feeds.Type.BOTLOG, event.getJDA().getGuildById(SpConst.JAGZONE_ID),
                SpConst.WARNING+"**"+SpConst.BOTNAME+"** has <@&196723905354924032>\n"
                        + (lastDisconnect==null ? "No disconnect time recorded." : 
                        "Downtime: "+FormatUtil.secondsToTime(lastDisconnect.until(OffsetDateTime.now(), ChronoUnit.SECONDS))) );
        lastDisconnect = null;
    }

    @Override
    public void onResume(ResumedEvent event) {
        handler.submitText(Feeds.Type.BOTLOG, event.getJDA().getGuildById(SpConst.JAGZONE_ID),
                SpConst.WARNING+"**"+SpConst.BOTNAME+"** has <@&196723649061847040>\n"
                        + (lastDisconnect==null ? "No disconnect time recorded." : 
                        "Downtime: "+FormatUtil.secondsToTime(lastDisconnect.until(OffsetDateTime.now(), ChronoUnit.SECONDS))) );
        lastDisconnect = null;
    }

    @Override
    public void onStatusChange(StatusChangeEvent event) {
        SimpleLog.getLog("Status").info("Status changed from ["+event.getOldStatus()+"] to ["+event.getStatus()+"]");
    }

    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        
        PermLevel perm;
        boolean ignore;
        boolean isCommand = false;
        boolean successful;
        
        if(afks.get(event.getAuthor().getId())!=null)
        {
            Sender.sendPrivate(SpConst.SUCCESS+"Welcome back, I have removed your AFK status.", event.getAuthor().getPrivateChannel());
            afks.remove(event.getAuthor().getId());
        }
        if(!event.isPrivate())
            event.getMessage().getMentionedUsers().stream().filter((u) -> (afks.get(u.getId())!=null)).forEach((u) -> {
                String relate = "__"+event.getGuild().getName()+"__ <#"+event.getTextChannel().getId()+"> <@"+event.getAuthor().getId()+">:\n"+event.getMessage().getRawContent();
                if(relate.length()>2000)
                    relate = relate.substring(0,2000);
                Sender.sendPrivate(relate, u.getPrivateChannel());
        });
        
        if(idling && !event.getAuthor().getId().equals(SpConst.JAGROSH_ID))
            return;
        //get the settings for the server
        //settings will be null for private messages
        //make default settings if no settings exist for a server
        String[] currentSettings = (event.isPrivate() ? null : settings.getSettingsForGuild(event.getGuild().getId()));
        if(currentSettings==null && !event.isPrivate())
            currentSettings = settings.makeNewSettingsForGuild(event.getGuild().getId());
        
        //get a sorted list of prefixes
        String[] prefixes = event.isPrivate() ?
            new String[]{SpConst.PREFIX,SpConst.ALTPREFIX} :
            Settings.prefixesFromList(currentSettings[Settings.PREFIXES]);
        
        //compare against each prefix
        String strippedMessage=null;
        for(int i=prefixes.length-1;i>=0;i--)
        {
            if(event.getMessage().getRawContent().toLowerCase().startsWith(prefixes[i].toLowerCase()))
            {
                strippedMessage = event.getMessage().getRawContent().substring(prefixes[i].length()).trim();
                break; 
            }
        }
        //find permission level
        perm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
        
        //check if should ignore
        ignore = false;
        if(!event.isPrivate())
        {
            if( currentSettings[Settings.IGNORELIST].contains("u"+event.getAuthor().getId()) || currentSettings[Settings.IGNORELIST].contains("c"+event.getTextChannel().getId()) )
                ignore = true;
            else if(currentSettings[Settings.IGNORELIST].contains("r"+event.getGuild().getId()) && event.getGuild().getRolesForUser(event.getAuthor()).isEmpty())
                ignore = true;
            else
                for(Role r: event.getGuild().getRolesForUser(event.getAuthor()))
                    if(currentSettings[Settings.IGNORELIST].contains("r"+r.getId()))
                    {
                        ignore = true;
                        break;
                    }
        }
        
        if(!ignore && !event.isPrivate() && !event.getMessage().getMentionedUsers().isEmpty() && !event.getAuthor().isBot())
        {
            StringBuilder builder = new StringBuilder("");
            event.getMessage().getMentionedUsers().stream().forEach(u -> {
                if(afks.get(u.getId())!=null)
                {
                    String response = afks.get(u.getId())[AFKs.MESSAGE];
                    if(response!=null)
                        builder.append("\n\uD83D\uDCA4 **").append(u.getUsername()).append("** is currently AFK:\n").append(response);
                }});
            String afkmessage = builder.toString().trim();
            if(!afkmessage.equals(""))
            Sender.sendMsg(afkmessage, event.getTextChannel());
        }
        
        if(strippedMessage!=null && !event.getAuthor().isBot())//potential command right here
        {
            strippedMessage = strippedMessage.trim();
            if(strippedMessage.equalsIgnoreCase("help"))//send full help message (based on access level)
            {//we don't worry about ignores for help
                isCommand = true;
                successful = true;
                String helpmsg = "**Available help ("+(event.isPrivate() ? "Direct Message" : "<#"+event.getTextChannel().getId()+">")+")**:";
                PermLevel current = PermLevel.EVERYONE;
                for(Command com: commands)
                {
                    if(com.hidden)
                        continue;
                    if( perm.isAtLeast(com.level) )
                    {
                        if(com.level==PermLevel.MODERATOR&& !current.isAtLeast(PermLevel.MODERATOR))
                        {
                            current = PermLevel.MODERATOR;
                            helpmsg+= "\n**Moderator Commands:**";
                        }
                        else if(com.level==PermLevel.ADMIN && !current.isAtLeast(PermLevel.ADMIN))
                        {
                            current = PermLevel.ADMIN;
                            helpmsg+= "\n**Admin Commands:**";
                        }
                        else if(com.level==PermLevel.JAGROSH && !current.isAtLeast(PermLevel.JAGROSH))
                        {
                            current = PermLevel.JAGROSH;
                            helpmsg+= "\n**jagrosh Commands:**";
                        }
                        helpmsg += "\n`"+SpConst.PREFIX+com.command+"`"+Argument.arrayToString(com.arguments)+" - "+com.help;
                    }
                }
                helpmsg+="\n\nFor more information, call "+SpConst.PREFIX+"<command> help. For example, `"+SpConst.PREFIX+"tag help`";
                helpmsg+="\nFor commands, `<argument>` refers to a required argument, while `[argument]` is optional";
                helpmsg+="\nDo not add <> or [] to your arguments, nor quotation marks";
                helpmsg+="\nFor more help, contact **@jagrosh** "+(event.getAuthor().getId().equals(SpConst.JAGROSH_ID) ? "" : "(<@"+SpConst.JAGROSH_ID+">) ")+"or join "+SpConst.JAGZONE_INVITE;
                Sender.sendHelp(helpmsg, event.getAuthor().getPrivateChannel(), event);
            }
            else//wasn't base help command
            {
                Command toRun = null;
                String[] args = FormatUtil.cleanSplit(strippedMessage);
                if(args[0].equalsIgnoreCase("help"))
                {
                    String endhelp = args[1]+" "+args[0];
                    args = FormatUtil.cleanSplit(endhelp);
                }
                if(event.getMessage().getAttachments()!=null && !event.getMessage().getAttachments().isEmpty())
                    args[1]+=" "+event.getMessage().getAttachments().get(0).getUrl();
                for(Command com: commands)
                    if(com.isCommandFor(args[0]))
                    {
                        toRun = com;
                        break;
                    }
                if(toRun!=null)
                {
                    isCommand = true;
                    //check if banned
                    boolean banned = false;
                    if(!event.isPrivate())
                    {
                        for(String bannedCmd : Settings.restrCmdsFromList(currentSettings[Settings.BANNEDCMDS]))
                            if(bannedCmd.equalsIgnoreCase(toRun.command))
                                banned = true;
                        if(banned)
                            if(event.getTextChannel().getTopic().contains("{"+toRun.command+"}"))
                                banned = false;
                    }
                    successful = toRun.run(args[1], event, perm, ignore, banned);
                }
                else if (!event.isPrivate() && (!ignore || perm.isAtLeast(PermLevel.ADMIN)))
                {
                    String[] tagCommands = Settings.tagCommandsFromList(currentSettings[Settings.TAGIMPORTS]);
                    for(String cmd : tagCommands)
                        if(cmd.equalsIgnoreCase(args[0]))
                        {
                            isCommand=true;
                            boolean nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
                            String[] tag = overrides.findTag(event.getGuild(), cmd, nsfw);
                            if(tag==null)
                                tag = tags.findTag(cmd, null, false, nsfw);
                            if(tag==null)
                            {
                                Sender.sendResponse(SpConst.ERROR+"Tag \""+cmd+"\" no longer exists!", event);
                            }
                            else
                            {
                                Sender.sendResponse("\u200B"+JagTag.convertText(tag[Tags.CONTENTS], args[1], event.getAuthor(), event.getGuild(), event.getChannel()), event);
                            }
                        }
                }
            }
        }
        if(!event.isPrivate() && !isCommand && !ignore && !event.getAuthor().isBot())
        {
            TextChannel chan = phones.getOtherLine(event.getTextChannel());
            if(chan!=null)
            {
                String toSend = (PhoneConnections.LINE+FormatUtil.appendAttachmentUrls(event.getMessage()))
                        .replaceAll("(?i)spectra", "you")
                        .replace(event.getAuthorName(), "Spectra")
                        .replace("@everyone", "@\u200Beveryone")
                        .replace("@here", "@\u200Bhere");
                if(toSend.length()>2000)
                    toSend = toSend.substring(0,2000);
                try{
                    chan.sendMessage(toSend);
                }catch(Exception e)
                {
                    phones.endCall(event.getJDA());
                }
            }
        }
    }

    
    @Override
    public void onUserTyping(UserTypingEvent event) {
        if(afks.get(event.getUser().getId())!=null)
        {
            Sender.sendPrivate(SpConst.SUCCESS+"Welcome back, I have removed your AFK status.", event.getUser().getPrivateChannel());
            afks.remove(event.getUser().getId());
        }
        if(event.getChannel() instanceof TextChannel)
        {
            TextChannel other = phones.getOtherLine(((TextChannel)event.getChannel()));
            if(other!=null && !event.getUser().isBot())
                other.sendTyping();
        }
    }
    
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        rooms.setLastActivity(event.getChannel().getId(), event.getMessage().getTime());
        if(feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG)!=null)
            messagecache.addMessage(event.getGuild().getId(), event.getMessage());
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG);
        if(feed!=null)
        {
            String id = event.getAuthor().getId();
            Message msg = messagecache.updateMessage(event.getGuild().getId(), event.getMessage());
            String details = feed[Feeds.DETAILS];
            if(msg!=null && !msg.getRawContent().equals(event.getMessage().getRawContent()) && (details==null || details.contains("+m"+id) || !(details.contains("-m"+id) || details.contains("+m"))))
            {
                String old = FormatUtil.appendAttachmentUrls(msg);
                String newmsg = FormatUtil.appendAttachmentUrls(event.getMessage());

                handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                        "\u26A0 **"+event.getAuthor().getUsername()+"** edited a message in <#"+event.getChannel().getId()+">\n**From:** "
                        +old+"\n**To:** "+newmsg );
            }
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        CallDepend.getInstance().delete(event.getMessageId());
        String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG);
        if(feed!=null)
        {
            String id = event.getAuthor().getId();
            Message msg = messagecache.deleteMessage(event.getGuild().getId(), event.getMessageId());
            String details = feed[Feeds.DETAILS];
            if( msg!=null && (details==null || details.contains("+m"+id) || !(details.contains("-m"+id) || details.contains("+m"))) )
            {
                String del = FormatUtil.appendAttachmentUrls(msg);
                handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                        "\u274C **"+msg.getAuthor().getUsername()
                        +"**'s message has been deleted from <#"+msg.getChannelId()+">:\n"+del );
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(), 
                "\uD83D\uDCE5 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") joined the server.");
        if(mutes.getMute(event.getUser().getId(), event.getGuild().getId())!=null)
            if(PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MANAGE_ROLES, event.getGuild()))
                event.getGuild().getRoles().stream().filter((role) -> (role.getName().equalsIgnoreCase("muted") 
                        && PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))).forEach((role) -> {
                    event.getGuild().getManager().addRoleToUser(event.getUser(), role);
        });
        String[] currentsettings = settings.getSettingsForGuild(event.getGuild().getId());
        String current = currentsettings==null ? null : currentsettings[Settings.WELCOMEMSG];
        if(current!=null && !current.equals(""))
        {
            String[] parts = Settings.parseWelcomeMessage(current);
            TextChannel channel = parts[0]==null ? event.getGuild().getPublicChannel() : event.getJDA().getTextChannelById(parts[0]);
            if(channel==null || !channel.getGuild().equals(event.getGuild()))
                channel = event.getGuild().getPublicChannel();
            String toSend = JagTag.convertText(parts[1].replace("%user%", event.getUser().getUsername()).replace("%atuser%", event.getUser().getAsMention()), "", event.getUser(),event.getGuild(), channel).trim();
            if(!toSend.equals(""))
                Sender.sendMsg(toSend, channel);
        }
        String[] guide = guides.get(event.getGuild().getId());
        if(guide!=null)
            for(int i=1; i<guide.length; i++)
                if(guide[i]!=null && !guide[i].equals(""))
                    Sender.sendPrivate(guide[i], event.getUser().getPrivateChannel());
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                "\uD83D\uDCE4 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") left or was kicked from the server.");
        String[] currentsettings = settings.getSettingsForGuild(event.getGuild().getId());
        String current = currentsettings==null ? null : currentsettings[Settings.LEAVEMSG];
        if(current!=null && !current.equals(""))
        {
            String[] parts = Settings.parseWelcomeMessage(current);
            TextChannel channel = parts[0]==null ? event.getGuild().getPublicChannel() : event.getJDA().getTextChannelById(parts[0]);
            if(channel==null || !channel.getGuild().equals(event.getGuild()))
                channel = event.getGuild().getPublicChannel();
            String toSend = JagTag.convertText(parts[1].replace("%user%", event.getUser().getUsername()).replace("%atuser%", event.getUser().getAsMention()), "", event.getUser(),event.getGuild(), channel).trim();
            if(!toSend.equals(""))
                Sender.sendMsg(toSend, channel);
        }
    }

    @Override
    public void onGuildMemberBan(GuildMemberBanEvent event) {
        handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                "\uD83D\uDD28 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") was banned from the server.");
    }

    @Override
    public void onGuildMemberUnban(GuildMemberUnbanEvent event) {
        handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                "\uD83D\uDD27 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") was unbanned from the server.");
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        int size = event.getMessageIds().size();
        boolean servlogon = feeds.feedForGuild(event.getChannel().getGuild(), Feeds.Type.SERVERLOG)!=null;
        String guildid = event.getChannel().getGuild().getId();
        String header = "\u274C\u274C "+size+" messages were deleted from <#"+event.getChannel().getId()+">";
        StringBuilder builder = new StringBuilder("-- deleted messages --\n");
        event.getMessageIds().stream().forEach((id) -> {
            CallDepend.getInstance().delete(id);
            if(servlogon)
                {
                    Message m = messagecache.deleteMessage(guildid, id);
                    if(m!=null)
                    {
                        builder.append("[").append(m.getTime()==null ? "UNKNOWN TIME" : m.getTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("] ");
                        builder.append( m.getAuthor() == null ? "????" : m.getAuthor().getUsername() ).append(" : ");
                        builder.append(m.getContent()).append(m.getAttachments()!=null && m.getAttachments().size()>0 ? " "+m.getAttachments().get(0).getUrl() : "").append("\n\n");
                    }
                }
        });
        if(servlogon)
            handler.submitFile(Feeds.Type.SERVERLOG, event.getChannel().getGuild(), ()->{
                File f = OtherUtil.writeArchive(builder.toString(), "deleted_messages");
                return new Tuple<>(header,f);}, header);
    }

    @Override
    public void onGuildMemberNickChange(GuildMemberNickChangeEvent event) {
        String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG);
        if(feed!=null)
        {
            String id = event.getUser().getId();
            String details = feed[Feeds.DETAILS];
            if( details==null || details.contains("+n"+id) || !(details.contains("-n"+id) || details.contains("+n")) )
                handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(), "\u270D **"+event.getUser().getUsername()+"** (ID:"
                        +event.getUser().getId()+") has changed nicknames from "+(event.getPrevNick()==null ? "[none]" : "**"+event.getPrevNick()+"**")+" to "+
                        (event.getNewNick()==null ? "[none]" : "**"+event.getNewNick()+"**"));
        }
    }
    
    @Override
    public void onUserNameUpdate(UserNameUpdateEvent event) {
        savednames.addName(event.getUser().getId(), event.getPreviousUsername());
        ArrayList<Guild> guilds = new ArrayList<>();
        jda.getGuilds().stream().filter((g) -> (g.isMember(event.getUser()))).forEach((g) -> {
            guilds.add(g);
        });
        handler.submitText(Feeds.Type.SERVERLOG, guilds, "\uD83D\uDCDB **"+event.getPreviousUsername()+"** (ID:"
                        +event.getUser().getId()+") has changed names to **"
                        +event.getUser().getUsername()+"**");
    }

    @Override
    public void onUserAvatarUpdate(UserAvatarUpdateEvent event)  {
        if(event.getUser().equals(event.getJDA().getSelfInfo()))
            return;
        String id = event.getUser().getId();
        String oldurl = event.getPreviousAvatarUrl()==null ? event.getUser().getDefaultAvatarUrl() : event.getPreviousAvatarUrl();
        String newurl = event.getUser().getAvatarUrl()==null ? event.getUser().getDefaultAvatarUrl() : event.getUser().getAvatarUrl();
        ArrayList<Guild> guilds = new ArrayList<>();
        jda.getGuilds().stream().filter((g) -> (g.isMember(event.getUser()))).forEach((g) -> {
            String[] feed = feeds.feedForGuild(g, Feeds.Type.SERVERLOG);
            if(feed!=null)
            {
                String details = feed[Feeds.DETAILS];
                if (details==null || details.contains("+a"+id) || !(details.contains("-a"+id) || details.contains("+a"))) 
                {
                    guilds.add(g);
                }
            }
        });
        handler.submitFile(Feeds.Type.SERVERLOG, guilds, ()->{
                BufferedImage oldimg = OtherUtil.imageFromUrl(oldurl);
                BufferedImage newimg = OtherUtil.imageFromUrl(newurl);
                BufferedImage combo = new BufferedImage(256,128,BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g2d = combo.createGraphics();
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, 256, 128);
                if(oldimg!=null)
                {
                    g2d.drawImage(oldimg, 0, 0, 128, 128, null);
                }
                if(newimg!=null)
                {
                    g2d.drawImage(newimg, 128, 0, 128, 128, null);
                }
                File f = new File("avatarchange.png");
                try {
                    ImageIO.write(combo, "png", f);
                } catch (IOException ex) {
                    System.out.println("[ERROR] An error occured drawing the avatar.");
                }
                return new Tuple<>("\uD83D\uDDBC **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") has changed avatars:",f);
                }, 
                "\uD83D\uDDBC **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") has changed avatars:"
                    + "\nOld: "+event.getPreviousAvatarUrl()
                    + "\nNew: "+event.getUser().getAvatarUrl());
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        handler.submitText(Feeds.Type.BOTLOG, event.getJDA().getGuildById(SpConst.JAGZONE_ID), 
                "```diff\n+ JOINED : "+guild.getName()
                + "```Users : **"+guild.getUsers().size()
                +  "**\nOwner : **"+guild.getOwner().getUsername()+"** (ID:"+guild.getOwnerId()
                +   ")\nCreation : **"+MiscUtil.getCreationTime(guild.getId()).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**");
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        handler.submitText(Feeds.Type.BOTLOG, event.getJDA().getGuildById(SpConst.JAGZONE_ID), 
                "```diff\n- LEFT : "+guild.getName()
                + "```Users : **"+guild.getUsers().size()
                +  "**\nOwner : **"+guild.getOwner().getUsername()+"** (ID:"+guild.getOwnerId()
                +   ")\nCreation : **"+MiscUtil.getCreationTime(guild.getId()).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**");
    }

    @Override
    public void onVoiceLeave(VoiceLeaveEvent event) {
        if(event.getOldChannel().getUsers().isEmpty())
        {
            String[] room = rooms.get(event.getOldChannel().getId());
            if(room!=null)
            {
                event.getOldChannel().getManager().delete();
                rooms.remove(event.getOldChannel().getId());
            }
        }
    }
    
    
    
    
    public boolean isIdling()
    {
        return idling;
    }
    
    public void setIdling(boolean value)
    {
        idling = value;
    }
    
    public OffsetDateTime getStart()
    {
        return start;
    }
    
    public Command[] getCommandList()
    {
        return commands;
    }
}
