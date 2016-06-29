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

import java.io.File;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.entities.Tuple;
import spectra.tempdata.CallDepend;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Sender { 
    
    //for replying to commands
    public static void sendResponse(String message, MessageReceivedEvent event)
    {
        ArrayList<String> bits = splitMessage(message);
        bits.stream().forEach((bit) -> {
            event.getChannel().sendMessageAsync(bit, m -> {
                if(event.getChannel() instanceof TextChannel)
                    CallDepend.getInstance().add(event.getMessage().getId(), m);
            });
        });
    }
    
    //reply with a file (or permission error)
    public static void sendFileResponse(Supplier<Tuple<String,File>> message, MessageReceivedEvent event)
    {
        sendFileResponseWithAlternate(message, null, event);
    }
    
    //reply with a file, or text if a file can't be sent
    public static void sendFileResponseWithAlternate(Supplier<Tuple<String,File>> message, String alternate, MessageReceivedEvent event)
    {
        if(!event.isPrivate())
        {
            if(!PermissionUtil.checkPermission(event.getTextChannel().getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES, event.getTextChannel()))
            {
                sendResponse(alternate==null ? String.format(SpConst.NEED_PERMISSION, Permission.MESSAGE_ATTACH_FILES) : alternate , event);
                return;
            }
        }
        event.getChannel().sendTyping();
        Tuple<String,File> tuple = message.get();
        String msg = tuple.getFirst();
        File file = tuple.getSecond();
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
    
    //send automatic message
    public static boolean sendFeed(String message, TextChannel tchan)
    {
        if(PermissionUtil.checkPermission(tchan.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, tchan))
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
    public static boolean sendFeedFile(String message, File file, String alternate, TextChannel tchan)
    {
        if(PermissionUtil.checkPermission(tchan.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, tchan))
        {
            ArrayList<String> bits = splitMessage(message);
            
            if(PermissionUtil.checkPermission(tchan.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES, tchan))
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
    
    private static ArrayList<String> splitMessage(String stringtoSend)
    {
        ArrayList<String> msgs =  new ArrayList<>();
        if(stringtoSend!=null)
        {
            stringtoSend = stringtoSend.replace("@everyone", "@\u180Eeveryone").replace("@here", "@\u180Ehere").trim();
            while(stringtoSend.length()>2000)
            {
                int index = stringtoSend.lastIndexOf("\n", 2000);
                if(index==-1)
                    index = stringtoSend.lastIndexOf(" ", 2000);
                if(index==-1)
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
