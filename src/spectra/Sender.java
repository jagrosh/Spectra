/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

import java.util.ArrayList;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import spectra.tempdata.CallDepend;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Sender { 
    
    
    
    public static void sendResponse(String message, MessageChannel chan, String dependency)
    {
        ArrayList<String> bits = splitMessage(message);
        for(String bit: bits)
            chan.sendMessageAsync(bit, m -> {
                CallDepend.getInstance().add(dependency, m);
            });
    }
    
    public static void sendPrivate(String message, PrivateChannel pchan, TextChannel fallback, String dependency)//dependency for fallback
    {
        ArrayList<String> bits = splitMessage(message);
        for(int i=0; i<bits.size(); i++)
        {
            boolean first = (i == 0);
            pchan.sendMessageAsync(bits.get(i), m ->
            {
                if(m==null && first && fallback!=null)//failed to send
                {
                    fallback.sendMessageAsync(SpConst.CANT_HELP, m2 -> {
                        if(m2 != null)
                            CallDepend.getInstance().add(dependency, m2);
                    });
                }
            });
        }
    }
    
    public static void sendPrivate(String message, PrivateChannel pchan)
    {
        ArrayList<String> bits = splitMessage(message);
        for(String bit: bits)
            pchan.sendMessageAsync(bit, null);
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
