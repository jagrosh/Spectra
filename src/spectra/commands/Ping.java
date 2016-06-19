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

import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.SpConst;
import spectra.tempdata.CallDepend;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Ping extends Command{
    
    public Ping(){
    this.command = "ping";
    this.aliases = new String[]{"pang","peng","pong","pung"};
    this.help = "check the bot's latency";
    this.longhelp = "This command checks the difference in time between when discord "
            + "recieves the command and when discord receives "+SpConst.BOTNAME+"'s "
            + "response. The response is then edited to reflect the difference.";
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String vowel = "aeiou".charAt((int)(Math.random()*5))+"";
        event.getChannel().sendMessageAsync("P"+vowel+"ng: ...", m -> {
            if(m!=null)
            {
                m.updateMessageAsync("P"+vowel+"ng: "+event.getMessage().getTime().until(m.getTime(), ChronoUnit.MILLIS)+"ms", m2 -> {
                    CallDepend.getInstance().add(event.getMessage().getId(), m2);
                });
                
            }
        });
        return true;
    }
    
}
