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
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.PermissionOverride;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Feeds;
import spectra.datasources.Rooms;
import spectra.datasources.Settings;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Room extends Command 
{
    private final Rooms rooms;
    private final Settings settings;
    private final FeedHandler handler;
    public Room(Rooms rooms, Settings settings, FeedHandler handler)
    {
        this.rooms = rooms;
        this.settings = settings;
        this.handler = handler;
        this.command = "room";
        this.help = "private room management";
        this.longhelp = "This command is used for users on the server to make and manage "
                + "their own private text channels or temporary voice channels. These channels "
                + "are hidden from all users (except those with the Administrator permission).";
        this.availableInDM = false;
        //this.cooldown = 1200;
        //this.cooldownKey = (event) -> {return event.getGuild().getId()+"|"+event.getAuthor().getId();};
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_CHANNEL,
            Permission.MANAGE_ROLES
        };
        this.arguments = new Argument[]{
            new Argument("command",Argument.Type.SHORTSTRING,false)
        };
        this.children = new Command[]{
            new RoomInvite(),
            new RoomJoin(),
            new RoomKick(),
            new RoomLeave(),
            new RoomList(),
            new RoomLock(),
            new RoomRemove(),
            new RoomText(),
            new RoomTopic(),
            new RoomTTS(),
            new RoomVoice(),
            
            new RoomAssign(),
            new RoomCheck(),
            new RoomMode(),
            new RoomPermanent()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String invalidcommand = args[0]==null?null:(String)(args[0]);
        String str;
        if(invalidcommand==null)
        {
            str = "\u2139 The `room` command is used for private-channel management";
            List<String[]> textlist = rooms.getTextRoomsOnGuild(event.getGuild());
            List<String[]> voicelist = rooms.getVoiceRoomsOnGuild(event.getGuild());
            if(!textlist.isEmpty() || !voicelist.isEmpty())
                str+="\nThere are **"+textlist.size()+"** text rooms and **"+voicelist.size()+"** voice rooms on **"+event.getGuild().getName()+"**";
        }
        else if (invalidcommand.equalsIgnoreCase("create"))
        {
            str = SpConst.WARNING+"There are two types of rooms that can be made:\n"
                    + "Use `"+SpConst.PREFIX+"room text <room_name_here>` to create a private text room.\n"
                    + "Use `"+SpConst.PREFIX+"room voice <Room Name Here>` to create a temporary voice room.";
        }
        else str = SpConst.ERROR+"That is not a valid room command!";
        Sender.sendResponse(str + "\nPlease use `"+SpConst.PREFIX+"room help` for a valid list of commands", event);
        return true;
    }
    
    
    private class RoomMode extends Command
    {
        private RoomMode()
        {
            this.command = "mode";
            this.help = "sets the mode for rooms";
            this.longhelp = "This command sets the mode for the room commands. In normal mode, users can "
                    + "create both private text channels and temporary voice channels. Textonly and Voiceonly are "
                    + "for limiting to one type, and NoCreation prevents creating new rooms.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("mode",Argument.Type.SHORTSTRING,true)
            };
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String mode = ((String)args[0]).toUpperCase();
            String[] modes = new String[]{"NORMAL","TEXTONLY","VOICEONLY","NOCREATION"};
            for(String m : modes)
                if(m.equals(mode))
                {
                    settings.setSetting(event.getGuild().getId(), Settings.ROOMSETTING, mode);
                    Sender.sendResponse(SpConst.SUCCESS+"Room mode set to `"+mode+"`", event);
                    return true;
                }
            StringBuilder builder = new StringBuilder(SpConst.ERROR+"That is not a valid room mode!\nValid modes:");
            for(String m: modes)
                builder.append(" `").append(m).append("`");
            Sender.sendResponse(builder.toString(), event);
            return false;
        }
    }
    
    private class RoomText extends Command
    {
        private RoomText()
        {
            this.command = "text";
            this.help = "create a private text channel";
            this.longhelp = "This command creates a new private text channel that only the owner "
                    + "(and any users with the Administrator permission) can see. After creation, the "
                    + "owner can change the topic and settings for the channel.";
            this.availableInDM = false;
            this.cooldown = 1000;
            this.cooldownKey = (event) -> {return event.getGuild().getId()+"|"+event.getAuthor().getId()+"|roomtext";};
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("room_name",Argument.Type.LONGSTRING,true,2,100)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String roomname = (String)args[0];
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            String mode = currentSettings[Settings.ROOMSETTING];
            if((mode.equals("VOICEONLY") || mode.equals("NOCREATION")) && !PermLevel.getPermLevelForUser(event.getAuthor(),event.getGuild(), currentSettings).isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"This command cannot be used because rooms are set to `"+mode+"`", event);
                return false;
            }
            roomname = roomname.replace(" ", "_").toLowerCase();
            for(TextChannel tc : event.getGuild().getTextChannels())
                if(tc.getName().equals(roomname))
                {
                    Sender.sendResponse(SpConst.ERROR+"A room called \""+roomname+"\" already exists!", event);
                    return false;
                }
            TextChannel created;
            try{
            created = (TextChannel)event.getGuild().createTextChannel(roomname).getChannel();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"A valid text channel name must use only alphanumeric characters and contain no spaces or special characters.",event);
                return false;
            }
            //tc.getOverrideForRole(guild.getPublicRole()).getManager()
            created.createPermissionOverride(event.getGuild().getPublicRole())
                    .deny(Permission.CREATE_INSTANT_INVITE,Permission.MESSAGE_READ)
                    .update();
            created.createPermissionOverride(event.getAuthor()).grant(Permission.MESSAGE_READ,Permission.MESSAGE_MANAGE).update();
            created.getManager().setTopic("Room owner: <@"+event.getAuthor().getId()+">  \nUse `"+SpConst.PREFIX+"room leave` to leave this room.").update();
            String[] room = new String[4];
            room[Rooms.SERVERID] = event.getGuild().getId();
            room[Rooms.CHANNELID] = created.getId();
            room[Rooms.OWNERID] = event.getAuthor().getId();
            room[Rooms.LOCKED] = "false";
            rooms.set(room);
            Sender.sendResponse(SpConst.SUCCESS+"Private channel \""+created.getName()+"\" (<#"+created.getId()+">) has been created.", event);
            Sender.sendMsg(SpConst.SUCCESS+"<@"+event.getAuthor().getId()+"> has created \""+created.getName()+"\"", created);
            handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(), "\uD83D\uDCFA Text channel **"+created.getName()+
                    "** (ID:"+created.getId()+") has been created by **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+")");
            return true;
        }
    }
    
    private class RoomVoice extends Command
    {
        private RoomVoice()
        {
            this.command = "voice";
            this.help = "create a temporary voice channel";
            this.longhelp = "This command creates a temporary voice channel. It will be "
                    + "automatically deleted when there are no users left in it.";
            this.availableInDM = false;
            this.cooldown = 300;
            this.cooldownKey = (event) -> {return event.getGuild().getId()+"|"+event.getAuthor().getId()+"|roomvoice";};
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("room name",Argument.Type.LONGSTRING,true,2,100)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String roomname = (String)args[0];
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            String mode = currentSettings[Settings.ROOMSETTING];
            if((mode.equals("TEXTONLY") || mode.equals("NOCREATION")) && !PermLevel.getPermLevelForUser(event.getAuthor(),event.getGuild(), currentSettings).isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"This command cannot be used because rooms are set to `"+mode+"`", event);
                return false;
            }
            for(VoiceChannel vc : event.getGuild().getVoiceChannels())
                if(vc.getName().equalsIgnoreCase(roomname))
                {
                    Sender.sendResponse(SpConst.ERROR+"A room called \""+vc.getName()+"\" already exists!", event);
                    return false;
                }
            VoiceChannel created;
            try{
            created = (VoiceChannel)event.getGuild().createVoiceChannel(roomname).getChannel();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"The voice channel could not be created.",event);
                return false;
            }
            String[] room = new String[4];
            room[Rooms.SERVERID] = event.getGuild().getId();
            room[Rooms.CHANNELID] = created.getId();
            room[Rooms.OWNERID] = event.getAuthor().getId();
            room[Rooms.LOCKED] = "voice";
            rooms.set(room);
            Sender.sendResponse(SpConst.SUCCESS+"Temporary voice channel \""+created.getName()+"\" has been created.", event);
            handler.submitText(Feeds.Type.SERVERLOG, event.getGuild(), "\uD83C\uDF99 Voice channel **"+created.getName()+
                    "** (ID:"+created.getId()+") has been created by **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+")");
            return true;
        }
    }
    
    private class RoomRemove extends Command
    {
        private RoomRemove()
        {
            this.command = "remove";
            this.aliases = new String[]{"delete"};
            this.help = "remove a private text channel that you created";
            this.longhelp = "This command allows a user (or an Admin) to remove a private text room.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel channel = (TextChannel)(args[0]);
            if(channel==null)
                channel = event.getTextChannel();
            String[] room = rooms.get(channel.getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+channel.getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(!room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot remove a room you don't own!", event);
                return false;
            }
            if(room[Rooms.OWNERID].equals(event.getJDA().getSelfInfo().getId()))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot remove a permanent room with this command. Please check `"+SpConst.PREFIX+"room permanent help`", event);
                return false;
            }
            boolean same = channel.getId().equals(event.getTextChannel().getId());
            String name = channel.getName();
            Guild guild = event.getGuild();
            try{
                channel.getManager().delete();
            }catch(Exception e){
                Sender.sendResponse(SpConst.WARNING+"I failed to remove the room.", event);
                return false;
            }
            
            if(!same)
                Sender.sendResponse(SpConst.SUCCESS+"You have removed \""+channel.getName()+"\"", event);
            rooms.remove(channel.getId());
            handler.submitText(Feeds.Type.SERVERLOG, guild, "\uD83D\uDCFA Text channel **"+name+
                    "** (ID:"+channel.getId()+") has been removed by **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+")");
            return true;
        }
    }
    
    private class RoomList extends Command
    {
        private RoomList()
        {
            this.command = "list";
            this.help = "shows a list of permanent text channels that you can join";
            this.longhelp = "This command shows the list of private rooms that you can join. "
                    + "Locked rooms, user-created rooms, and rooms you are in are not shown.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.children = new Command[]{
                new RoomListAll()
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            StringBuilder builder = new StringBuilder("\uD83D\uDCFA Text Rooms **"+event.getAuthor().getUsername()+"** can join ( use `"+SpConst.PREFIX+"room join <roomname>` to join):");
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            List<TextChannel> list = new ArrayList<>();
            rooms.getTextRoomsOnGuild(event.getGuild()).stream().forEach((room) -> {
                TextChannel chan = event.getJDA().getTextChannelById(room[Rooms.CHANNELID]);
                if(room[Rooms.OWNERID].equals(event.getJDA().getSelfInfo().getId()))
                    if (room[Rooms.LOCKED].equalsIgnoreCase("false") || authorperm.isAtLeast(PermLevel.MODERATOR)) 
                    {
                        if (chan!=null && !PermissionUtil.checkPermission(event.getAuthor(), Permission.MESSAGE_READ, chan)) 
                        {
                            list.add(chan);
                        }
                    }
            });
            Collections.sort(list, (TextChannel a, TextChannel b) -> a.getPosition()-b.getPosition() );
            list.stream().map((chan) -> {
                builder.append("\n**").append(rooms.get(chan.getId())[Rooms.LOCKED].equalsIgnoreCase("true") ? "\uD83D\uDD12 " : "").append(chan.getName()).append("**");
                return chan;
            }).map((chan) -> chan.getTopic()).filter((topic) -> (topic!=null && !topic.startsWith("Room owner:"))).map((topic) -> FormatUtil.unembed(topic.split("\n")[0])).map((topic) -> {
                if(topic.length()>100)
                    topic = topic.substring(0,95)+" (...)";
                return topic;
            }).map((topic) -> " - "+topic).forEach((topic) -> {
                builder.append(topic);
            });
            builder.append("\nTo see the full list, use `"+SpConst.PREFIX+"room list all`");
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
        
        private class RoomListAll extends Command
        {
            private RoomListAll()
            {
                this.command = "all";
                this.aliases = new String[]{"full"};
                this.help = "shows a list of private text channels that you can join";
                this.longhelp = "This command shows the full list of private rooms that you can join. "
                        + "Locked rooms, and room you are already in are not shown.";
                this.availableInDM = false;
                this.requiredPermissions = new Permission[]{
                    Permission.MANAGE_CHANNEL,
                    Permission.MANAGE_ROLES
                };
            }
            @Override
            protected boolean execute(Object[] args, MessageReceivedEvent event) {
                StringBuilder builder = new StringBuilder("\uD83D\uDCFA Text Rooms **"+event.getAuthor().getUsername()+"** can join ( use `"+SpConst.PREFIX+"room join <roomname>` to join):");
                String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
                PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
                List<TextChannel> list = new ArrayList<>();
                rooms.getTextRoomsOnGuild(event.getGuild()).stream().forEach((room) -> {
                    TextChannel chan = event.getJDA().getTextChannelById(room[Rooms.CHANNELID]);
                    if (room[Rooms.LOCKED].equalsIgnoreCase("false") || authorperm.isAtLeast(PermLevel.MODERATOR)) {
                        if (chan!=null && !PermissionUtil.checkPermission(event.getAuthor(), Permission.MESSAGE_READ, chan)) {
                            list.add(chan);
                        }
                    }
                });
                Collections.sort(list, (TextChannel a, TextChannel b) -> a.getPosition()-b.getPosition() );
                list.stream().map((chan) -> {
                    builder.append("\n**").append(rooms.get(chan.getId())[Rooms.LOCKED].equalsIgnoreCase("true") ? "\uD83D\uDD12 " : "").append(chan.getName()).append("**");
                    return chan;
                }).map((chan) -> chan.getTopic()).filter((topic) -> (topic!=null && !topic.startsWith("Room owner:"))).map((topic) -> FormatUtil.unembed(topic.split("\n")[0])).map((topic) -> {
                    if(topic.length()>100)
                        topic = topic.substring(0,95)+" (...)";
                    return topic;
                }).map((topic) -> " - "+topic).forEach((topic) -> {
                    builder.append(topic);
                });
                Sender.sendResponse(builder.toString(), event);
                return true;
            }
        }
    }
    
    private class RoomTopic extends Command
    {
        private RoomTopic()
        {
            this.command = "topic";
            this.help = "sets the topic of a private text channel that you created";
            this.longhelp = "This command sets the topic of a private room you created.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("topic",Argument.Type.LONGSTRING,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String topic = args[0]==null ? null : (String)args[0];
            String[] room = rooms.get(event.getTextChannel().getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+event.getTextChannel().getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(!room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot set the topic for a room you don't own!", event);
                return false;
            }
            try{
                event.getTextChannel().getManager()
                    .setTopic((topic==null ? "" : topic+"  \n") + "Room owner: <@"+event.getAuthor().getId()+">  \nUse `"+SpConst.PREFIX+"room leave` to leave this room.").update();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.WARNING+"I failed to set the topic.", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"You have "+(topic==null ? "cleared" : "set")+" the topic of \""+event.getTextChannel().getName()+"\"", event);
            return true;
        }
    }
    
    private class RoomTTS extends Command
    {
        private RoomTTS()
        {
            this.command = "tts";
            this.help = "sets text-to-speech in a private text channel that you created";
            this.longhelp = "This command can be used to enable or disable text-to-speech in a private room you own";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("true|false",Argument.Type.SHORTSTRING,true)
            };
            this.cooldown = 20;
            this.cooldownKey = event -> event.getTextChannel().getId();
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String value = ((String)args[0]).toUpperCase();
            if(!value.equals("TRUE") && !value.equals("FALSE"))
            {
                Sender.sendResponse(SpConst.ERROR+"TTS setting must be `TRUE` or `FALSE`", event);
                return false;
            }
            String[] room = rooms.get(event.getTextChannel().getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+event.getTextChannel().getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(!room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot set TTS in a room you don't own!", event);
                return false;
            }
            try{
                PermissionOverride ovr = event.getTextChannel().getOverrideForRole(event.getGuild().getPublicRole());
                PermissionOverrideManager mng = ovr.getManager();
                ovr.getAllowed().stream().forEach((p) -> {
                    mng.grant(p);
                });
                ovr.getDenied().stream().forEach((p) -> {
                    mng.deny(p);
                });
                if(value.equals("TRUE"))
                    mng.grant(Permission.MESSAGE_TTS).update();
                else
                    mng.deny(Permission.MESSAGE_TTS).update();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.WARNING+"I failed to set TTS.", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"You have "+(value.equals("TRUE") ? "enabled" : "disabled")+" text-to-speech in \""+event.getTextChannel().getName()+"\"", event);
            return true;
        }
    }
    
    private class RoomKick extends Command
    {
        private RoomKick()
        {
            this.command = "kick";
            this.help = "kicks someone from a private text channel that you created";
            this.longhelp = "This command kicks a user from a private room you own. "
                    + "Once kicked, they cannot join unless invited back.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("username",Argument.Type.LOCALUSER,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            User user = (User)args[0];
            String[] room = rooms.get(event.getTextChannel().getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+event.getTextChannel().getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(!room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.MODERATOR))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot kick someone from a room you don't own!", event);
                return false;
            }
            PermLevel userperm = PermLevel.getPermLevelForUser(user, event.getGuild(), currentSettings);
            if(userperm.isAtLeast(PermLevel.MODERATOR) || user.equals(event.getJDA().getSelfInfo()) || user.equals(event.getAuthor()) || !PermissionUtil.canInteract(event.getJDA().getSelfInfo(), user, event.getGuild()))
            {
                Sender.sendResponse(SpConst.ERROR+"**"+user.getUsername()+"** cannot be kicked.", event);
                return false;
            }
            try{
                event.getTextChannel().createPermissionOverride(user).deny(Permission.MESSAGE_READ,Permission.MESSAGE_HISTORY).update();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.WARNING+"I failed to kick **"+user.getUsername()+"**", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"You have kicked **"+user.getUsername()+"** from \""+event.getTextChannel().getName()+"\"", event);
            return true;
        }
    }
    
    private class RoomLock extends Command
    {
        private RoomLock()
        {
            this.command = "lock";
            this.help = "locks a private text channel that you created";
            this.longhelp = "This command locks a private room you own. Once locked, "
                    + "no users can join themselves; they must be invited by the owner. Also "
                    + "the room will not appear in the room list.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("channel",Argument.Type.TEXTCHANNEL,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel channel = (TextChannel)(args[0]);
            if(channel==null)
                channel = event.getTextChannel();
            String[] room = rooms.get(channel.getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+channel.getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(!room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot (un)lock a room you don't own!", event);
                return false;
            }
            if(room[Rooms.OWNERID].equals(event.getJDA().getSelfInfo().getId()))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot (un)lock a permanent room", event);
                return false;
            }
            if(room[Rooms.LOCKED].equalsIgnoreCase("true"))
                room[Rooms.LOCKED] = "FALSE";
            else
                room[Rooms.LOCKED] = "TRUE";
            rooms.set(room);
            Sender.sendResponse(SpConst.SUCCESS+"You have "+(room[Rooms.LOCKED].equalsIgnoreCase("FALSE") ? "un" : "")+"locked \""+channel.getName()+"\"", event);
            return true;
        }
    }
    
    private class RoomJoin extends Command
    {
        private RoomJoin()
        {
            this.command = "join";
            this.help = "joins an unlocked a private text channel";
            this.longhelp = "This command is used to join a private room on the server.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("roomname",Argument.Type.TEXTCHANNEL,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel channel = (TextChannel)args[0];
            String[] room = rooms.get(channel.getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+channel.getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(room[Rooms.LOCKED].equalsIgnoreCase("true") && !authorperm.isAtLeast(PermLevel.MODERATOR))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot join a locked room!", event);
                return false;
            }
            if(channel.getOverrideForUser(event.getAuthor())!=null && channel.getOverrideForUser(event.getAuthor()).getDenied().contains(Permission.MESSAGE_HISTORY))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot join a room you were kicked from, you must be invited to access this room.", event);
                return false;
            }
            try{
                channel.createPermissionOverride(event.getAuthor()).grant(Permission.MESSAGE_READ).update();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.WARNING+"I failed to add you to \""+channel.getName()+"\"", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"You have joined \""+channel.getName()+"\" (<#"+channel.getId()+">)", event);
            Sender.sendMsg("Welcome, <@"+event.getAuthor().getId()+">!", channel);
            return true;
        }
    }
    
    private class RoomInvite extends Command
    {
        private RoomInvite()
        {
            this.command = "invite";
            this.help = "adds someone a private text channel";
            this.longhelp = "This command is used to invite a user to a private room. "
                    + "If the room is locked, only the owner can invite";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("username",Argument.Type.LOCALUSER,true,"to"),
                new Argument("channel",Argument.Type.TEXTCHANNEL,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            User user = (User)args[0];
            TextChannel channel = args[1]==null ? event.getTextChannel() : (TextChannel)args[1];
            String[] room = rooms.get(channel.getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+channel.getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
            PermLevel authorperm = PermLevel.getPermLevelForUser(event.getAuthor(), event.getGuild(), currentSettings);
            if(room[Rooms.LOCKED].equalsIgnoreCase("TRUE") && !room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot invite someone to a room you don't own if the room is locked!", event);
                return false;
            }
            if(!channel.getUsers().contains(event.getAuthor()) && !room[Rooms.OWNERID].equals(event.getAuthor().getId()) && !authorperm.isAtLeast(PermLevel.ADMIN))
            {
                Sender.sendResponse(SpConst.ERROR+"You must be in the room to invite someone to it!", event);
                return false;
            }
            try{
                channel.createPermissionOverride(user).grant(Permission.MESSAGE_READ,Permission.MESSAGE_HISTORY).update();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.WARNING+"I failed to add **"+user.getUsername()+"** to \""+channel.getName()+"\"", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"You have added **"+user.getUsername()+"** to \""+channel.getName()+"\"", event);
            Sender.sendMsg("Welcome, <@"+user.getId()+">!", channel);
            return true;
        }
    }
    
    private class RoomPermanent extends Command
    {
        private RoomPermanent()
        {
            this.command = "permanent";
            this.help = "creates a permanent leavable room";
            this.longhelp = "This room is used to convert a regular (non-"+SpConst.BOTNAME+" room) "
                    + "to a permanent room, or convert a "+SpConst.BOTNAME+" room to a regular room.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("true|false",Argument.Type.SHORTSTRING,true)
            };
            this.level = PermLevel.ADMIN;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String value = ((String)args[0]).toUpperCase();
            if(!value.equals("TRUE") && !value.equals("FALSE"))
            {
                Sender.sendResponse(SpConst.ERROR+"Permanent setting must be `TRUE` or `FALSE`", event);
                return false;
            }
            String[] room = rooms.get(event.getTextChannel().getId());
            
            if(value.equals("TRUE"))
            {
                if(room!=null)
                {
                    Sender.sendResponse(SpConst.ERROR+"<#"+event.getTextChannel().getId()+"> is already a "+SpConst.BOTNAME+" room! Please use `"+SpConst.PREFIX+"room permanent FALSE` first.", event);
                    return false;
                }
                String[] newroom = new String[4];
                newroom[Rooms.CHANNELID] = event.getTextChannel().getId();
                newroom[Rooms.OWNERID] = event.getJDA().getSelfInfo().getId();
                newroom[Rooms.SERVERID] = event.getGuild().getId();
                newroom[Rooms.LOCKED] = "FALSE";
                rooms.set(newroom);
                try{
                event.getTextChannel().getManager().setTopic(event.getTextChannel().getTopic()
                        +"  \nType `"+SpConst.PREFIX+"room leave` in here or mute this channel if you don't want to get notifications.").update();
                }catch(Exception e){}//set topic if we can
                Sender.sendResponse(SpConst.SUCCESS+"<#"+event.getTextChannel().getId()+"> is now a permanent "+SpConst.BOTNAME+" room.", event);
                return true;
            }
            else
            {
                if(room==null)
                {
                    Sender.sendResponse(SpConst.ERROR+"<#"+event.getTextChannel().getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                    return false;
                }
                rooms.remove(event.getTextChannel().getId());
                Sender.sendResponse(SpConst.SUCCESS+"<#"+event.getTextChannel().getId()+"> room is no longer a "+SpConst.BOTNAME+" room", event);
                return true;
            }
        }
    }
    
    private class RoomLeave extends Command
    {
        private RoomLeave()
        {
            this.command = "leave";
            this.help = "leaves a private text channel";
            this.longhelp = "This command removes you from a private or permanent "+SpConst.BOTNAME+" room.";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("roomname",Argument.Type.TEXTCHANNEL,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            TextChannel channel = args[0]==null ? event.getTextChannel() : (TextChannel)args[0];
            String[] room = rooms.get(channel.getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.ERROR+"<#"+channel.getId()+"> is not a "+SpConst.BOTNAME+" room!", event);
                return false;
            }
            if(room[Rooms.OWNERID].equals(event.getAuthor().getId()))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot leave a room you own! Use `"+SpConst.PREFIX+"room remove` to delete the room.", event);
                return false;
            }
            try{
                channel.createPermissionOverride(event.getAuthor()).deny(Permission.MESSAGE_READ).update();
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.WARNING+"I failed to add you to \""+channel.getName()+"\"", event);
                return false;
            }
            if(channel.equals(event.getTextChannel()))
            {
                event.getMessage().deleteMessage();
                Sender.sendPrivate("You have left \""+channel.getName()+"\"", event.getAuthor().getPrivateChannel());
            }
            else
            {
                Sender.sendResponse(SpConst.SUCCESS+"You have left \""+channel.getName()+"\"", event);
            }
            return true;
        }
    }
    
    private class RoomCheck extends Command
    {
        private RoomCheck()
        {
            this.command = "check";
            this.help = "checks the status of a room";
            this.longhelp = "This command checks if a room is a "+SpConst.BOTNAME+" room";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.level = PermLevel.ADMIN;
            this.hidden= true;
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] room = rooms.get(event.getTextChannel().getId());
            if(room==null)
            {
                Sender.sendResponse(SpConst.WARNING+"This is not a "+SpConst.BOTNAME+" room.", event);
                return true;
            }
            Sender.sendResponse(SpConst.SUCCESS+"This is a "+SpConst.BOTNAME+" room.\nIt's owned by <@"+room[Rooms.OWNERID]+"> (owned by "+SpConst.BOTNAME+"=permanent)", event);
            return true;
        }
    }
    
    private class RoomAssign extends Command
    {
        private RoomAssign()
        {
            this.command = "assign";
            this.help = "converts a non-"+SpConst.BOTNAME+" room to a room owned by the given user";
            this.longhelp = "This command converts a non-"+SpConst.BOTNAME+" room to a room owned by the given user";
            this.availableInDM = false;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_ROLES
            };
            this.level = PermLevel.ADMIN;
            this.hidden= true;
            this.arguments = new Argument[]{
                new Argument("user",Argument.Type.LOCALUSER,true)
            };
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            User u = (User)args[0];
            String[] room = rooms.get(event.getTextChannel().getId());
            if(room!=null)
            {
                Sender.sendResponse(SpConst.ERROR+"This is already a "+SpConst.BOTNAME+" room.", event);
                return true;
            }
            String[] newroom = new String[4];
            newroom[Rooms.CHANNELID] = event.getTextChannel().getId();
            newroom[Rooms.LOCKED] = "false";
            newroom[Rooms.SERVERID] = event.getGuild().getId();
            newroom[Rooms.OWNERID] = u.getId();
            rooms.set(newroom);
            Sender.sendResponse(SpConst.SUCCESS+"This room has been assigned to **"+u.getUsername()+"**", event);
            return true;
        }
    }
}
