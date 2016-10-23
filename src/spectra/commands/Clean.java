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
import java.util.stream.Collectors;
import net.dv8tion.jda.EmbedType;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.User;
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
public class Clean extends Command {
    private final FeedHandler handler;
    private final Feeds feeds;
    public Clean(FeedHandler handler, Feeds feeds)
    {
        this.handler = handler;
        this.feeds = feeds;
        this.command = "clean";
        this.aliases = new String[]{"clear","purge"};
        this.help = "deletes posts within the specified number";
        this.longhelp = "This command is used to delete a lot of posts very quickly. Without a given user, "
                + "it will delete all posts within the number given. If a user is provided, it will look within "
                + "the given number of posts and only delete posts by that user.";
        this.arguments= new Argument[]{
            new Argument("numposts",Argument.Type.INTEGER,true,2,1000), 
            new Argument("username",Argument.Type.USER,false)};//<numposts> [channel]
        this.availableInDM = false;
        this.level = PermLevel.MODERATOR;
        this.cooldown=8;
        this.cooldownKey = event -> event.getGuild().getId()+"|clean";
        this.requiredPermissions = new Permission[]{
            Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
        };
        this.children = new Command[]{
            new CleanBots(),
            new CleanContaining(),
            new CleanImages(),
            new CleanLinks(),
            new CleanRegex()
        };
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        long numposts = (long)(args[0]);
        User user = (User)(args[1]);
        MessageHistory mh = new MessageHistory(event.getTextChannel());
        event.getChannel().sendTyping();
        List<Message> messages = mh.retrieve((int)numposts+1);
        List<Message> toDelete;
        messages.remove(event.getMessage());
        if(user==null)
            toDelete = messages;
        else
        {
            toDelete = new ArrayList<>();
            messages.stream().filter((m) -> (m.getAuthor()!=null && m.getAuthor().equals(user))).forEach((m) -> {
                toDelete.add(m);
            });
        }
        if(toDelete.isEmpty())
        {
            Sender.sendResponse(SpConst.WARNING+"There were no messages to delete", event);
            return false;
        }
        int count = toDelete.size();
        if(toDelete.size()%100==1)
        {
            toDelete.get(0).deleteMessage();
            toDelete.remove(0);
        }
        int index = 0;
        while(index < toDelete.size())
        {
            if(index+100 < toDelete.size())
                event.getTextChannel().deleteMessages(toDelete.subList(index, index+100));
            else
                event.getTextChannel().deleteMessages(toDelete.subList(index, toDelete.size()));
            index+=100;
            try{Thread.sleep(1100);}catch(Exception e){}
        }
        Sender.sendResponse("\uD83D\uDEAE Cleaned **"+count+"** messages"+(user==null ? "" : " by **"+user.getUsername()+"**"), event);
        String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
        if(feed!=null && !feed[Feeds.DETAILS].contains("-clean"))
            handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                    +" cleaned **"+count+"** messages "+(user==null ? "" : "by **"+user.getUsername()+"** ")+"in <#"+event.getTextChannel().getId()+">");
        return true;
    }
    
    private class CleanLinks extends Command {
        private CleanLinks()
        {
            this.command = "links";
            this.help = "deletes recent posts with links";
            this.longhelp = "This command deletes posts (within the last 100) that contain links";
            this.availableInDM = false;
            this.level = PermLevel.MODERATOR;
            this.cooldown=2;
            this.cooldownKey = event -> event.getGuild().getId()+"|clean";
            this.requiredPermissions = new Permission[]{
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
            };
            this.arguments= new Argument[]{
                new Argument("numposts",Argument.Type.INTEGER,false,2,100)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            long posts = args[0]==null ? 100 : (long)args[0];
            List<Message> toDelete = event.getTextChannel().getHistory().retrieve((int)posts);
            toDelete.remove(event.getMessage());
            toDelete = toDelete.stream().filter((m) -> {return m.getRawContent().matches("(?s).*https?:\\/\\/.+");}).collect(Collectors.toList());
            if(toDelete.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There were no messages to delete", event);
                return false;
            }
            if(toDelete.size()==1)
                toDelete.get(0).deleteMessage();
            else
                event.getTextChannel().deleteMessages(toDelete);
            Sender.sendResponse("\uD83D\uDEAE Cleaned **"+toDelete.size()+"** messages containing links", event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-clean"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" cleaned **"+toDelete.size()+"** messages *containing links* in <#"+event.getTextChannel().getId()+">");
            return true;
        }
    }
    
    private class CleanImages extends Command {
        private CleanImages()
        {
            this.command = "images";
            this.aliases = new String[]{"image","img","embeds"};
            this.help = "deletes recent posts with image uploads or embeds";
            this.longhelp = "This command deletes posts (within the last 100) that contain image uploads or embeds";
            this.availableInDM = false;
            this.level = PermLevel.MODERATOR;
            this.cooldown=2;
            this.cooldownKey = event -> event.getGuild().getId()+"|clean";
            this.requiredPermissions = new Permission[]{
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
            };
            this.arguments= new Argument[]{
                new Argument("numposts",Argument.Type.INTEGER,false,2,100)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            long posts = args[0]==null ? 100 : (long)args[0];
            List<Message> toDelete = event.getTextChannel().getHistory().retrieve((int)posts)
                .stream().filter((m) -> {
                    return  m.getEmbeds().stream().anyMatch((me) -> (me.getType()==EmbedType.IMAGE || me.getType()==EmbedType.VIDEO)) || 
                        m.getAttachments().stream().anyMatch((a) -> (a.isImage()));
                }).collect(Collectors.toList());
            if(toDelete.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There were no messages to delete", event);
                return false;
            }
            if(toDelete.size()==1)
                toDelete.get(0).deleteMessage();
            else
                event.getTextChannel().deleteMessages(toDelete);
            Sender.sendResponse("\uD83D\uDEAE Cleaned **"+toDelete.size()+"** messages containing images", event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-clean"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" cleaned **"+toDelete.size()+"** messages *containing images* in <#"+event.getTextChannel().getId()+">");
            return true;
        }
    }
    
    private class CleanBots extends Command {
        private CleanBots()
        {
            this.command = "bots";
            this.aliases = new String[]{"bot"};
            this.help = "deletes posts by bots, and most commands";
            this.longhelp = "This command deletes posts (within the last 100) that were posted by a bot, or that begin with punctuation";
            this.availableInDM = false;
            this.level = PermLevel.MODERATOR;
            this.cooldown=2;
            this.cooldownKey = event -> event.getGuild().getId()+"|clean";
            this.requiredPermissions = new Permission[]{
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
            };
            this.arguments= new Argument[]{
                new Argument("numposts",Argument.Type.INTEGER,false,2,100)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            long posts = args[0]==null ? 100 : (long)args[0];
            List<Message> toDelete = event.getTextChannel().getHistory().retrieve((int)posts);
            toDelete.remove(event.getMessage());
            toDelete = toDelete.stream().filter((m) -> {
                    return (m.getAuthor()!=null && m.getAuthor().isBot()) || !m.getRawContent().matches("^([A-Za-z0-9]|(<@\\d+>)).*");
                }).collect(Collectors.toList());
            if(toDelete.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There were no messages to delete", event);
                return false;
            }
            if(toDelete.size()==1)
                toDelete.get(0).deleteMessage();
            else
                event.getTextChannel().deleteMessages(toDelete);
            Sender.sendResponse("\uD83D\uDEAE Cleaned **"+toDelete.size()+"** messages containing bot posts or commands", event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-clean"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" cleaned **"+toDelete.size()+"** messages *containing bot posts or commands* in <#"+event.getTextChannel().getId()+">");
            return true;
        }
    }
    
    private class CleanContaining extends Command {
        private CleanContaining()
        {
            this.command = "containing";
            this.aliases = new String[]{"contains","contain"};
            this.help = "deletes posts containing the given text";
            this.longhelp = "This command deletes posts (within the last 100) that contain the given text";
            this.availableInDM = false;
            this.level = PermLevel.MODERATOR;
            this.cooldown=2;
            this.cooldownKey = event -> event.getGuild().getId()+"|clean";
            this.requiredPermissions = new Permission[]{
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
            };
            this.arguments = new Argument[]{
                new Argument("phrase",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String words = (String)args[0];
            List<Message> toDelete = event.getTextChannel().getHistory().retrieve(100);
            toDelete.remove(event.getMessage());
            toDelete = toDelete.stream().filter((m) -> {
                   return m.getRawContent().toLowerCase().contains(words.toLowerCase());
               }).collect(Collectors.toList());
            if(toDelete.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There were no messages to delete", event);
                return false;
            }
            if(toDelete.size()==1)
                toDelete.get(0).deleteMessage();
            else
                event.getTextChannel().deleteMessages(toDelete);
            Sender.sendResponse("\uD83D\uDEAE Cleaned **"+toDelete.size()+"** messages containing \""+words+"\"", event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-clean"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" cleaned **"+toDelete.size()+"** messages *containing* \""+words+"\" in <#"+event.getTextChannel().getId()+">");
            return true;
        }
    }
    
    private class CleanRegex extends Command {
        private CleanRegex()
        {
            this.command = "matching";
            this.aliases = new String[]{"regex"};
            this.help = "deletes posts matching the given regex";
            this.longhelp = "This command deletes posts (within the last 100) that match the given regex";
            this.availableInDM = false;
            this.level = PermLevel.MODERATOR;
            this.cooldown=2;
            this.cooldownKey = event -> event.getGuild().getId()+"|clean";
            this.requiredPermissions = new Permission[]{
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
            };
            this.arguments = new Argument[]{
                new Argument("regex",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String regex = (String)args[0];
            List<Message> toDelete;
            try{
                 toDelete = event.getTextChannel().getHistory().retrieve(100);
                 toDelete.remove(event.getMessage());
                 toDelete = toDelete.stream().filter((m) -> {
                        return m.getRawContent().matches(regex);
                    }).collect(Collectors.toList());
            } catch (Exception e){
                Sender.sendResponse(SpConst.WARNING+"Invalid regex", event);
                return false;
            }
            if(toDelete.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There were no messages to delete", event);
                return false;
            }
            if(toDelete.size()==1)
                toDelete.get(0).deleteMessage();
            else
                event.getTextChannel().deleteMessages(toDelete);
            Sender.sendResponse("\uD83D\uDEAE Cleaned **"+toDelete.size()+"** messages matching `"+regex+"`", event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-clean"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" cleaned **"+toDelete.size()+"** messages *matching* `"+regex+"` in <#"+event.getTextChannel().getId()+">");
            return true;
        }
    }
}
