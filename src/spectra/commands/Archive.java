/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.commands;

import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Archive extends Command{

    public Archive()
    {
        this.command = "archive";
        this.help = "saves the previous posts, and returns an archive";
        this.longhelp = "This command is used to save all of the currently-visible text"
                + " from the current chat, format it, and upload it for easy viewing and/or"
                + " saving. This command requires a number of posts to archive (up to 1000), and"
                + " optionally a channel to archive in. If you do not include a channel name, the"
                + " archive will be of the current channel. You may only create archives where you"
                + " can see the channel contents.";
        this.arguments= new Argument[]{
            new Argument("numposts",Argument.Type.INTEGER,true,1,1000), 
            new Argument("channel",Argument.Type.TEXTCHANNEL,false)};//<numposts> [channel]
        this.cooldown=120;
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        int numposts = (int)(args[0]);
        TextChannel channel = (TextChannel)(args[1]);
        
        if(event.isPrivate())
        {
            MessageHistory mh = new MessageHistory(event.getPrivateChannel());
            
        }
        else
        {
            if(channel == null)
                channel = event.getTextChannel();
            //check permission of user
            
            //check permission of bot
            
            MessageHistory mh = new MessageHistory(channel);
        }
    }
    
}
