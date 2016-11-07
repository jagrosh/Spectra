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
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Feeds;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Feed extends Command {
    private final Feeds feeds;
    public Feed(Feeds feeds)
    {
        this.feeds = feeds;
        this.command = "feed";
        this.aliases = new String[]{"log"};
        this.help = "sets or removes a feed";
        this.longhelp = "This command sets up or removes feeds on the server.";
        this.level = PermLevel.ADMIN;
        this.arguments = new Argument[]{
            new Argument("feedtype",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new FeedAnnouncements(),
            new FeedList(),
            new FeedModlog(),
            new FeedRemove(),
            new FeedServerlog(),
            new FeedTaglog(),
            new FeedTwitch()
        };
        this.availableInDM = false;
    }
    
    private class FeedModlog extends Command {
        private FeedModlog()
        {
            this.command = "modlog";
            this.help = "sets the `modlog` feed, which displays moderation actions taken on the server";
            this.longhelp = "This command is used to set the `modlog` feed. This feed keeps track of moderator "
                    + "actions taken on the server, such as kicks, bans, mutes, and cleans, as well as the reasons "
                    + "for the actions (if included in the command).";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false),
                new Argument("options",Argument.Type.LONGSTRING,false)
            };
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel tchan = (TextChannel)(args[0]);
            String options = args[1]==null ? "" : (String)args[1];
            if(tchan==null)
                tchan = event.getTextChannel();
            //check bot permissions for channel
            if(!PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, tchan) || !PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_READ, tchan))
            {
                Sender.sendResponse(String.format(SpConst.NEED_PERMISSION,Permission.MESSAGE_READ+", "+Permission.MESSAGE_WRITE+
                        ", and preferably "+Permission.MESSAGE_ATTACH_FILES), event);
                return false;
            }
            String str = "";
            String[] current = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(current!=null)
                str+=SpConst.WARNING+"Feed "+Feeds.Type.MODLOG+" has been removed from <#"+current[Feeds.CHANNELID]+">\n";
            feeds.set(new String[]{tchan.getId(),Feeds.Type.MODLOG.toString(),event.getGuild().getId(),options});
            str+=SpConst.SUCCESS+"Feed "+Feeds.Type.MODLOG+" has been added to <#"+tchan.getId()+">"
                    + "\n*The `modlog` feed is for tracking bans, and other moderator commands like kicks, mutes, and cleans*"
                    + "\n*If you want a feed to track message edits, message deletes, avatar changes, and more, use the `serverlog` feed.*";
            Sender.sendResponse(str, event);
            return true;
        }
    }
    
    private class FeedServerlog extends Command {
        private FeedServerlog()
        {
            this.command = "serverlog";
            this.help = "sets the `serverlog` feed, which displays various activity in the server";
            this.longhelp = "This command sets the `serverlog` feed, which displays various activity on the server, "
                    + "including message deletes, message edits, username changes, avatar changes, nickname changes, "
                    + "server joins, server leaves, and room changes. Additionally, options can be provided to exclude "
                    + "some or all users and/or actions from this feed. For more details on these options, please read "
                    + "<https://github.com/jagrosh/Spectra/wiki/Serverlog-Feed-Parameters>.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false),
                new Argument("options",Argument.Type.LONGSTRING,false)
            };
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel tchan = (TextChannel)(args[0]);
            String options = args[1]==null ? "" : (String)args[1];
            if(tchan==null)
                tchan = event.getTextChannel();
            //check bot permissions for channel
            if(!PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, tchan) || !PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_READ, tchan))
            {
                Sender.sendResponse(String.format(SpConst.NEED_PERMISSION,Permission.MESSAGE_READ+", "+Permission.MESSAGE_WRITE+
                        ", and preferably "+Permission.MESSAGE_ATTACH_FILES), event);
                return false;
            }
            String str = "";
            String[] current = feeds.feedForGuild(event.getGuild(), Feeds.Type.SERVERLOG);
            if(current!=null)
                str+=SpConst.WARNING+"Feed "+Feeds.Type.SERVERLOG+" has been removed from <#"+current[Feeds.CHANNELID]+">\n";
            feeds.set(new String[]{tchan.getId(),Feeds.Type.SERVERLOG.toString(),event.getGuild().getId(),options});
            str+=SpConst.SUCCESS+"Feed "+Feeds.Type.SERVERLOG+" has been added to <#"+tchan.getId()+">";
            Sender.sendResponse(str, event);
            return true;
        }
    }
    
    private class FeedTwitch extends Command {
        private FeedTwitch()
        {
            this.command = "twitch";
            this.help = "sets the `twitch` feed, which shows when users begin/end streaming";
            this.longhelp = "This command sets the `twitch` feed, which displays when users on the server begin or end "
                    + "streaming sessions on twitch. To only show streams for some users, insert their IDs in the feed "
                    + "options in the form +sID. Example: `+s113156185389092864`";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false),
                new Argument("options",Argument.Type.LONGSTRING,false)
            };
            this.availableInDM = false;
            this.whitelistCooldown = -1;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel tchan = (TextChannel)(args[0]);
            String options = args[1]==null ? "" : (String)args[1];
            if(tchan==null)
                tchan = event.getTextChannel();
            //check bot permissions for channel
            if(!PermissionUtil.checkPermission(tchan, event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE) || !PermissionUtil.checkPermission(tchan, event.getJDA().getSelfInfo(), Permission.MESSAGE_READ))
            {
                Sender.sendResponse(String.format(SpConst.NEED_PERMISSION,Permission.MESSAGE_READ+", "+Permission.MESSAGE_WRITE+
                        ", and preferably "+Permission.MESSAGE_ATTACH_FILES), event);
                return false;
            }
            String str = "";
            String[] current = feeds.feedForGuild(event.getGuild(), Feeds.Type.TWITCH);
            if(current!=null)
                str+=SpConst.WARNING+"Feed "+Feeds.Type.TWITCH+" has been removed from <#"+current[Feeds.CHANNELID]+">\n";
            feeds.set(new String[]{tchan.getId(),Feeds.Type.TWITCH.toString(),event.getGuild().getId(),options});
            str+=SpConst.SUCCESS+"Feed "+Feeds.Type.TWITCH+" has been added to <#"+tchan.getId()+">";
            Sender.sendResponse(str, event);
            return true;
        }
    }
    
    private class FeedTaglog extends Command {
        private FeedTaglog()
        {
            this.command = "taglog";
            this.help = "sets the `taglog` feed, which displays changes to tags by members of the server";
            this.longhelp = "This command sets the `taglog` feed, which displays any tag changes by users "
                    + "on the current server. The full tag log can be found on jagrosh's bot server.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false)
            };
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel tchan = (TextChannel)(args[0]);
            if(tchan==null)
                tchan = event.getTextChannel();
            //check bot permissions for channel
            if(!PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, tchan) || !PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_READ, tchan))
            {
                Sender.sendResponse(String.format(SpConst.NEED_PERMISSION,Permission.MESSAGE_READ+", "+Permission.MESSAGE_WRITE+
                        ", and preferably "+Permission.MESSAGE_ATTACH_FILES), event);
                return false;
            }
            String str = "";
            String[] current = feeds.feedForGuild(event.getGuild(), Feeds.Type.TAGLOG);
            if(current!=null)
                str+=SpConst.WARNING+"Feed "+Feeds.Type.TAGLOG+" has been removed from <#"+current[Feeds.CHANNELID]+">\n";
            feeds.set(new String[]{tchan.getId(),Feeds.Type.TAGLOG.toString(),event.getGuild().getId(),""});
            str+=SpConst.SUCCESS+"Feed "+Feeds.Type.TAGLOG+" has been added to <#"+tchan.getId()+">";
            Sender.sendResponse(str, event);
            return true;
        }
    }
    
    private class FeedAnnouncements extends Command {
        private FeedAnnouncements()
        {
            this.command = "announcements";
            this.help = "sets the `announcements` feed, which relays important updates and information about "+SpConst.BOTNAME;
            this.longhelp = "This command sets the `announcements` feed, which is used for jagrosh to send important information "
                    + "and updates about "+SpConst.BOTNAME+".";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false)
            };
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel tchan = (TextChannel)(args[0]);
            if(tchan==null)
                tchan = event.getTextChannel();
            //check bot permissions for channel
            if(!PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, tchan) || !PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_READ, tchan))
            {
                Sender.sendResponse(String.format(SpConst.NEED_PERMISSION,Permission.MESSAGE_READ+", "+Permission.MESSAGE_WRITE+
                        ", and preferably "+Permission.MESSAGE_ATTACH_FILES), event);
                return false;
            }
            String str = "";
            String[] current = feeds.feedForGuild(event.getGuild(), Feeds.Type.ANNOUNCEMENTS);
            if(current!=null)
                str+=SpConst.WARNING+"Feed "+Feeds.Type.ANNOUNCEMENTS+" has been removed from <#"+current[Feeds.CHANNELID]+">\n";
            feeds.set(new String[]{tchan.getId(),Feeds.Type.ANNOUNCEMENTS.toString(),event.getGuild().getId(),""});
            str+=SpConst.SUCCESS+"Feed "+Feeds.Type.ANNOUNCEMENTS+" has been added to <#"+tchan.getId()+">";
            Sender.sendResponse(str, event);
            return true;
        }
    }
    
    private class FeedList extends Command {
        private FeedList()
        {
            this.command = "list";
            this.help = "shows the current feeds on the server";
            this.longhelp = "This command shows the current feeds on the server, as well as their locations, "
                    + "and any options that have been set.";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            List<String[]> feedlist = feeds.findFeedsForGuild(event.getGuild());
            if(feedlist.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"No feeds found on **"+event.getGuild().getName()+"**", event);
                return true;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS).append(feedlist.size()).append(" feeds found on **").append(event.getGuild().getName()).append("**:");
            feedlist.stream().forEach((feed) -> {
                builder.append("\n`").append(feed[Feeds.FEEDTYPE]).append("` - <#").append(feed[Feeds.CHANNELID])
                        .append(">").append((feed[Feeds.DETAILS]!=null && !feed[Feeds.DETAILS].equals("")) ? " : "+feed[Feeds.DETAILS] : "");
            });
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private class FeedRemove extends Command {
        private FeedRemove()
        {
            this.command = "remove";
            this.aliases = new String[]{"clear","delete"};
            this.help = "removes a feed";
            this.longhelp = "This command removes a feed from the server, and clears any option on that feed.";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("feed name",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String feedname = (String)(args[0]);
            List<String[]> feedlist = feeds.findFeedsForGuild(event.getGuild());
            if(feedlist.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"No feeds found on **"+event.getGuild().getName()+"**", event);
                return true;
            }
            for(String[] feed : feedlist)
                if(feed[Feeds.FEEDTYPE].equalsIgnoreCase(feedname))
                {
                    feeds.removeFeed(feed);
                    Sender.sendResponse(SpConst.SUCCESS+"Removed feed `"+feed[Feeds.FEEDTYPE]+"` from <#"+feed[Feeds.CHANNELID]+">", event);
                    return true;
                }
            Sender.sendResponse(SpConst.ERROR+"No feeds found matching \""+feedname+"\"", event);
            return false;
        }
    }
}
