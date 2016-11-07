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
package spectra.jagtag.libraries;

import jagtag.Method;
import jagtag.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import spectra.SpConst;
import spectra.utils.FinderUtil;
import spectra.utils.FormatUtil;

/**
 * This library is for Discord-related methods, using JDA
 * User -> "user"
 * TextChannel -> "channel"
 * 
 * @author John Grosh (jagrosh)
 */
public class Discord {
    
    public static Collection<Method> getMethods(){
        return Arrays.asList(
            // the username of the current user, or of the searched user
            new Method("user", (env) -> {
                User u = env.get("user");
                return u.getUsername();
            }, (env,in) -> {
                if(in[0].equals(""))
                    return "";
                List<User> users = null;
                TextChannel tc = env.get("channel");
                Guild g = tc.getGuild();
                User u = env.get("user");
                if(g!=null)
                    users = FinderUtil.findUsers(in[0], g);
                if(users==null || users.isEmpty())
                    users = FinderUtil.findUsers(in[0], u.getJDA());
                if(users.isEmpty())
                    throw new ParseException(String.format(SpConst.NONE_FOUND, "users", in[0]));
                if(users.size()>1)
                    throw new ParseException(FormatUtil.listOfUsers(users, in[0]));
                return users.get(0).getUsername();
            }),
                
            // the nickname of the current user, or of the searched user
            new Method("nick", (env) -> {
                User u = env.get("user");
                TextChannel tc = env.get("channel");
                Guild g = tc.getGuild();
                if(g==null)
                    return u.getUsername();
                String nickname = g.getNicknameForUser(u);
                return nickname==null ? u.getUsername() : nickname;
            }, (env,in) -> {
                if(in[0].equals(""))
                    return "";
                List<User> users;
                TextChannel tc = env.get("channel");
                Guild g = tc.getGuild();
                User u = env.get("user");
                if(g==null)
                    users = FinderUtil.findUsers(in[0], u.getJDA());
                else
                    users = FinderUtil.findUsers(in[0], g);
                if(users.isEmpty())
                    throw new ParseException(String.format(SpConst.NONE_FOUND, "users", in[0]));
                if(users.size()>1)
                    throw new ParseException(FormatUtil.listOfUsers(users, in[0]));
                if(g==null)
                    return users.get(0).getUsername();
                String nickname = g.getNicknameForUser(users.get(0));
                return nickname==null ? users.get(0).getUsername() : nickname;
            }),
            
            // the discrim of the current user, or of the searched user
            new Method("discrim", (env) -> {
                User u = env.get("user");
                return u.getDiscriminator();
            }, (env,in) -> {
                if(in[0].equals(""))
                    return "";
                List<User> users = null;
                TextChannel tc = env.get("channel");
                Guild g = tc.getGuild();
                User u = env.get("user");
                if(g!=null)
                    users = FinderUtil.findUsers(in[0], g);
                if(users==null || users.isEmpty())
                    users = FinderUtil.findUsers(in[0], u.getJDA());
                if(users.isEmpty())
                    throw new ParseException(String.format(SpConst.NONE_FOUND, "users", in[0]));
                if(users.size()>1)
                    throw new ParseException(FormatUtil.listOfUsers(users, in[0]));
                return users.get(0).getDiscriminator();
            }),
            
            // the id of the current user
            new Method("userid", (env) -> {
                User u = env.get("user");
                return u.getId();
            }),
            
            // a mention of the current user
            new Method("atuser", (env) -> {
                User u = env.get("user");
                return u.getAsMention();
            }),
            
            // the avatar of the current user
            new Method("avatar", (env) -> {
                User u = env.get("user");
                return u.getAvatarUrl()==null ? "" : u.getAvatarUrl();
            }),
                
            // the name of the current server (guild)
            new Method("server", (env) -> {
                TextChannel tc = env.get("channel");
                return tc==null ? "Direct Message" : tc.getGuild().getName();
            }),
            
            // the id of the current server (guild)
            new Method("serverid", (env) -> {
                TextChannel tc = env.get("channel");
                return tc==null ? "0" : tc.getGuild().getId();
            }),
            
            // the user count of the current server (guild)
            new Method("servercount", (env) -> {
                TextChannel tc = env.get("channel");
                return tc==null ? "1" : Integer.toString(tc.getGuild().getUsers().size());
            }),
            
            // the name of the current channel
            new Method("channel", (env) -> {
                TextChannel tc = env.get("channel");
                return tc==null ? "direct_message" : tc.getName();
            }),
            
            // the id of the current channel
            new Method("channelid", (env) -> {
                TextChannel tc = env.get("channel");
                return tc==null ? "0" : tc.getId();
            }),
            
            // a random user
            new Method("randuser", (env) -> {
                TextChannel tc = env.get("channel");
                if(tc!=null)
                    return tc.getGuild().getUsers().get((int)(tc.getGuild().getUsers().size()*Math.random())).getUsername();
                User u = env.get("user");
                return u.getJDA().getUsers().get((int)(u.getJDA().getUsers().size()*Math.random())).getUsername();
            }),
            
            // a random online user
            new Method("randonline", (env) -> {
                TextChannel tc = env.get("channel");
                User u = env.get("user");
                if(tc==null)
                    return u.getUsername();
                List<User> online = tc.getGuild().getUsers().stream().filter(us -> us.getOnlineStatus()==OnlineStatus.ONLINE).collect(Collectors.toList());
                if(online.isEmpty())
                    return u.getUsername();
                return online.get((int)(online.size()*Math.random())).getUsername();
            }),
            
            // a random channel
            new Method("randchannel", (env) -> {
                TextChannel tc = env.get("channel");
                return tc==null ? "sometextchannel" : tc.getGuild().getTextChannels().get((int)(tc.getGuild().getTextChannels().size()*Math.random())).getName();
            }),
                
            // marks jagtag as nsfw
            new Method("nsfw", (env) -> "")
        );
    }

}
