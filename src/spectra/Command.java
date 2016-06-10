package spectra;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.utils.FormatUtil;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author John Grosh (jagrosh)
 */
public abstract class Command {
    protected String command = "null";
    protected String[] aliases = new String[0];
    protected String help = "No help information given.";
    protected String arguments = null;
    protected String longhelp = "There is no help information available for this command.\n"
            + "Please contact jagrosh if you see this";
    protected Command[] children = new Command[0];
    protected PermLevel level = PermLevel.EVERYONE;
    protected boolean availableInDM = true;
    protected int cooldown = 0; //seconds
    
    protected abstract boolean execute(String args, MessageReceivedEvent event);
    
    public boolean run(String args, MessageReceivedEvent event, String[] settings, PermLevel perm, boolean ignore)
    {
        return run(args, event, settings, perm, ignore, "");
    }
    
    public boolean run(String args, MessageReceivedEvent event, String[] settings, PermLevel perm, boolean ignore, String parentChain)
    {
        if("help".equalsIgnoreCase(args))
        {
            String text = "**Available help for `"+parentChain+command+"` "+(event.isPrivate() ? "via Direct Message" : "in <#"+event.getTextChannel().getId()+">")+"**:\n";
            text += "Usage: `"+SpConst.PREFIX + parentChain + command +"`"+(arguments==null ? "" : " `"+arguments+"`");
            if(aliases.length>0)
            {
                text += "\nAliases:";
                for(String alias: aliases)
                    text+=" `"+alias+"`";
            }
            text += "\n*"+ longhelp+"*\n";
            if(children.length>0)
            {
                text += "\n**Subcommands**:";
                for(Command child: children)
                    text+="\n`"+SpConst.PREFIX+parentChain+command+" "+child.command+"`"+(child.arguments==null ? "" : " `"+child.arguments+"`")+" - "+child.help;
            }
            Sender.sendPrivate(text, event.getAuthor().getPrivateChannel(), event.getTextChannel(), event.getMessage().getId()); 
            return true;
        }
        if(args!=null)//run child command if possible
        {
            String[] argv = FormatUtil.cleanSplit(args);
            for(Command child: children)
                if(child.isCommandFor(argv[0]))
                    return child.run(args, event, settings, perm, ignore, parentChain+command+" ");
        }
        if(!availableInDM && event.isPrivate())
            Sender.sendPrivate(SpConst.NOT_VIA_DM, event.getPrivateChannel());
        if(level==PermLevel.JAGROSH && perm!=PermLevel.JAGROSH)
            return false;
        if(level==PermLevel.ADMIN && (perm!=PermLevel.ADMIN && perm!=PermLevel.JAGROSH))
            return false;
        if(level==PermLevel.MODERATOR && perm==PermLevel.EVERYONE)
            return false;
        if(ignore && (perm==PermLevel.EVERYONE || perm==PermLevel.MODERATOR))
            return false;
        if(!event.isPrivate() && !PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, event.getTextChannel()))
        {
            Sender.sendPrivate(SpConst.CANT_SEND_+event.getTextChannel().getAsMention(), event.getAuthor().getPrivateChannel());
            return false;
        }
        return execute(args,event);
    }

    public boolean isCommandFor(String string)
    {
        if(command.equalsIgnoreCase(string))
            return true;
        for(String alias : aliases)
            if(alias.equalsIgnoreCase(string))
                return true;
        return false;
    }

}
