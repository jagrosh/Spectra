/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

import java.util.Arrays;
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
import spectra.utils.OtherUtil;
import spectra.utils.FormatUtil;

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
        String[] currentSettings = (event.isPrivate() ? null : settings.getSettingsForGuild(event.getGuild().getId()));
        if(currentSettings==null && !event.isPrivate())
            currentSettings = settings.makeNewSettingsForGuild(event.getGuild().getId());
        
        //get a sorted list of prefixes
        String[] prefixes = event.isPrivate() ?
            new String[]{SpConst.PREFIX,SpConst.ALTPREFIX} :
            Settings.prefixesFromList(currentSettings[Settings.PREFIXES]);
        
        //compare against each prefix
        String strippedMessage=null;
        for(int i=prefixes.length-1;i>=0;i--)
        {
            if(event.getMessage().getRawContent().toLowerCase().startsWith(prefixes[i].toLowerCase()))
            {
                strippedMessage = event.getMessage().getRawContent().substring(prefixes[i].length()).trim();
                break; 
            }
        }
        //find permission level
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
        
        //check if should ignore
        boolean ignore = false;
        if(!event.isPrivate())
        {
            if( currentSettings[Settings.IGNORELIST].contains("u"+event.getAuthor().getId()) || currentSettings[Settings.IGNORELIST].contains("c"+event.getTextChannel().getId()) )
                ignore = true;
            else if(currentSettings[Settings.IGNORELIST].contains("r"+event.getGuild().getId()) && event.getGuild().getRolesForUser(event.getAuthor()).isEmpty())
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
            if(strippedMessage.toLowerCase().startsWith("help"))//send full help message (based on access level)
            {//we don't worry about ignores for help
                String helpmsg = "**Available help "+(event.isPrivate() ? "via Direct Message" : "in <#"+event.getTextChannel().getId()+">")+"**:";
                for(Command com: commands)
                {
                    if( perm.isAtLeast(com.level) )
                        helpmsg += "\n`"+SpConst.PREFIX+com.command+"`"+Argument.arrayToString(com.arguments)+" - "+com.help;
                }
                helpmsg+="\n\nFor more information, call "+SpConst.PREFIX+"<command> help. For example, `"+SpConst.PREFIX+"tag help`";
                helpmsg+="\nFor commands, `<argument>` refers to a required argument, while `[argument]` is optional";
                helpmsg+="\nDo not add <> or [] to your arguments, nor quotation marks";
                helpmsg+="\nFor more help, contact **@jagrosh** (<@"+SpConst.JAGROSH_ID+">) or join "+SpConst.JAGZONE_INVITE;
                Sender.sendPrivate(helpmsg, event.getAuthor().getPrivateChannel(), event.getTextChannel(), event.getMessage().getId());
            }
            else//didn't start with help
            {
                Command toRun = null;
                String[] args = FormatUtil.cleanSplit(strippedMessage);
                for(Command com: commands)
                    if(com.isCommandFor(args[0]))
                    {
                        toRun = com;
                        break;
                    }
                if(toRun!=null)
                {
                    boolean success = toRun.run(args[1], event, currentSettings, perm, ignore);
                }
            }
        }
        
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        
    }
    
    
    
    
    
    
    
    public Spectra()
    {
        settings = Settings.getInstance();
    }
    
    public void init()
    {
        commands = new Command[]{
            new About(),
            new Archive(),
            new Info(),
            new Ping()
        };
        
        sources = new DataSource[]{
            settings
        };
        
        for(DataSource source: sources)
            source.read();
        
        try {
            new JDABuilder().addListener(this).setBotToken(OtherUtil.readFile("discordbot.login").get(1)).buildAsync();
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
