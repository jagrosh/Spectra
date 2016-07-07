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
    public Clean(FeedHandler handler)
    {
        this.handler = handler;
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
        this.cooldown=10;
        this.cooldownKey = event -> event.getAuthor()+"|"+event.getTextChannel().getId()+"|clean";
        this.requiredPermissions = new Permission[]{
            Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE
        };
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        long numposts = (long)(args[0]);
        User user = (User)(args[1]);
        MessageHistory mh = new MessageHistory(event.getTextChannel());
        event.getChannel().sendTyping();
        List<Message> messages = mh.retrieve((int)numposts);
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
        Sender.sendResponse("\uD83D\uDEAE Cleaned **"+count+"**"+(user==null ? "" : " by **"+user.getUsername()+"**"), event);
        handler.submitText(Feeds.Type.MODLOG, event.getGuild(), "\uD83D\uDDD1 **"+event.getAuthor().getUsername()+"** cleaned **"+count+"** messages "+(user==null ? "" : "by **"+user.getUsername()+"** ")+"in <#"+event.getTextChannel().getId()+">");
        return true;
    }
}
