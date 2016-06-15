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
import java.util.Objects;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.tempdata.CallDepend;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Sender { 
    
    //for replying to commands
    public static void sendResponse(String message, MessageChannel chan, String dependency)
    {
        Objects.requireNonNull(dependency);
        ArrayList<String> bits = splitMessage(message);
        bits.stream().forEach((bit) -> {
            chan.sendMessageAsync(bit, m -> {
                if(chan instanceof TextChannel)
                    CallDepend.getInstance().add(dependency, m);
            });
        });
    }
    
    //reply with a file (or permission error)
    public static void sendFileResponse(String message, File file, MessageChannel chan, String dependency)
    {
        sendFileResponseWithAlternate(message, file, null, chan, dependency);
    }
    
    //reply with a file, or text if a file can't be sent
    public static void sendFileResponseWithAlternate(String message, File file, String alternate, MessageChannel chan, String dependency)
    {
        Objects.requireNonNull(dependency);
        if(chan instanceof TextChannel)
        {
            if(!PermissionUtil.checkPermission(((TextChannel)chan).getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES, (TextChannel)chan))
            {
                sendResponse(alternate==null ? String.format(SpConst.NEED_PERMISSION, Permission.MESSAGE_ATTACH_FILES) : alternate , chan, dependency);
                return;
            }
        }
        chan.sendFileAsync(file, new MessageBuilder().appendString((message.length() > 2000 ? message.substring(0, 2000) : message)).build(), m -> {
                if(chan instanceof TextChannel)
                    CallDepend.getInstance().add(dependency, m);
            });
    }
    
    //send help (warn if can't send)
    public static void sendHelp(String message, PrivateChannel pchan, TextChannel fallback, String dependency)//dependency for fallback
    {
        ArrayList<String> bits = splitMessage(message);
        for(int i=0; i<bits.size(); i++)
        {
            boolean first = (i == 0);
            pchan.sendMessageAsync(bits.get(i), m ->
            {
                if(m==null && first && fallback!=null)//failed to send
                {
                    sendResponse(SpConst.CANT_HELP, fallback, dependency);
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
