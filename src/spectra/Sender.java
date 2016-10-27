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
package spectra;

import com.mashape.unirest.http.Unirest;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.util.Pair;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Emote;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.tempdata.CallDepend;
import spectra.utils.OtherUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Sender { 
    private final static int maxMessages = 2;
    //for replying to commands
    public static void sendResponse(String message, MessageReceivedEvent event)
    {
        List<String> bits = splitMessage(message);
        if(bits.size() > maxMessages)
        {
            if(event.isPrivate() || PermissionUtil.checkPermission(event.getTextChannel(), event.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES))
            {
                event.getChannel().sendTyping();
                String header = bits.get(0);
                header = header.split("\n",2)[0];
                if(header.length()>100)
                {
                    header = header.substring(0,100);
                    int index = header.lastIndexOf(" ");
                    if(index!=-1)
                        header = header.substring(0,index);
                }
                File f = OtherUtil.writeArchive(message, "Result");
                event.getChannel().sendFileAsync(f, new MessageBuilder().appendString(header).build(), m -> {
                    CallDepend.getInstance().add(event.getMessage().getId(), m);
                });
                return;
            }
            bits = bits.subList(0, maxMessages);
            String lastBit = bits.get(maxMessages-1);
            if(lastBit.length()>(2000-30))
                lastBit = lastBit.substring(0,1970);
            lastBit+="(...message too long!)";
            bits.set(maxMessages-1, lastBit);
        }
        bits.stream().forEach((bit) -> {
            event.getChannel().sendMessageAsync(bit, m -> {
                if(!event.isPrivate())
                    CallDepend.getInstance().add(event.getMessage().getId(), m);
            });
        });
    }
    
    //reply with a file (or permission error)
    public static void sendFileResponse(Supplier<Pair<String,File>> message, MessageReceivedEvent event)
    {
        sendFileResponseWithAlternate(message, null, event);
    }
    
    //reply with a file, or text if a file can't be sent
    public static void sendFileResponseWithAlternate(Supplier<Pair<String,File>> message, String alternate, MessageReceivedEvent event)
    {
        if(!event.isPrivate())
        {
            if(!PermissionUtil.checkPermission(event.getTextChannel(),event.getTextChannel().getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES))
            {
                sendResponse(alternate==null ? String.format(SpConst.NEED_PERMISSION, Permission.MESSAGE_ATTACH_FILES) : alternate , event);
                return;
            }
        }
        event.getChannel().sendTyping();
        Pair<String,File> tuple = message.get();
        String msg = tuple.getKey();
        File file = tuple.getValue();
        event.getChannel().sendFileAsync(file, msg==null ? null : new MessageBuilder().appendString(msg.length() > 2000 ? msg.substring(0, 2000) : msg).build(), m -> {
                if(!event.isPrivate())
                    CallDepend.getInstance().add(event.getMessage().getId(), m);
            });
    }
    
    //send help (warn if can't send)
    public static void sendHelp(String message, PrivateChannel pchan, MessageReceivedEvent event)//dependency for fallback
    {
        ArrayList<String> bits = splitMessage(message);
        for(int i=0; i<bits.size(); i++)
        {
            boolean first = (i == 0);
            pchan.sendMessageAsync(bits.get(i), m ->
            {
                if(m==null && first && !event.isPrivate())//failed to send
                {
                    sendResponse(SpConst.CANT_HELP, event);
                }
            });
        }
    }
    
    //send private message, silent fail
    public static void sendPrivate(String message, PrivateChannel pchan)
    {
        ArrayList<String> bits = splitMessage(message);
        bits.stream().forEach((bit) -> {
            pchan.sendMessageAsync(bit, null);
        });
    }
    
    //send automatic message with callback
    public static boolean sendMsg(String message, TextChannel tchan, Consumer<Message> callback)
    {
        if(PermissionUtil.checkPermission(tchan, tchan.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
        {
            ArrayList<String> bits = splitMessage(message);
            for(int i=0; i<bits.size(); i++)
                tchan.sendMessageAsync(bits.get(i), i+1==bits.size() ? callback : null);
            return true;
        }
        return false;
    }
    
    //send automatic message
    public static boolean sendMsg(String message, TextChannel tchan)
    {
        if(PermissionUtil.checkPermission(tchan, tchan.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
        {
            ArrayList<String> bits = splitMessage(message);
            bits.stream().forEach((bit) -> {
                tchan.sendMessageAsync(bit, null);
            });
            return true;
        }
        return false;
    }
    
    //send automatic message with file
    public static boolean sendMsgFile(String message, File file, String alternate, TextChannel tchan)
    {
        if(PermissionUtil.checkPermission(tchan, tchan.getJDA().getSelfInfo(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
        {
            ArrayList<String> bits = splitMessage(message);
            
            if(PermissionUtil.checkPermission(tchan, tchan.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES))
            {
                tchan.sendFileAsync(file, bits.isEmpty() ? null : new MessageBuilder().appendString(bits.get(0)).build(), null);
            }
            else
            {
                bits.stream().forEach((bit) -> {
                    tchan.sendMessageAsync(bit, null);
                });
            }
            return true;
        }
        return false;
    }
    
    //send reaction
    public static void sendReaction(Message message, String encodedEmoji)
    {
        try{
            Unirest.put("https://canary.discordapp.com/api/v6/channels/"+message.getChannelId()+"/messages/"+message.getId()+"/reactions/"+encodedEmoji+"/@me")
                    .header("Authorization", message.getJDA().getAuthToken())
                    .asJsonAsync();
        }catch(Exception e){}
    }
    
    public static void sendReaction(Message message, Emote emote)
    {
        try{
            Unirest.put("https://canary.discordapp.com/api/v6/channels/"+message.getChannelId()+"/messages/"+message.getId()+"/reactions/"+emote.getName()+":"+emote.getId()+"/@me")
                    .header("Authorization", message.getJDA().getAuthToken())
                    .asJsonAsync();
        }catch(Exception e){}
    }
    
    private static ArrayList<String> splitMessage(String stringtoSend)
    {
        ArrayList<String> msgs =  new ArrayList<>();
        if(stringtoSend!=null)
        {
            stringtoSend = stringtoSend.replace("@everyone", "@\u200Beveryone").replace("@here", "@\u200Bhere").trim();
            while(stringtoSend.length()>2000)
            {
                int leeway = 2000 - (stringtoSend.length()%2000);
                int index = stringtoSend.lastIndexOf("\n", 2000);
                if(index<leeway)
                    index = stringtoSend.lastIndexOf(" ", 2000);
                if(index<leeway)
                    index=2000;
                String temp = stringtoSend.substring(0,index).trim();
                if(!temp.equals(""))
                    msgs.add(temp);
                stringtoSend = stringtoSend.substring(index).trim();
            }
            if(!stringtoSend.equals(""))
                msgs.add(stringtoSend);
        }
        return msgs;
    }
    
}
