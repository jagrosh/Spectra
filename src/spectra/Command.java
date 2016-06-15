package spectra;

import java.util.List;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.utils.FinderUtil;
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
    protected Argument[] arguments = new Argument[0];
    protected String longhelp = "There is no help information available for this command.\n"
            + "Please contact jagrosh if you see this";
    protected Command[] children = new Command[0];
    protected PermLevel level = PermLevel.EVERYONE;
    protected boolean availableInDM = true;
    protected int cooldown = 0; //seconds
    protected String separatorRegex = null;
    
    protected abstract boolean execute(Object[] args, MessageReceivedEvent event);
    
    public boolean run(String args, MessageReceivedEvent event, String[] settings, PermLevel perm, boolean ignore)
    {
        return run(args, event, settings, perm, ignore, "");
    }
    
    public boolean run(String args, MessageReceivedEvent event, String[] settings, PermLevel perm, boolean ignore, String parentChain)
    {
        if("help".equalsIgnoreCase(args))//display help text if applicable
        {
            StringBuilder builder = new StringBuilder();
            builder.append("**Available help for `").append(parentChain).append(command).append("` ").append(event.isPrivate() ? "via Direct Message" : "in <#"+event.getTextChannel().getId()+">").append("**:\n");
            builder.append("Usage: `" + SpConst.PREFIX).append(parentChain).append(command).append("`").append(Argument.arrayToString(arguments));
            
            if(aliases.length>0)
            {
                builder.append("\nAliases:");
                for(String alias: aliases)
                    builder.append(" `").append(alias).append("`");
            }
            builder.append("\n*").append(longhelp).append("*\n");
            if(children.length>0)
            {
                builder.append("\n**Subcommands**:");
                for(Command child: children)
                    builder.append("\n`" + SpConst.PREFIX).append(parentChain).append(command).append(" ").append(child.command).append("`").append(Argument.arrayToString(child.arguments)).append(" - ").append(child.help);
            }
            Sender.sendHelp(builder.toString(), event.getAuthor().getPrivateChannel(), event.getTextChannel(), event.getMessage().getId()); 
            return true;
        }
        
        if(args!=null)//run child command if possible
        {
            String[] argv = FormatUtil.cleanSplit(args);
            for(Command child: children)
                if(child.isCommandFor(argv[0]))
                    return child.run(argv[1], event, settings, perm, ignore, parentChain+command+" ");
        }
        
        if(!availableInDM && event.isPrivate())//can't use in dm
        {
            Sender.sendPrivate(SpConst.NOT_VIA_DM, event.getPrivateChannel());
            return false;
        }
        if(!perm.isAtLeast(level))//not enough permission
            return false;
        if(ignore && (perm==PermLevel.EVERYONE || perm==PermLevel.MODERATOR))//ignore commands by non-admins
            return false;
        if(!event.isPrivate() && !PermissionUtil.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, event.getTextChannel()))
        {
            Sender.sendPrivate(String.format(SpConst.CANT_SEND, event.getTextChannel().getAsMention()), event.getAuthor().getPrivateChannel());
            return false;
        }
        
        //parse arguments
        Object[] parsedArgs = new Object[arguments.length];
        String workingSet = args;
        for(int i=0; i<arguments.length; i++)
        {
            if(workingSet==null)
            {
                if (arguments[i].required)
                {
                    Sender.sendResponse(String.format(SpConst.TOO_FEW_ARGS,parentChain+command), event.getChannel(), event.getMessage().getId());
                    return false;
                }
                else continue;
            }
            switch(arguments[i].type)
            {
                case INTEGER:{
                    String[] parts = FormatUtil.cleanSplit(workingSet);
                    int num;
                    boolean invalid = false;
                    try{
                        num = Integer.parseInt(parts[0]);
                        if(num < arguments[i].min || num > arguments[i].max)
                        {
                            Sender.sendResponse(String.format(SpConst.INVALID_INTEGER, arguments[i].name, arguments[i].min, arguments[i].max), event.getChannel(), event.getMessage().getId());
                            return false;
                        }
                    } catch(NumberFormatException e)
                    {
                        Sender.sendResponse(String.format(SpConst.INVALID_INTEGER, arguments[i].name, arguments[i].min, arguments[i].max), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    parsedArgs[i] = num;
                    workingSet = parts[1];
                    break;}
                case SHORTSTRING:{
                    String[] parts = FormatUtil.cleanSplit(workingSet);
                    parsedArgs[i] = parts[0];
                    workingSet = parts[1];
                    break;}
                case LONGSTRING:{
                    String[] parts;
                    if(separatorRegex==null)
                        parts = new String[]{workingSet,null};
                    else
                        parts = FormatUtil.cleanSplit(workingSet, separatorRegex);
                    parsedArgs[i] = parts[0];
                    workingSet = parts[1];
                    break;}
                case TIME:{
                    break;}
                case USER:{
                    String[] parts;
                    if(separatorRegex==null)
                        parts = new String[]{workingSet,null};
                    else
                        parts = FormatUtil.cleanSplit(workingSet, separatorRegex);
                    List<User> ulist = null;
                    if(!event.isPrivate())
                        ulist = FinderUtil.findUsers(parts[0], event.getGuild());
                    if(ulist==null || ulist.isEmpty())
                        ulist = FinderUtil.findUsers(parts[0], event.getJDA().getUsers());
                    if(ulist.isEmpty())
                    {
                        Sender.sendResponse(String.format(SpConst.NONE_FOUND, "users", parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    else if (ulist.size()>1)
                    {
                        Sender.sendResponse(FormatUtil.listOfUsers(ulist, parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    parsedArgs[i] = ulist.get(0);
                    workingSet = parts[1];
                    break;}
                case LOCALUSER:{
                    if(event.isPrivate())
                    {
                        Sender.sendResponse(String.format(SpConst.INVALID_IN_DM, arguments[i].name), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    String[] parts;
                    if(separatorRegex==null)
                        parts = new String[]{workingSet,null};
                    else
                        parts = FormatUtil.cleanSplit(workingSet, separatorRegex);
                    List<User> ulist = FinderUtil.findUsers(parts[0], event.getGuild());
                    if(ulist.isEmpty())
                    {
                        Sender.sendResponse(String.format(SpConst.NONE_FOUND, "users", parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    else if (ulist.size()>1)
                    {
                        Sender.sendResponse(FormatUtil.listOfUsers(ulist, parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    parsedArgs[i] = ulist.get(0);
                    workingSet = parts[1];
                    break;}
                case TEXTCHANNEL:{
                    if(event.isPrivate())
                    {
                        Sender.sendResponse(String.format(SpConst.INVALID_IN_DM, arguments[i].name), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    String[] parts = FormatUtil.cleanSplit(workingSet);
                    List<TextChannel> tclist = FinderUtil.findTextChannel(parts[0], event.getGuild().getTextChannels());
                    if(tclist.isEmpty())
                    {
                        Sender.sendResponse(String.format(SpConst.NONE_FOUND, "text channels", parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    else if (tclist.size()>1)
                    {
                        Sender.sendResponse(FormatUtil.listOfChannels(tclist, parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    parsedArgs[i] = tclist.get(0);
                    workingSet = parts[1];
                    break;}
                case ROLE:{
                    if(event.isPrivate())
                    {
                        Sender.sendResponse(String.format(SpConst.INVALID_IN_DM, arguments[i].name), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    String[] parts;
                    if(separatorRegex==null)
                        parts = new String[]{workingSet,null};
                    else
                        parts = FormatUtil.cleanSplit(workingSet,separatorRegex);
                    List<Role> rlist = FinderUtil.findRole(parts[0], event.getGuild().getRoles());
                    if(rlist.isEmpty())
                    {
                        Sender.sendResponse(String.format(SpConst.NONE_FOUND, "roles", parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    else if (rlist.size()>1)
                    {
                        Sender.sendResponse(FormatUtil.listOfRoles(rlist, parts[0]), event.getChannel(), event.getMessage().getId());
                        return false;
                    }
                    parsedArgs[i] = rlist.get(0);
                    workingSet = parts[1];
                    break;}
            }
        }
        return execute(parsedArgs,event);
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
