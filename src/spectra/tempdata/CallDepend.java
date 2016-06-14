/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.tempdata;

import java.util.ArrayList;
import net.dv8tion.jda.entities.Message;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class CallDepend {
    
    private final ArrayList<DepMessage> messages;
    private final int maxSize;
    private static final CallDepend callDepend = new CallDepend();
    
    private CallDepend()
    {
        messages = new ArrayList<>();
        maxSize = 300;
    }
    
    public static CallDepend getInstance()
    {
        return callDepend;
    }
    
    public synchronized void add(String dependency, Message message)
    {
        messages.add(new DepMessage(dependency,message));
        while(messages.size()>maxSize)
            messages.remove(0);
    }
    
    public synchronized void delete(String dependency)
    {
        messages.stream().filter((msg) -> (msg.dependency.equals(dependency))).forEach((msg) -> {
            try{
                msg.message.deleteMessage();
            }catch(Exception e){}// ratelimits, or message already gone
            // either way, we just want to move on
        });
    }
    
    private class DepMessage
    {
        public String dependency;
        public Message message;
        public DepMessage(String dep, Message msg)
        {
            message = msg;
            dependency = dep;
        }
    }
}
