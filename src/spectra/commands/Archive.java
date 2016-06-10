/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.commands;

import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
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
        this.arguments="<numposts> [channel]";
        this.cooldown=120;
    }
    
    @Override
    protected boolean execute(String args, MessageReceivedEvent event) {
        if(args==null)
        {
            Sender.sendResponse(SpConst.ARGUMENT_ERROR_+"Please include a number of posts, and optionally a channel", event.getChannel(), event.getMessage().getId());
            return false;
        }
        String[] argv = FormatUtil.cleanSplit(args);//int, and optional channel
        int numPosts=-1;
        try{
            numPosts = Integer.parseInt(argv[0]);
        } catch (NumberFormatException e) {}
        if(numPosts<1 || numPosts > 1000)
        {
            Sender.sendResponse(SpConst.ARGUMENT_ERROR_+"Please enter a valid number of posts (1 to 1000)", event.getChannel(), event.getMessage().getId());
            return false;
        }
        TextChannel target = event.getTextChannel();
        if(argv[1]!=null)//channel search
        {
            if(event.isPrivate())
            {
                Sender.sendResponse(SpConst.WARNING+" You cannot archive a different channel from a Direct Message!", event.getChannel(), event.getMessage().getId());
                return false;
            }
            
        }
        
    }
    
}
