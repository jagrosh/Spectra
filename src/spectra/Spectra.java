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
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.DisconnectEvent;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.ResumedEvent;
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
import net.dv8tion.jda.events.voice.VoiceLeaveEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.MiscUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.commands.*;
import spectra.datasources.*;
import spectra.entities.Tuple;
import spectra.tempdata.CallDepend;
import spectra.tempdata.MessageCache;
import spectra.utils.OtherUtil;
import spectra.utils.FormatUtil;

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
    
    
    //datasources
    private final Feeds feeds;
    private final Mutes mutes;
    private final Overrides overrides;
    private final Profiles profiles;
    private final Rooms rooms;
    private final SavedNames savednames;
    private final Settings settings;
    private final Tags tags;
    
    //handlers
    private final FeedHandler handler;
    
    //tempdata
    private final MessageCache messagecache;
    
    //timers
    private final ScheduledExecutorService feedflusher;
    private final ScheduledExecutorService unmuter;
    private final ScheduledExecutorService roomchecker;
    
    public static void main(String[] args)
    {
        new Spectra().init();
    }
    
    public Spectra()
    {
        feeds       = new Feeds();
        mutes       = new Mutes();
        overrides   = new Overrides();
        profiles    = new Profiles();
        rooms       = new Rooms();
        savednames  = new SavedNames();
        settings    = new Settings();
        tags        = new Tags();
        
        handler = new FeedHandler(feeds);
        messagecache = new MessageCache();
        
        feedflusher = Executors.newSingleThreadScheduledExecutor();
        unmuter  = Executors.newSingleThreadScheduledExecutor();
        roomchecker = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void init()
    {
        feeds.read();
        mutes.read();
        overrides.read();
        profiles.read();
        rooms.read();
        savednames.read();
        settings.read();
        tags.read();
        
        commands = new Command[]{
            new About(),
            new Archive(),
            new Avatar(),
            new ChannelCmd(),
            new Draw(),
            new Info(),
            new Invite(),
            new Names(savednames),
            new Ping(),
            new Profile(profiles),
            new Room(rooms, settings, handler),
            new Server(),
            new Tag(tags, overrides, settings, handler),
            
            new Ban(handler, settings),
            new BotScan(),
            new Kick(handler, settings),
            new Mute(handler, settings, mutes),
            new Softban(handler, settings, mutes),
            
            new Feed(feeds),
            new SystemCmd(this,feeds)
        };
        
        try {
            jda = new JDABuilder().addListener(this).setBotToken(OtherUtil.readFileLines("discordbot.login").get(1)).setBulkDeleteSplittingEnabled(false).buildAsync();
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
        feedflusher.scheduleWithFixedDelay(()->{handler.flush(jda);}
                , 0, 20, TimeUnit.SECONDS);
        unmuter.scheduleWithFixedDelay(()->// <editor-fold defaultstate="collapsed" desc="{unmuter}">
        {
            List<String[]> expiredMutes = mutes.getExpiredMutes();
            List<String[]> finished = new ArrayList<>();
            for(String[] mute : expiredMutes)
            {
                Guild guild = jda.getGuildById(mute[Mutes.SERVERID]);
                if(guild==null)
                    finished.add(mute);
                else if(guild.isAvailable())
                {
                    finished.add(mute);
                    User u = jda.getUserById(mute[Mutes.USERID]);
                    if(guild.isMember(u))
                    {
                        for(Role r : guild.getRolesForUser(u))
                            if(r.getName().equalsIgnoreCase("Muted"))
                            {
                                try{
                                guild.getManager().removeRoleFromUser(u, r).update();
                                handler.submitText(Feeds.Type.MODLOG, guild, "\uD83D\uDD09 **"+u.getUsername()+"** (ID:"+u.getId()+") was unmuted.");
                                }catch(Exception e){System.out.println("Unable to remove a muted role on "+guild.getName()+" ("+guild.getId()+")");}
                                break;
                            }
                    }
                    else
                    {
                        handler.submitText(Feeds.Type.MODLOG, guild, "\uD83D\uDD09 "+(u==null ? "[???]" :"**"+u.getUsername()+"**")+" (ID:"+mute[Mutes.USERID]+") was unmuted.");
                    }
                }
            }
            if(!finished.isEmpty())
                mutes.removeAll(finished);
        }// </editor-fold>
                ,0, 10, TimeUnit.SECONDS);
        roomchecker.scheduleWithFixedDelay(() -> // <editor-fold defaultstate="collapsed" desc="{roomchecker}">
        {
            int warn = 36;
            int delete = 12;
            List<String> allIds = rooms.getAllRoomIds();
            for(String id : allIds)
            {
                TextChannel tc = jda.getTextChannelById(id);
                if(tc==null)
                {
                    Guild guild = jda.getGuildById(rooms.get(id)[Rooms.SERVERID]);
                    if(guild==null || guild.isAvailable())
                        rooms.remove(id);
                    continue;
                }
                boolean checked = false;
                if(rooms.getLastActivity(id)==null)
                {
                    MessageHistory mh = new MessageHistory(tc);
                    List<Message> messages = mh.retrieve(1);
                    checked = true;
                    if(messages==null || messages.isEmpty())
                        rooms.setLastActivity(id, MiscUtil.getCreationTime(id));
                    else
                    {
                        rooms.setLastActivity(id, messages.get(0).getTime());
                        if(messages.get(0).getAuthor().equals(jda.getSelfInfo()) && messages.get(0).getRawContent().startsWith("\u200B"))
                            rooms.setWarned(id);
                    }
                }
                if(rooms.getLastActivity(id).isBefore(OffsetDateTime.now().minus(delete, ChronoUnit.HOURS)) && rooms.isWarned(id))
                {
                    if(!checked)
                    {
                        MessageHistory mh = new MessageHistory(tc);
                        List<Message> messages = mh.retrieve(1);
                        checked = true;
                        if(messages==null || messages.isEmpty())
                            rooms.setLastActivity(id, MiscUtil.getCreationTime(id));
                        else
                        {
                            rooms.setLastActivity(id, messages.get(0).getTime());
                            if(messages.get(0).getAuthor().equals(jda.getSelfInfo()) && messages.get(0).getRawContent().startsWith("\u200B"))
                                rooms.setWarned(id);
                        }
                        if(rooms.getLastActivity(id).isBefore(OffsetDateTime.now().minus(delete, ChronoUnit.HOURS)) && rooms.isWarned(id))
                        {
                            rooms.remove(id);
                            handler.submitText(Feeds.Type.SERVERLOG, tc.getGuild(), "\uD83D\uDCFA Text channel **"+tc.getName()+
                                "** (ID:"+tc.getId()+") has been removed due to inactivity.");
                            tc.getManager().delete();
                            continue;
                        }
                    }
                    else
                    {
                        rooms.remove(id);
                        handler.submitText(Feeds.Type.SERVERLOG, tc.getGuild(), "\uD83D\uDCFA Text channel **"+tc.getName()+
                            "** (ID:"+tc.getId()+") has been removed due to inactivity.");
                        tc.getManager().delete();
                        continue;
                    }
                }
                if(rooms.getLastActivity(id).isBefore(OffsetDateTime.now().minus(warn, ChronoUnit.HOURS)))
                {
                    if(!checked)
                    {
                        MessageHistory mh = new MessageHistory(tc);
                        List<Message> messages = mh.retrieve(1);
                        //checked = true;
                        if(messages==null || messages.isEmpty())
                            rooms.setLastActivity(id, MiscUtil.getCreationTime(id));
                        else
                        {
                            rooms.setLastActivity(id, messages.get(0).getTime());
                            if(messages.get(0).getAuthor().equals(jda.getSelfInfo()) && messages.get(0).getRawContent().startsWith("\u200B"))
                                rooms.setWarned(id);
                        }
                        if(rooms.getLastActivity(id).isBefore(OffsetDateTime.now().minus(warn, ChronoUnit.HOURS)))
                        {
                            //warn
                            Sender.sendMsg(String.format(SpConst.ROOM_WARNING, "<@"+rooms.get(id)[Rooms.OWNERID]+">"), tc);
                            rooms.setWarned(id);
                            //continue;
                        }
                    }
                    else
                    {
                        //warn
                        Sender.sendMsg(String.format(SpConst.ROOM_WARNING, "<@"+rooms.get(id)[Rooms.OWNERID]+">"), tc);
                        rooms.setWarned(id);
                        //continue;
                    }
                }
            }
        }// </editor-fold>
                , 0, 120, TimeUnit.SECONDS);
    }
    
    public void shutdown(JDA jda)
    {
        jda.shutdown();
        
        feeds.shutdown();
        mutes.shutdown();
        overrides.shutdown();
        profiles.shutdown();
        rooms.shutdown();
        savednames.shutdown();
        settings.shutdown();
        tags.shutdown();
        
        feedflusher.shutdown();
        unmuter.shutdown();
        roomchecker.shutdown();
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
    public void onMessageReceived(MessageReceivedEvent event) {
        
        PermLevel perm;
        boolean ignore;
        boolean isCommand;
        boolean successful;
        
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
        
        if(strippedMessage!=null)//potential command right here
        {
            strippedMessage = strippedMessage.trim();
            if(strippedMessage.equalsIgnoreCase("help"))//send full help message (based on access level)
            {//we don't worry about ignores for help
                isCommand = true;
                successful = true;
                String helpmsg = "**Available help "+(event.isPrivate() ? "via Direct Message" : "in <#"+event.getTextChannel().getId()+">")+"**:";
                PermLevel current = PermLevel.EVERYONE;
                for(Command com: commands)
                {
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
                                Sender.sendResponse("\u180E"+JagTag.convertText(tag[Tags.CONTENTS], args[1], event.getAuthor(), event.getGuild(), event.getChannel()), event);
                            }
                        }
                }
            }
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
        if(feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG)!=null)
        {
            Message msg = messagecache.updateMessage(event.getGuild().getId(), event.getMessage());
            if(msg!=null && !msg.getRawContent().equals(event.getMessage().getRawContent()))
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
        if(feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG)!=null)
        {
            Message msg = messagecache.deleteMessage(event.getGuild().getId(), event.getMessageId());
            if(msg!=null)
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
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                "\uD83D\uDCE4 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") left or was kicked from the server.");
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
        String id = event.getUser().getId();
        if(feed!=null)
        {
            if(feed[Feeds.DETAILS]!=null && (feed[Feeds.DETAILS].contains("+n"+id) || (!feed[Feeds.DETAILS].contains("-n"+id) && !feed[Feeds.DETAILS].contains("+n"))))
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
    public void onUserAvatarUpdate(UserAvatarUpdateEvent event) {
        if(event.getUser().equals(event.getJDA().getSelfInfo()) || event.getUser().getId().equals("135251434445733888"))
            return;
        String id = event.getUser().getId();
        String oldurl = event.getPreviousAvatarUrl()==null ? event.getUser().getDefaultAvatarUrl() : event.getPreviousAvatarUrl();
        String newurl = event.getUser().getAvatarUrl()==null ? event.getUser().getDefaultAvatarUrl() : event.getUser().getAvatarUrl();
        ArrayList<Guild> guilds = new ArrayList<>();
        jda.getGuilds().stream().filter((g) -> (g.isMember(event.getUser()))).forEach((g) -> {
            String[] feed = feeds.feedForGuild(g, Feeds.Type.SERVERLOG);
            if (feed!=null && feed[Feeds.DETAILS]!=null && (feed[Feeds.DETAILS].contains("+a"+id) || (!feed[Feeds.DETAILS].contains("-a"+id) && !feed[Feeds.DETAILS].contains("+a")))) {
                guilds.add(g);
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
    
    
}
