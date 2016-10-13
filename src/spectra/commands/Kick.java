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

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Feeds;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Kick extends Command {
    private final FeedHandler handler;
    private final Settings settings;
    private final Feeds feeds;
    public Kick(FeedHandler handler, Settings settings, Feeds feeds)
    {
        this.handler = handler;
        this.settings = settings;
        this.feeds = feeds;
        this.command = "kick";
        this.help = "kicks a user from the server";
        this.longhelp = "This command kicks the specified user from the server.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,true,"for"),
            new Argument("reason",Argument.Type.LONGSTRING,false)
        };
        this.availableInDM=false;
        this.level = PermLevel.MODERATOR;
        this.requiredPermissions = new Permission[]{
            Permission.KICK_MEMBERS
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        String reason = args[1]==null?null:(String)(args[1]);
        if(reason==null)
            reason = "[no reason specified]";
        PermLevel targetLevel = PermLevel.getPermLevelForUser(target, event.getGuild(), settings.getSettingsForGuild(event.getGuild().getId()));
        //check perm level of other user
        if(targetLevel.isAtLeast(level))
        {
            Sender.sendResponse(SpConst.WARNING+"**"+target.getUsername()+"** cannot be kicked because they are listed as "+targetLevel, event);
            return false;
        }
        
        //check if bot can interact with the other user
        if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), target, event.getGuild()))
        {
            Sender.sendResponse(SpConst.WARNING+"I cannot kick **"+target.getUsername()+"** due to permission hierarchy", event);
            return false;
        }
        
        //attempt to kick
        try{
            event.getGuild().getManager().kick(target);
            Sender.sendResponse(SpConst.SUCCESS+"**"+target.getUsername()+"** was kicked from the server \uD83D\uDC62", event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-kick"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                        "\uD83D\uDC62 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                            +" kicked **"+target.getUsername()+"** (ID:"+target.getId()+") for "+reason);
            return true;
        }catch(Exception e)
        {
            Sender.sendResponse(SpConst.ERROR+"Failed to kick **"+target.getUsername()+"**", event);
            return false;
        }
    }
}
