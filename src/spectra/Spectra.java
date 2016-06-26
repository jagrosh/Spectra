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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberBanEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberUnbanEvent;
import net.dv8tion.jda.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.events.message.MessageDeleteEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.MessageUpdateEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.events.user.UserAvatarUpdateEvent;
import net.dv8tion.jda.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
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
    
    JDA jda;
    Command[] commands;
    Thread feedFlusher;
    boolean isRunning = true;
    //DataSource[] sources;
    //final Settings settings;

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().getAccountManager().setGame("Type "+SpConst.PREFIX+"help");
        feedFlusher.start();
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        //do something with game
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        
        PermLevel perm;
        boolean ignore;
        boolean isCommand;
        boolean successful;
        
        //get the settings for the server
        //settings will be null for private messages
        //make default settings if no settings exist for a server
        String[] currentSettings = (event.isPrivate() ? null : Settings.getInstance().getSettingsForGuild(event.getGuild().getId()));
        if(currentSettings==null && !event.isPrivate())
            currentSettings = Settings.getInstance().makeNewSettingsForGuild(event.getGuild().getId());
        
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
                for(Command com: commands)
                {
                    if( perm.isAtLeast(com.level) )
                        helpmsg += "\n`"+SpConst.PREFIX+com.command+"`"+Argument.arrayToString(com.arguments)+" - "+com.help;
                }
                helpmsg+="\n\nFor more information, call "+SpConst.PREFIX+"<command> help. For example, `"+SpConst.PREFIX+"tag help`";
                helpmsg+="\nFor commands, `<argument>` refers to a required argument, while `[argument]` is optional";
                helpmsg+="\nDo not add <> or [] to your arguments, nor quotation marks";
                helpmsg+="\nFor more help, contact **@jagrosh** (<@"+SpConst.JAGROSH_ID+">) or join "+SpConst.JAGZONE_INVITE;
                Sender.sendHelp(helpmsg, event.getAuthor().getPrivateChannel(), event.getTextChannel(), event.getMessage().getId());
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
                            String[] tag = Overrides.getInstance().findTag(event.getGuild(), cmd, nsfw);
                            if(tag==null)
                                tag = Tags.getInstance().findTag(cmd, null, false, nsfw);
                            if(tag==null)
                            {
                                Sender.sendResponse(SpConst.ERROR+"Tag \""+cmd+"\" no longer exists!", event.getChannel(), event.getMessage().getId());
                            }
                            else
                            {
                                Sender.sendResponse("\u180E"+JagTag.convertText(tag[Tags.CONTENTS], args[1], event.getAuthor(), event.getGuild(), event.getChannel()), 
                                    event.getChannel(), event.getMessage().getId());
                            }
                        }
                }
            }
        }
        
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
    }

    
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if(Feeds.getInstance().feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG)!=null)
            MessageCache.getInstance().addMessage(event.getGuild().getId(), event.getMessage());
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        if(Feeds.getInstance().feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG)!=null)
        {
            Message msg = MessageCache.getInstance().updateMessage(event.getGuild().getId(), event.getMessage());
            if(msg!=null && !msg.getRawContent().equals(event.getMessage().getRawContent()))
            {
                String old = FormatUtil.appendAttachmentUrls(msg);
                String newmsg = FormatUtil.appendAttachmentUrls(event.getMessage());

                FeedHandler.getInstance().submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                        "\u26A0 **"+event.getAuthor().getUsername()+"** edited a message in <#"+event.getChannel().getId()+">\n**From:** "
                        +old+"\n**To:** "+newmsg );
            }
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if(Feeds.getInstance().feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG)!=null)
        {
            Message msg = MessageCache.getInstance().deleteMessage(event.getGuild().getId(), event.getMessageId());
            if(msg!=null)
            {
                String del = FormatUtil.appendAttachmentUrls(msg);
                FeedHandler.getInstance().submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                        "\u274C **"+msg.getAuthor().getUsername()
                        +"**'s message has been deleted from <#"+msg.getChannelId()+">:\n"+del );
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        FeedHandler.getInstance().submitText(Feeds.Type.SERVERLOG, event.getGuild(), 
                "\uD83D\uDCE5 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") joined the server.");
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        FeedHandler.getInstance().submitText(Feeds.Type.SERVERLOG, event.getGuild(),
                "\uD83D\uDCE4 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") left or was kicked from the server.");
    }

    @Override
    public void onGuildMemberBan(GuildMemberBanEvent event) {
        FeedHandler.getInstance().submitText(Feeds.Type.MODLOG, event.getGuild(), 
                "\uD83D\uDD28 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") was banned from the server.");
    }

    @Override
    public void onGuildMemberUnban(GuildMemberUnbanEvent event) {
        FeedHandler.getInstance().submitText(Feeds.Type.MODLOG, event.getGuild(), 
                "\uD83D\uDD27 **"+event.getUser().getUsername()+"** (ID:"+event.getUser().getId()+") was unbanned from the server.");
    }
    
    

    
    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        CallDepend.getInstance().delete(event.getMessageId());
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        int size = event.getMessageIds().size();
        boolean servlogon = Feeds.getInstance().feedForGuild(event.getChannel().getGuild(), Feeds.Type.SERVERLOG)!=null;
        String guildid = event.getChannel().getGuild().getId();
        String header = "\u274C\u274C "+size+" messages were deleted from <#"+event.getChannel().getId()+">";
        StringBuilder builder = new StringBuilder("-- deleted messages --\n");
        event.getMessageIds().stream().forEach((id) -> {
            CallDepend.getInstance().delete(id);
            if(servlogon)
                {
                    Message m = MessageCache.getInstance().deleteMessage(guildid, id);
                    if(m!=null)
                    {
                        builder.append("[").append(m.getTime()==null ? "UNKNOWN TIME" : m.getTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("] ");
                        builder.append( m.getAuthor() == null ? "????" : m.getAuthor().getUsername() ).append(" : ");
                        builder.append(m.getContent()).append(m.getAttachments()!=null && m.getAttachments().size()>0 ? " "+m.getAttachments().get(0).getUrl() : "").append("\n\n");
                    }
                }
        });
        if(servlogon)
            FeedHandler.getInstance().submitFile(Feeds.Type.SERVERLOG, event.getChannel().getGuild(), ()->{
                File f = OtherUtil.writeArchive(builder.toString(), "deleted_messages");
            return new Tuple<>(header,f);}, header);
    }

    
    @Override
    public void onUserNameUpdate(UserNameUpdateEvent event) {
        SavedNames.getInstance().addName(event.getUser().getId(), event.getPreviousUsername());
        ArrayList<Guild> guilds = new ArrayList<>();
        jda.getGuilds().stream().filter((g) -> (g.isMember(event.getUser()))).forEach((g) -> {
            guilds.add(g);
        });
        FeedHandler.getInstance().submitText(Feeds.Type.SERVERLOG, guilds, "\uD83D\uDCDB **"+event.getPreviousUsername()+"** (ID:"
                        +event.getUser().getId()+") has changed names to **"
                        +event.getUser().getUsername()+"**");
    }

    @Override
    public void onUserAvatarUpdate(UserAvatarUpdateEvent event) {
        if(event.getUser().equals(event.getJDA().getSelfInfo()))
            return;
        String oldurl = event.getPreviousAvatarUrl()==null ? event.getUser().getDefaultAvatarUrl() : event.getPreviousAvatarUrl();
        String newurl = event.getUser().getAvatarUrl()==null ? event.getUser().getDefaultAvatarUrl() : event.getUser().getAvatarUrl();
        ArrayList<Guild> guilds = new ArrayList<>();
        jda.getGuilds().stream().filter((g) -> (g.isMember(event.getUser()))).forEach((g) -> {
            guilds.add(g);
        });
        FeedHandler.getInstance().submitFile(Feeds.Type.SERVERLOG, guilds, ()->{
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
    
    
    public void init()
    {
        commands = new Command[]{
            new About(),
            new Archive(),
            new Avatar(),
            new Channel(),
            new Draw(),
            new Info(),
            new Names(),
            new Ping(),
            new Server(),
            new Tag(),
            
            new BotScan(),
            
            new Feed()
        };
        
        Feeds.getInstance().read();
        Overrides.getInstance().read();
        SavedNames.getInstance().read();
        Settings.getInstance().read();
        Tags.getInstance().read();
        
        
        try {
            jda = new JDABuilder().addListener(this).setBotToken(OtherUtil.readFileLines("discordbot.login").get(1)).setBulkDeleteSplittingEnabled(false).buildAsync();
        } catch (LoginException | IllegalArgumentException ex) {
            System.err.println("ERROR - Building JDA : "+ex.toString());
            System.exit(1);
        }
    }
    
    public void shutdown(JDA jda)
    {
        jda.shutdown();
        isRunning=false;
        Feeds.getInstance().shutdown();
        Overrides.getInstance().shutdown();
        SavedNames.getInstance().shutdown();
        Settings.getInstance().shutdown();
        Tags.getInstance().shutdown();
    }
    
    public Spectra()
    {
        feedFlusher = new Thread(){
            @Override
            public void run()
            {
                while(isRunning)
                {
                    FeedHandler.getInstance().flush(jda);
                    try{Thread.sleep(20000);}catch(InterruptedException e){}
                }
            }
        };
    }
    
    public static void main(String[] args)
    {
        new Spectra().init();
    }
}
