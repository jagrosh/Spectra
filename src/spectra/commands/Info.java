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
package spectra.commands;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.entities.Game;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.MiscUtil;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.misc.SafeEmote;

/**
 *
 * @author johna
 */
public class Info extends Command{

    public Info()
    {
        this.command = "info";
        this.aliases = new String[]{"i","userinfo"};
        this.help = "gets information about a given user";
        this.longhelp = "This command provides basic information about the given user, "
                + "or the caller of the command if no user is provided. If used within a "
                + "guild, and the given user is in the guild, additional information about "
                + "the user in the guild will also be provided.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.USER,false)
        };
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User user = (User)(args[0]);
        if(user==null)
            user = event.getAuthor();
        
        boolean here = (!event.isPrivate() && event.getGuild().getUsers().contains(user));
        String bot = SafeEmote.BOT.get(event.getJDA());
        String str = (user.isBot()?bot:"\uD83D\uDC64")+" Information about **"+user.getUsername()+"** #"+user.getDiscriminator()
                +":\n"+SpConst.LINESTART+"Discord ID: **"+user.getId()+"**";
        if(here)
        {
            String nick = event.getGuild().getNicknameForUser(user);
            if(nick!=null)
                str+="\n"+SpConst.LINESTART+"Nickname: **"+nick+"**";
            String roles="";
            for(Role rol: event.getGuild().getRolesForUser(user)){
                String r = rol.getName();
                if(!r.equalsIgnoreCase("@everyone"))
                    roles+=", "+r;}
            if(roles.equals(""))
                roles="None";
            else
                roles=roles.substring(2);
            str+="\n"+SpConst.LINESTART+"Roles: "+roles;
        }
        str+="\n"+SpConst.LINESTART+"Status: **"+user.getOnlineStatus().name()+"**";
        Game game = user.getCurrentGame();
        if(game!=null)
            str+=" ("+(game.getType()==Game.GameType.TWITCH ? "Streaming" : "Playing")+" *"+game+"*)";
        str+="\n"+SpConst.LINESTART+"Account Creation: **"+MiscUtil.getDateTimeString(MiscUtil.getCreationTime(user))+"**";
        
        if(here)
        {
            List<User> joins = new ArrayList<>(event.getGuild().getUsers());
            Collections.sort(joins, (User a, User b) -> event.getGuild().getJoinDateForUser(a).compareTo(event.getGuild().getJoinDateForUser(b)));
            int index = joins.indexOf(user);
            str+="\n"+SpConst.LINESTART+"Guild Join Date: **"+event.getGuild().getJoinDateForUser(user).format(DateTimeFormatter.RFC_1123_DATE_TIME) + "** `(#"+(index+1)+")`";
            index-=3;
            if(index<0)
                index=0;
            str+="\n"+SpConst.LINESTART+"Join Order: ";
            if(joins.get(index).equals(user))
                str+="**"+joins.get(index).getUsername()+"**";
            else
                str+=joins.get(index).getUsername();
            for(int i=index+1;i<index+7;i++)
            {
                if(i>=joins.size())
                    break;
                User u = joins.get(i);
                String name = u.getUsername();
                if(u.equals(user))
                    name="**"+name+"**";
                str+=" > "+name;
            }
        }
        String url = (user.getId().equals("1") ? "https://discordapp.com/assets/f78426a064bc9dd24847519259bc42af.png" : user.getAvatarUrl());
        if(url!=null)
            str+="\n"+SpConst.LINESTART+"Avatar: "+url;
        
        Sender.sendResponse(str, event);
        return true;
    }
    
}
