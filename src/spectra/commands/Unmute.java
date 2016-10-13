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
import net.dv8tion.jda.entities.Role;
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
import spectra.datasources.Mutes;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Unmute extends Command {
    private final FeedHandler handler;
    private final Settings settings;
    private final Mutes mutes;
    private final Feeds feeds;
    public Unmute(FeedHandler handler, Settings settings, Mutes mutes, Feeds feeds)
    {
        this.handler = handler;
        this.settings = settings;
        this.mutes = mutes;
        this.feeds = feeds;
        this.command = "unmute";
        this.help = "unmutes the specified user";
        this.longhelp = "This command removes the \"Muted\" role from the specified user. "
                + "The muted role can be set up however you wish, although the recommended "
                + "can be used with `"+SpConst.PREFIX+"mute setup`.";
        this.availableInDM = false;
        this.level = PermLevel.MODERATOR;
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,true,"for"),
            new Argument("reason", Argument.Type.LONGSTRING,false)
        };
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_ROLES
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        String reason = args[1]==null?null:(String)(args[1]);
        if(reason==null)
            reason = "[no reason specified]";
        PermLevel targetLevel = PermLevel.getPermLevelForUser(target, event.getGuild(), settings.getSettingsForGuild(event.getGuild().getId()));
        //make sure a Muted role exists
        Role mutedrole = null;
        for(Role role : event.getGuild().getRoles())
            if(role.getName().equalsIgnoreCase("muted"))
            {
                mutedrole = role; break;
            }
        if(mutedrole==null)
        {
            Sender.sendResponse(SpConst.WARNING+"No \"Muted\" role exists! Please add and setup up a \"Muted\" role, or use `"
                    +SpConst.PREFIX+"mute setup` to have one made automatically.", event);
            return false;
        }
        
        //check perm level of other user
        if(targetLevel.isAtLeast(level))
        {
            Sender.sendResponse(SpConst.WARNING+"**"+target.getUsername()+"** cannot be muted because they are listed as "+targetLevel, event);
            return false;
        }
        
        //check if the other user is muted
        if(!event.getGuild().getRolesForUser(target).contains(mutedrole))
        {
            Sender.sendResponse(SpConst.WARNING+"I cannot unmute **"+target.getUsername()+"** because they are not muted!", event);
            return false;
        }
        
        //check if can interact with muted role
        if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), mutedrole))
        {
            Sender.sendResponse(SpConst.WARNING+"I cannot mute **"+target.getUsername()+"** because the \"Muted\" role is above my highest role!", event);
            return false;
        }
        
        //attempt to mute
        try{
            event.getGuild().getManager().removeRoleFromUser(target, mutedrole).update();
            mutes.remove(target.getId()+"|"+event.getGuild().getId());
            Sender.sendResponse(SpConst.SUCCESS+"**"+target.getUsername()+"** was unmuted",event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-mute"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                    "\uD83D\uDD09 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" unmuted **"+target.getUsername()+"** (ID:"+target.getId()+") for "+reason);
            return true;
        }catch(Exception e)
        {
            Sender.sendResponse(SpConst.ERROR+"Failed to unmute **"+target.getUsername()+"**", event);
            return false;
        }
    }
}
