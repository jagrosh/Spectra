/*
 * Copyright 2016 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spectra.commands;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.GlobalLists;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Authorize extends Command {
    private final GlobalLists lists;
    private final String WIKI = "<https://github.com/jagrosh/Spectra/wiki/Getting-Started>";
    private final String NEED_AUTH = "Hello **%s**#%s. To authorize "+SpConst.BOTNAME+" for your server, "
            + "please follow the instructions here: "+WIKI
            + "\nServer ID: %s\nUser ID: %s";
    
    public Authorize(GlobalLists lists)
    {
        this.lists = lists;
        this.command = "authorize";
        this.help = "authorizes the bot to run on your server";
        this.longhelp = "";
        this.availableInDM = false;
        this.level = PermLevel.ADMIN;
        this.arguments = new Argument[]{
            new Argument("authcode",Argument.Type.SHORTSTRING,false)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        //check if already authorized
        if(lists.isAuthorized(event.getGuild().getId()))
        {
            Sender.sendResponse(SpConst.WARNING+"This server is already "+(lists.isWhitelisted(event.getGuild().getId()) ? "whitelisted" : "authorized")+"!", event);
            return false;
        }
        String code = args[0]==null ? null : (String)args[0];
        String truecode = event.getGuild().getId().substring(event.getGuild().getId().length()-1)
                + event.getAuthor().getId().substring(event.getAuthor().getId().length()-1)
                + event.getGuild().getVoiceChannels().size()
                + event.getAuthor().getDiscriminator().substring(3)
                + "5"
                ;
        
        if(code==null)
        {
            Sender.sendResponse(String.format(NEED_AUTH, event.getAuthor().getUsername(),event.getAuthor().getDiscriminator(),event.getGuild().getId(),event.getAuthor().getId()), event);
            return false;
        }
        else if (!truecode.equals(code))
        {
            Sender.sendResponse(SpConst.ERROR+"Invalid authorization code.\n\n"+String.format(NEED_AUTH, event.getAuthor().getUsername(),event.getAuthor().getDiscriminator(),event.getGuild().getId(),event.getAuthor().getId()), event);
            return false;
        }
        else //authorize
        {
            lists.authorize(event.getGuild().getId(), event.getAuthor().getId()+"|"+event.getMessage().getTime());
            Sender.sendResponse(SpConst.SUCCESS+"Congratulations! **"+event.getGuild().getName()+"** has been authorized. Please continue to read "+WIKI+" for additional information and setup.", event);
            return true;
        }
    }
    
}
