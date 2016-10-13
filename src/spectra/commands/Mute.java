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
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
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
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Mute extends Command {
    private final FeedHandler handler;
    private final Settings settings;
    private final Mutes mutes;
    private final Feeds feeds;
    public Mute(FeedHandler handler, Settings settings, Mutes mutes, Feeds feeds)
    {
        this.handler = handler;
        this.settings = settings;
        this.mutes = mutes;
        this.feeds = feeds;
        this.command = "mute";
        this.help = "mutes the specified user for the given time";
        this.longhelp = "This command applies the \"Muted\" role to the specified user for the specified "
                + "length of time. The muted role can be set up however you wish, although the recommended "
                + "can be used with `"+SpConst.PREFIX+"mute setup`. If a user is muted, leaves the server, and "
                + "rejoins during the time they should still be muted, the role will be re-applied on entry.";
        this.availableInDM = false;
        this.level = PermLevel.MODERATOR;
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,true,"for"),
            new Argument("time", Argument.Type.TIME, true, "for", 0, 432000),
            new Argument("reason", Argument.Type.LONGSTRING,false)
        };
        this.children = new Command[]{
            new MuteList(),
            new MuteSetup()
        };
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_ROLES
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        long seconds = (long)(args[1]);
        String reason = args[2]==null?null:(String)(args[2]);
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
        
        //check if bot can interact with the other user
        if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), target, event.getGuild()))
        {
            Sender.sendResponse(SpConst.WARNING+"I cannot mute **"+target.getUsername()+"** due to permission hierarchy", event);
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
            event.getGuild().getManager().addRoleToUser(target, mutedrole).update();
            mutes.set(new String[]{target.getId(),event.getGuild().getId(),OffsetDateTime.now().plusSeconds(seconds).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)});
            Sender.sendResponse(SpConst.SUCCESS+"**"+target.getUsername()+"** was muted for "+FormatUtil.secondsToTime(seconds), event);
            String[] feed = feeds.feedForGuild(event.getGuild(), Feeds.Type.MODLOG);
            if(feed!=null && !feed[Feeds.DETAILS].contains("-mute"))
                handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                    "\uD83D\uDD07 **"+event.getAuthor().getUsername()+"**#"+event.getAuthor().getDiscriminator()
                        +" muted **"+target.getUsername()+"** (ID:"+target.getId()+") for "+FormatUtil.secondsToTime(seconds)+" for "+reason);
            return true;
        }catch(Exception e)
        {
            Sender.sendResponse(SpConst.ERROR+"Failed to mute **"+target.getUsername()+"**", event);
            return false;
        }
    }
    
    private class MuteList extends Command
    {
        private MuteList()
        {
            this.command = "list";
            this.help = "lists users with a mute on the server";
            this.longhelp = "This command shows the list of users that are muted on the server.";
            this.availableInDM = false;
            this.level = PermLevel.MODERATOR;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
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
            List<User> list = event.getGuild().getUsersWithRole(mutedrole);
            List<String[]> list2 = mutes.getMutesForGuild(event.getGuild().getId());
            StringBuilder builder = new StringBuilder();
            int count = list.size();
            list.stream().map((u) -> {
                builder.append("\n**").append(u.getUsername()).append("** (ID:").append(u.getId()).append(")");
                return u;
            }).map((u) -> mutes.getMute(u.getId(), event.getGuild().getId())).filter((savedmute) -> (savedmute!=null)).forEach((savedmute) -> {
                builder.append(" ends in ").append(FormatUtil.secondsToTime(OffsetDateTime.now().until(OffsetDateTime.parse(savedmute[Mutes.UNMUTETIME]), ChronoUnit.SECONDS)));
            });
            for(String[] savedmute : list2)
            {
                User u = event.getJDA().getUserById(savedmute[Mutes.USERID]);
                if(u==null || !event.getGuild().isMember(u))
                {
                    count++;
                    builder.append("ID:").append(savedmute[Mutes.USERID]).append(" ends in ").append(FormatUtil.secondsToTime(OffsetDateTime.now().until(OffsetDateTime.parse(savedmute[Mutes.UNMUTETIME]), ChronoUnit.SECONDS)));
                }
            }
            if(count==0)
            {
                Sender.sendResponse(SpConst.WARNING+"There are no muted users!", event);
                return true;
            }
            Sender.sendResponse(SpConst.SUCCESS+"**"+count+"** users muted on **"+event.getGuild().getName()+"**:"+builder.toString(), event);
            return true;
        }
    }
    
    private class MuteSetup extends Command
    {
        private MuteSetup()
        {
            this.command = "setup";
            this.help = "sets up the muted role on the server";
            this.longhelp = "This command sets up the \"Muted\" role on the server. The default configuration "
                    + "disallows sending messages in any channels, as well as being unable to connect to voice channels. "
                    + "It it recommened to create all channels on the server before running this setup.";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("confirmation",Argument.Type.SHORTSTRING,false)
            };
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String confirmation = args[0]==null ? null : (String)args[0];
            String trueconfirm = event.getGuild().getId().substring(4, 7)+event.getAuthor().getId().substring(4, 7);
            if(confirmation!=null && !confirmation.equals(trueconfirm))
            {
                Sender.sendResponse(SpConst.ERROR+"Incorrect confirmation code! Please type `"+SpConst.PREFIX+"mute setup` to see the status and get the confirmation code.", event);
                return false;
            }
            Role mutedrole = null;
            for(Role role : event.getGuild().getRoles())
                if(role.getName().equalsIgnoreCase("muted"))
                {
                    mutedrole = role;
                    break;
                }
            if(mutedrole!=null && !PermissionUtil.canInteract(event.getJDA().getSelfInfo(), mutedrole))
            {
                Sender.sendResponse(SpConst.ERROR+"Please delete the current muted role, or move it below my highest role, before continuing setup.", event);
                return false;
            }
            if(confirmation==null)
            {
                String str = SpConst.WARNING+"Running this command will set up a muted role on this server.\n";
                if(mutedrole==null)
                    str+="This will created the role and set up the permissions.";
                else
                    str+="The role already exists. This will overwrite the current permissions.";
                str+="Type `"+SpConst.PREFIX+"mute setup "+trueconfirm+"` to continue.";
                Sender.sendResponse(str, event);
                return true;
            }
            try
            {
                event.getChannel().sendTyping();
                if(mutedrole==null)
                {
                    mutedrole = event.getGuild().createRole().getRole();
                    int pos = event.getGuild().getRolesForUser(event.getJDA().getSelfInfo()).get(0).getPosition();
                    mutedrole.getManager().move(pos-1).revoke(Permission.values()).setColor(11).setName("Muted").update();
                }
                for(TextChannel tc: event.getGuild().getTextChannels())
                    tc.createPermissionOverride(mutedrole).deny(Permission.MESSAGE_WRITE).update();
                for(VoiceChannel vc: event.getGuild().getVoiceChannels())
                    vc.createPermissionOverride(mutedrole).deny(Permission.VOICE_CONNECT,Permission.VOICE_SPEAK).update();
                Sender.sendResponse(SpConst.SUCCESS+"The muted role has been set up. Please make sure everything is in order "
                        + "by checking the position of the role, as well as the channel specific permissions.", event);
                return true;
            } catch (Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"Something went wrong while setting up. Please make sure "
                        + "I have permission to edit/create roles, and modify every channel. Alternatively, give me the "
                        + "`Administrator` permission for setting up. If this still fails, please contact jagrosh.", event);
                return false;
            }
        }
    }
}
