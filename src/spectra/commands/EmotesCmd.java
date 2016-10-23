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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Emote;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.InviteUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import org.json.JSONObject;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.OtherUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class EmotesCmd extends Command {
    private final String usertoken;
    private final String userid;
    public EmotesCmd(String usertoken, String userid)
    {
        this.usertoken = usertoken;
        this.userid = userid;
        this.command = "emote";
        this.aliases = new String[]{"emotes","emoji","charinfo"};
        this.help = "views info on an emote";
        this.longhelp = "This command shows detailed information about an emote, emoji, or character.";
        this.arguments = new Argument[]{
            new Argument("emote",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new EmoteList(),
            
            new AddEmote(),
            new DeleteEmote()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String str = (String)args[0];
        if(str.matches("<:.*:\\d+>"))
        {
            String id = str.replaceAll("<:.*:(\\d+)>", "$1");
            Emote emote = event.getJDA().getEmoteById(id);
            if(emote==null)
            {
                Sender.sendResponse(SpConst.WARNING+"Unknown emote:\n"
                    +SpConst.LINESTART+"ID: **"+id+"**\n"
                    +SpConst.LINESTART+"Guild: Unknown\n"
                    +SpConst.LINESTART+"URL: https://discordcdn.com/emojis/"+id+".png",event);
                return true;
            }
            Sender.sendResponse(SpConst.SUCCESS+"Emote **"+emote.getName()+"**:\n"
                    +SpConst.LINESTART+"ID: **"+emote.getId()+"**\n"
                    +SpConst.LINESTART+"Guild: "+(emote.getGuild()==null ? "Unknown" : "**"+emote.getGuild().getName()+"**")+"\n"
                    +SpConst.LINESTART+"URL: "+emote.getImageUrl(),event);
            return true;
        }
        if(str.codePoints().count()>10)
        {
            Sender.sendResponse(SpConst.ERROR+"Invalid emote, or input is too long", event);
            return false;
        }
        StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Emoji/Character info:");
        str.codePoints().forEachOrdered(code -> {
            char[] chars = Character.toChars(code);
            String hex = Integer.toHexString(code).toUpperCase();
            while(hex.length()<4)
                hex = "0"+hex;
            builder.append("\n`\\u").append(hex).append("`   ");
            if(chars.length>1)
            {
                String hex0 = Integer.toHexString(chars[0]).toUpperCase();
                String hex1 = Integer.toHexString(chars[1]).toUpperCase();
                while(hex0.length()<4)
                    hex0 = "0"+hex0;
                while(hex1.length()<4)
                    hex1 = "0"+hex1;
                builder.append("[`\\u").append(hex0).append("\\u").append(hex1).append("`]   ");
            }
            builder.append(String.valueOf(chars)).append("   _").append(Character.getName(code)).append("_");
        });
        Sender.sendResponse(builder.toString(), event);
        return true;
    }
    
    private class EmoteList extends Command {
        private EmoteList()
        {
            this.command = "list";
            this.availableInDM = false;
            this.help = "shows the server's emotes";
            this.longhelp = "This command shows the emotes that are currently available on the server";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(event.getGuild().getEmotes().isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There are no emotes on this server!", event);
                return false;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Emotes on **"+event.getGuild().getName()+"**:\n");
            event.getGuild().getEmotes().forEach( e -> builder.append(" ").append(e.getAsEmote()));
            Sender.sendResponse(builder.toString(),event);
            return true;
        }
    }
    
    private class DeleteEmote extends Command {
        private DeleteEmote()
        {
            this.command = "remove";
            this.aliases = new String[]{"delete"};
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.help = "removes an emote";
            this.longhelp = "Removes the given emote from your server.";
            this.arguments = new Argument[]{
                new Argument("emote",Argument.Type.SHORTSTRING,true)
            };
            this.requiredPermissions = new Permission[]{Permission.CREATE_INSTANT_INVITE};
            this.hidden = true;
            this.whitelistCooldown = -1;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            Emote em = null;
            if(name.matches("<:.*:\\d+>"))
            {
                String id = name.replaceAll("<:.*:(\\d+)>", "$1");
                for(Emote e : event.getGuild().getEmotes())
                    if(e.getId().equals(id))
                    {
                        em = e;
                        break;
                    }
            }
            if(em==null)
                for(Emote e : event.getGuild().getEmotes())
                    if(e.getName().equals(name))
                    {
                        em = e;
                        break;
                    }
            if(em==null)
            {
                Sender.sendResponse(SpConst.ERROR+"That emote doesn't exist here!", event);
                return false;
            }
            if(!confirmation(event))
                return false;
            String emname = em.getName();
            int result;
            try {
            result = Unirest.delete("https://discordapp.com/api/guilds/"+event.getGuild().getId()+"/emojis/"+em.getId())
                    .header("Content-Type", "application/json")
                    .header("Authorization", usertoken)
                    .asJson().getStatus();
            } catch (UnirestException ex) {
                Sender.sendResponse(SpConst.WARNING+"Something went wrong sending the request...", event);
            return false;
            }
            if(result>=200 && result<=300)
            {
                Sender.sendResponse(SpConst.SUCCESS+"Deleted emote `"+emname+"`!", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Something went wrong, check the name", event);
                return false;
            }
        }
    }
    
    private class AddEmote extends Command {
        
        public AddEmote()
        {
            this.command = "add";
            this.aliases = new String[]{"create"};
            this.availableInDM= false;
            this.level = PermLevel.ADMIN;
            this.help = "adds an emote to the server";
            this.longhelp = "This command can add emotes for use on your server only. Note that they will not work on other servers. "
                    + "Emote names cannot contain spaces or other special characters. Please kick the Emote Provider account from your server when "
                    + "you have finished adding and removing emotes. Thank you.";
            this.arguments = new Argument[]{
                new Argument("emotename",Argument.Type.SHORTSTRING,true,2,32),
                new Argument("image_link",Argument.Type.SHORTSTRING,true)
            };
            this.requiredPermissions = new Permission[]{Permission.CREATE_INSTANT_INVITE};
            this.whitelistCooldown = -1;
            this.cooldown = 10;
            this.cooldownKey = event -> event.getGuild().getId()+"|addemote";
            this.hidden = true;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            String url = (String)args[1];
            event.getChannel().sendTyping();
            if(!confirmation(event))
                return false;
            if(!name.matches("[A-Za-z0-9_]+"))
            {
                Sender.sendResponse(SpConst.ERROR+"Invalid name. Names can only include letters, numbers, and underscores", event);
                return false;
            }
            BufferedImage img = OtherUtil.imageFromUrl(url);
            if(img==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Invalid image", event);
                return false;
            }
            BufferedImage emote = new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g2d = emote.createGraphics();
            g2d.drawImage(img, 0, 0, 128, 128, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(emote, "png", baos);
            } catch (IOException ex) {
                Sender.sendResponse(SpConst.WARNING+"Something went wrong encoding the image...", event);
                return false;
            }
            byte[] bytes = baos.toByteArray();
            String data = Base64.encode(bytes);
            int result;
            String request = new JSONObject().put("name", name).put("image", "data:image/png;base64,"+data).toString();
            //System.out.println(request);
            try {
                result = Unirest.post("https://discordapp.com/api/guilds/"+event.getGuild().getId()+"/emojis")
                        .header("Content-Type", "application/json")
                        .header("Authorization", usertoken)
                        .body(request).asJson().getStatus();
            } catch (UnirestException ex) {
                Sender.sendResponse(SpConst.WARNING+"Something went wrong sending the request...", event);
                return false;
            }
            if(result>=200 && result<=300)
            {
                Sender.sendResponse(SpConst.SUCCESS+"Added emote `"+name+"`!", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Something went wrong, check for a valid/unique name and valid image", event);
                return false;
            }
        }
    }
    
    private boolean confirmation(MessageReceivedEvent event)
    {
        String id = userid;
        if(id.startsWith(">"))
        {
            if(event.getGuild().getUserById(SpConst.JAGROSH_ID)==null)
            {
                Sender.sendResponse(SpConst.WARNING+"Sorry, this feature is currently only available for servers with the bot creator.", event);
                return false;
            }
            id = id.substring(1);
        }
        User userbot = event.getGuild().getUserById(id);
        if(userbot==null)
        {
            for(User u : event.getGuild().getManager().getBans())
                if(u.getId().equals(id))
                {
                    Sender.sendResponse(SpConst.ERROR+"Please unban the Emote Provider.", event);
                    return false;
                }
        }
        if(userbot==null)
        {
            String code = null;
            try{
                code = event.getGuild().getInvites().get(0).getCode();
            }catch(Exception e){}
            if(code==null)
                try {
                    code = InviteUtil.createInvite(event.getGuild().getPublicChannel()).getCode();
                }catch(Exception e)
                {
                    Sender.sendResponse(SpConst.ERROR+"I was unable to create an invite for the Emote Provider. "
                            + "Please make sure I can make an invite for the default channel (you can revoke the invite later)", event);
                    return false;
                }
            int status;
            try {
                status = Unirest.post("https://discordapp.com/api/v6/invite/"+code).header("Authorization", usertoken).asJson().getStatus();
            } catch (UnirestException ex) {
                status = 0;
            }
            if(status<200 || status > 400)
            {
                Sender.sendResponse(SpConst.ERROR+"Unable to invite userbot, please ask jagrosh to fix this", event);
                return false;
            }
            try{Thread.sleep(2000);}catch(Exception e){}
            Sender.sendResponse(SpConst.ERROR+"Please make sure that <@"+id+"> is in the server and has a role with Manage Emotes permission!"
                    + "\nAdditionally, please kick the account when you are finished adding/removing emotes. Thank you!", event);
            return false;
        }
        if(!PermissionUtil.checkPermission(event.getGuild(), userbot, Permission.MANAGE_EMOTES))
        {
            Sender.sendResponse(SpConst.ERROR+"Please make sure <@"+id+"> has Manage Emotes perms on the server!", event);
            return false;
        }
        return true;
    }
}
