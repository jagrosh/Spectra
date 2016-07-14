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
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;
import spectra.Sender;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class PhoneConnections {
    ArrayList<String> ids;
    public PhoneConnections()
    {
        ids = new ArrayList<>();
    }
    
    // returns true if it is connected after this is called
    // returns false if it is still waiting for another party
    public synchronized boolean connect(TextChannel current)
    {
        ids.add(current.getId());
        if(ids.size()==1)
            return false;
        else if(ids.size()==2)
        {
            TextChannel other = current.getJDA().getTextChannelById(ids.get(0));
            if(other==null)
            {
                ids.remove(0);
                return false;
            }
            else
            {
                Sender.sendMsg(OTHER_PICKUP, other);
                return true;
            }
        }
        else
        {
            TextChannel done = current.getJDA().getTextChannelById(ids.get(0));
            if(done!=null)
            {
                Sender.sendMsg(CONNECTION_LOST, done);
                Cooldowns.getInstance().checkAndApply(done.getGuild().getId()+"|speakerphone", 120);
            }
            ids.remove(0);
            TextChannel first = current.getJDA().getTextChannelById(ids.get(0));
            if(first==null)
            {
                ids.remove(0);
                return false;
            }
            else
            {
                Sender.sendMsg(SOMEONE_ELSE, first);
                return true;
            }
        }
    }
    
    // returns true if it disconnected from a call
    // returns false if it wasn't connected when it disconnected
    public synchronized boolean disconnect(TextChannel current)
    {
        if(ids.size()==2)
        {
            TextChannel other = ids.get(0).equals(current.getId()) ? current.getJDA().getTextChannelById(ids.get(1)) : current.getJDA().getTextChannelById(ids.get(0));
            if(other!=null)
                Sender.sendMsg(THEY_HUNG_UP, other);
            ids.clear();
            return true;
        }
        ids.clear();
        return false;
    }
    
    public synchronized void endCall(JDA jda)
    {
        ids.stream().map((id) -> jda.getTextChannelById(id)).filter((chan) -> (chan!=null)).map((chan) -> {
            Sender.sendMsg(OVERLOAD, chan);
            return chan;
        }).forEach((chan) -> {
            Cooldowns.getInstance().checkAndApply(chan.getGuild().getId()+"|speakerphone", 180);
        });
        ids.clear();
    }
    
    public synchronized boolean isConnected(TextChannel current)
    {
        return ids.stream().anyMatch((id) -> (id.equals(current.getId())));
    }
    
    public synchronized TextChannel getOtherLine(TextChannel current)
    {
        if(ids.size()<2)
            return null;
        TextChannel chan;
        if(ids.get(0).equals(current.getId()))
        {
            chan = current.getJDA().getTextChannelById(ids.get(1));
        }
        else if(ids.get(1).equals(current.getId()))
        {
            chan = current.getJDA().getTextChannelById(ids.get(0));
        }
        else return null;
        if(chan==null)
        {
            ids.clear();
            Sender.sendMsg(CONNECTION_LOST, chan);
        }
        return chan;
    }
    
    public final static String PHONE = "\u260E ";
    public final static String LINE = "\uD83D\uDCDE ";
    public final static String CALLING = PHONE+"**Calling...**";
    public final static String THEY_HUNG_UP = PHONE+"**The other party hung up.**";
    public final static String YOU_HUNG_UP = PHONE+"**You hung up the conversation.**";
    public final static String NO_RESPONSE = PHONE+"**Looks like there's no response; hanging up.**";
    public final static String OTHER_PICKUP = PHONE+"**The other party has picked up!**";
    public final static String CONNECTION_MADE = PHONE+"**A connection has been made! Say hello!**";
    public final static String CONNECTION_LOST = PHONE+"**The connection has been lost...**";
    public final static String SOMEONE_ELSE = PHONE+"**Sounds like someone else picked up.**";
    public final static String OVERLOAD = PHONE+"**The connection was severed due to overload.**";
}
