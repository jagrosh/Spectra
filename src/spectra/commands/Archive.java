/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.commands;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.FormatUtil;
import spectra.utils.OtherUtil;

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
        MessageHistory mh;
        if(event.isPrivate())
        {
            mh = new MessageHistory(event.getPrivateChannel());
            
        }
        else
        {
            if(channel == null)
                channel = event.getTextChannel();
            //check permission of user
            if(!PermissionUtil.checkPermission(event.getAuthor(), Permission.MESSAGE_HISTORY, channel) || !PermissionUtil.checkPermission(event.getAuthor(), Permission.MESSAGE_READ, channel))
            {
                Sender.sendResponse(SpConst.ERROR+"You can only archive channels in which you can see the Message History!",event.getChannel(),event.getMessage().getId());
                return false;
            }
            //check permission of bot
            if(!PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_HISTORY, channel))
            {
                Sender.sendResponse(String.format(SpConst.NEED_PERMISSION, Permission.MESSAGE_HISTORY), event.getChannel(), event.getMessage().getId());
                return false;
            }
            mh = new MessageHistory(channel);
        }
        List<Message> messages = mh.retrieve(numposts);
        StringBuilder builder = new StringBuilder("--Archive--\n");
        for(int i=messages.size()-1;i>=0;i--)
        {
        Message m = messages.get(i);
        builder.append("[").append(m.getTime()==null ? "UNKNOWN TIME" : m.getTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("] ");
        builder.append( m.getAuthor() == null ? "????" : m.getAuthor().getUsername() ).append(" : ");
        builder.append(m.getContent()).append("\n\n");
        }
        
        String message = SpConst.SUCCESS+"Archive of the past "+messages.size()+" messages:";
        File file = OtherUtil.writeArchive(builder.toString(), "archive "+event.getMessage().getTime().format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(":", ""));
        Sender.sendFileResponse(message, file, event.getChannel(), event.getMessage().getId());
        return true;
    }
    
}
