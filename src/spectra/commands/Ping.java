/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
            + "response. The response is then editted to reflect the difference.";
    }
    
    @Override
    protected boolean execute(String args, MessageReceivedEvent event) {
        String vowel = "aeiou".charAt((int)(Math.random()*5))+"";
        event.getChannel().sendMessageAsync("P"+vowel+"ng: ...", m -> {
            if(m!=null)
            {
                m.updateMessageAsync("P"+vowel+"ng: "+event.getMessage().getTime().until(m.getTime(), ChronoUnit.MILLIS)+"ms", null);
                CallDepend.getInstance().add(event.getMessage().getId(), m);
            }
        });
        return true;
    }
    
}
