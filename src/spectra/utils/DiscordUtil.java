/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.utils;

import java.io.File;
import java.util.ArrayList;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.exceptions.BlockedException;
import net.dv8tion.jda.exceptions.PermissionException;
import spectra.ComboMessage;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class DiscordUtil {
    
    private void sendWrapper(MessageChannel mc, ComboMessage cm, String dependencyid, TextChannel fallback)
    {
        if(item==null)
            return;
        if(debug)
            System.out.println("[CHAN]: "+mc.toString()+"\n[SENT]: "+item.toString());
        
        String stringtoSend = null;
        File filetoSend = null;
        String altstring = null;
        if(item instanceof String)
            stringtoSend = ((String)item).replace("@everyone", "@\u180Eeveryone").replace("@here", "@\u180Ehere").trim();
        if(item instanceof File)
            filetoSend = (File)item;
        if(item instanceof ComboMessage)
        {
            stringtoSend = ((ComboMessage)item).message.replace("@everyone", "@\u180Eeveryone").replace("@here", "@\u180Ehere").trim();
            filetoSend = ((ComboMessage)item).file;
            altstring = ((ComboMessage)item).alternate;
        }
        String str = stringtoSend;
        ArrayList<String> msgs =  new ArrayList<>();
        if(stringtoSend!=null)
        {
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
        
        try{
        if(filetoSend!=null)
        {
            try{
                queuedmsgs++;
            mc.sendFileAsync(filetoSend, msgs.size()>0?new MessageBuilder().appendString(msgs.get(0)).build():null, m -> {
                if(dependencyid!=null){
                    cmdhistory.add(new DepMessage(dependencyid,m));
                    if(cmdhistory.size()>200)
                        cmdhistory.remove(0);}
                queuedmsgs--;
                    });
            }catch(PermissionException pe){
                if(altstring==null)
                    altstring = NEED_PERMISSION+Permission.MESSAGE_ATTACH_FILES;
                if(altstring.length()>2000)
                    altstring=altstring.substring(0,2000);
                queuedmsgs++;
                mc.sendMessageAsync(altstring,  m -> {
                if(dependencyid!=null){
                    cmdhistory.add(new DepMessage(dependencyid,m));
                        if(cmdhistory.size()>200)
                        cmdhistory.remove(0);}
                queuedmsgs--;
                    });
            }
        }
        else
        {
            //msgs.stream().forEach((string) -> 
            for(int i=0;i<msgs.size();i++)
            {
                String string = msgs.get(i);
                boolean first = (i==0);
                queuedmsgs++;
                mc.sendMessageAsync(string,  m -> {
                    if(m==null)
                    {
                        if(fallback!=null && first)
                        {
                        queuedmsgs++;
                        fallback.sendMessageAsync(WARNING+" Help could not be sent because you are blocking Direct Messages.", m2-> {
                        if(dependencyid!=null){
                            cmdhistory.add(new DepMessage(dependencyid,m2));
                            if(cmdhistory.size()>200)
                                cmdhistory.remove(0);}
                        queuedmsgs--;
                            });
                        }
                    }
                    else
                    {
                    if(dependencyid!=null){
                        cmdhistory.add(new DepMessage(dependencyid,m));
                        if(cmdhistory.size()>200)
                            cmdhistory.remove(0);}
                    }
                    queuedmsgs--;
                    });
            }//);
        }
        }catch(PermissionException pex){
            if(mc instanceof TextChannel)
            {
                TextChannel tc = (TextChannel)mc;
                System.out.println("[PERM FAIL] "+tc.getGuild().getName()+" - #"+tc.getName()+": "+str);
            }
            else
                System.out.println("Failed to send a message due to permissions.");
        }catch (BlockedException bex){
            if(fallback!=null)
            {
                queuedmsgs++;
                fallback.sendMessageAsync(WARNING+" Help could not be sent because you are blocking Direct Messages.", m-> {
                    if(dependencyid!=null){
                        cmdhistory.add(new DepMessage(dependencyid,m));
                        if(cmdhistory.size()>200)
                            cmdhistory.remove(0);}
                    queuedmsgs--;
                        });
            }
        }
    }

    
}
