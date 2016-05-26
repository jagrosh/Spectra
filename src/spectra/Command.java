package spectra;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
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
    protected int cooldown = 0; //seconds
    
    protected abstract boolean execute(String args, MessageReceivedEvent event);
    
    public boolean run(String args, MessageReceivedEvent event, String[] settings, PermLevel perm, boolean ignore)
    {
        if("help".equalsIgnoreCase(args))
        {
            String text = "**Available help for `"+command+"` "+(event.isPrivate() ? "via Direct Message" : "in <#"+event.getTextChannel().getId()+">")+"**:\n";
            text += "Usage: `"+SpConst.PREFIX + command +"`"+(arguments==null ? "" : " `"+arguments+"`");
            text += "\n*"+ longhelp+"*\n";
            if(children.length>0)
            {
                text += "\n**Subcommands**:";
                for(Command child: children)
                    text+="\n`"+SpConst.PREFIX+command+" "+child.command+"`"+(child.arguments==null ? "" : " `"+child.arguments+"`")+" - "+child.help;
            }
            return true;
        }
        if(args!=null)
        {
            String[] argv = FormatUtil.cleanSplit(args);
            for(Command child: children)
                if(child.isCommandFor(argv[0]))
                    return child.run(args, event, settings, perm, ignore);
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
