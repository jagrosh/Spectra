/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.commands.*;
import spectra.datasources.*;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Spectra extends ListenerAdapter {
    
    Command[] commands;
    DataSource[] sources;
    final Settings settings;

    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //get the settings for the server
        //settings will be null for private messages
        //make default settings if no settings exist for a server
        String[] currentSettings = settings.getSettingsForGuild(event.getGuild().getId());
        if(currentSettings==null && !event.isPrivate())
            currentSettings = settings.makeNewSettingsForGuild(event.getGuild().getId());
        
        //get a sorted list of prefixes
        String[] prefixes = event.isPrivate() ?
            new String[]{SpConst.PREFIX,SpConst.ALTPREFIX} :
            Settings.prefixesFromList(currentSettings[Settings.PREFIXES]);
        
        //compare against each prefix
        String strippedMessage=null;
        for(int i=prefixes.length-1;i>=0;i--)
            if(event.getMessage().getRawContent().toLowerCase().startsWith(prefixes[i].toLowerCase()))
            {
                strippedMessage = event.getMessage().getRawContent().substring(prefixes[i].length());
                break;
            }
        
        PermLevel perm = PermLevel.EVERYONE;//start with everyone
        if(event.getAuthor().getId().equals(SpConst.JAGROSH_ID))
            perm = PermLevel.JAGROSH;
        else if(!event.isPrivate())//we're in a guild
        {
            if(PermissionUtil.checkPermission(event.getAuthor(), Permission.MANAGE_SERVER, event.getGuild()))
                perm = PermLevel.ADMIN;
            else
            {
                if(currentSettings[Settings.MODIDS].contains(event.getAuthor().getId()))
                    perm = PermLevel.MODERATOR;
                else
                {
                    for(Role r: event.getGuild().getRolesForUser(event.getAuthor()))
                        if(currentSettings[Settings.MODIDS].contains("r"+r.getId()))
                        {
                            perm = PermLevel.MODERATOR;
                            break;
                        }
                }
            }
        }
        
        boolean ignore = false;
        if(!event.isPrivate())
        {
            if( currentSettings[Settings.IGNORELIST].contains("u"+event.getAuthor().getId()) || currentSettings[Settings.IGNORELIST].contains("c"+event.getTextChannel().getId()) )
                ignore = true;
            else
                for(Role r: event.getGuild().getRolesForUser(event.getAuthor()))
                    if(currentSettings[Settings.IGNORELIST].contains("r"+r.getId()))
                    {
                        ignore = true;
                        break;
                    }
        }
        
        if(strippedMessage!=null)//potential command right here
        {
            strippedMessage = strippedMessage.trim();
            if(strippedMessage.equalsIgnoreCase("help"))//send full help message (based on access level)
            {
                String helpmsg = "**Available help "+(event.isPrivate() ? "via Direct Message" : "in <#"+event.getTextChannel().getId()+">")+"**:";
                for(Command com: commands)
                {
                    if(     (com.level == PermLevel.EVERYONE) || 
                            (com.level == PermLevel.MODERATOR && (perm==PermLevel.MODERATOR || perm==PermLevel.ADMIN || perm==PermLevel.JAGROSH)) ||
                            (com.level == PermLevel.ADMIN && (perm==PermLevel.ADMIN || perm==PermLevel.JAGROSH)) ||
                            (com.level == PermLevel.JAGROSH && perm==PermLevel.JAGROSH))
                        helpmsg += "\n`"+SpConst.PREFIX+com.command+"`"+(com.arguments == null ? "" : " `"+com.arguments+"`")+" - "+com.help;
                }
                helpmsg+="\n\nFor more information, call "+SpConst.PREFIX+"<command> help. For example, `"+SpConst.PREFIX+"tag help";
                helpmsg+="\nFor commands, `<argument>` refers to a required argument, while `[argument]` is optional";
                helpmsg+="\nDo not add <> or [] to your arguments, nor quotation marks";
                helpmsg+="\nFor more help, contact **jagrosh** (<@"+SpConst.JAGROSH_ID+">) or join "+SpConst.JAGZONE_INVITE;
                
                //SEND HELP MESSAGE HERE
            }
            else
            {
                if (strippedMessage.startsWith("help"))//send warning to try %help
                {
                    
                }
                
                
                
            }
        }
        
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        
    }
    
    
    
    
    
    
    
    public Spectra()
    {
        settings = new Settings();
    }
    
    public void init()
    {
        commands = new Command[]{
            new Ping()
        };
        
        sources = new DataSource[]{
            settings
        };
        
        for(DataSource source: sources)
            source.read();
        
        try {
            new JDABuilder().addListener(this).setBotToken(null).buildAsync();
        } catch (LoginException | IllegalArgumentException ex) {
            System.err.println("ERROR - Building JDA : "+ex.toString());
            System.exit(1);
        }
    }
    
    public static void main(String[] args)
    {
        new Spectra().init();
    }
}
