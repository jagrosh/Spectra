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
package spectra.tempdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.entities.Message;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class MessageCache {
    private final HashMap<String,List<Message>> messages;
    private final int perguildcap;
    
    public MessageCache()
    {
        messages = new HashMap<>();
        perguildcap = 1000;
    }
    /*
        adds a message to the message cache
    */
    public void addMessage(String guildid, Message message)
    {
        synchronized(messages)
        {
            //String guildid = message.getJDA().getTextChannelById(message.getChannelId()).getGuild().getId();
            if(messages.get(guildid)==null)
            {
                ArrayList<Message> list = new ArrayList<>();
                list.add(message);
                messages.put(guildid,list);
            }
            else
            {
                messages.get(guildid).add(message);
                while(messages.get(guildid).size()>perguildcap)
                    messages.get(guildid).remove(0);
            }
        }
    }
    
    /*
        updates a message in the message cache, returning the old message
    */
    public Message updateMessage(String guildid, Message message)
    {
        synchronized(messages)
        {
            if(messages.get(guildid)==null)
            {
                ArrayList<Message> list = new ArrayList<>();
                list.add(message);
                messages.put(guildid,list);
                return null;
            }
            else
            {
                Message old = null;
                for(Message msg : messages.get(guildid))
                    if(msg.getId().equals(message.getId()))
                    {
                        old = msg;
                        break;
                    }
                if(old!=null)
                    messages.get(guildid).remove(old);
                messages.get(guildid).add(message);
                while(messages.get(guildid).size()>perguildcap)
                    messages.get(guildid).remove(0);
                return old;
            }
        }
    }
    
    /*
        deletes a message in the message cache, returning the old message
    */
    public Message deleteMessage(String guildid, String messageid)
    {
        synchronized(messages)
        {
            if(messages.get(guildid)==null)
            {
                return null;
            }
            else
            {
                Message toRemove = null;
                for(Message msg : messages.get(guildid))
                    if(msg.getId().equals(messageid))
                    {
                        toRemove = msg;
                        break;
                    }
                if(toRemove!=null)
                    messages.get(guildid).remove(toRemove);
                return toRemove;
            }
        }
    }
    
}
