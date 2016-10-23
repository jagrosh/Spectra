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

import java.util.List;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.SimpleLog;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.web.BingImageSearcher;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ImageSearch extends Command {
    private final BingImageSearcher bingsearch;
    public ImageSearch(BingImageSearcher bingsearch)
    {
        this.bingsearch = bingsearch;
        this.command = "image";
        this.aliases = new String[]{"img","pic"};
        this.help = "searches the web for an image";
        this.longhelp = "This command searches Bing for an image matching the given query.";
        this.arguments = new Argument[]{
            new Argument("query",Argument.Type.LONGSTRING,true,1,500)
        };
        this.cooldown = 200;
        this.whitelistCooldown = 60;
        this.goldlistCooldown = 10;
        this.cooldownKey = event -> event.getAuthor().getId()+"|image";
        this.availableInDM = false;
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String query = (String)args[0];
        event.getChannel().sendTyping();
        //SimpleLog.getLog("ImageLog").info(event.getAuthor().getUsername()+" (ID:"+event.getAuthor().getId()+"): "+query);
        List<String> urls = bingsearch.search(query);
        if(urls==null)
        {
            Sender.sendResponse(SpConst.ERROR+"An error occurred while searching", event);
            return false;
        }
        if(urls.isEmpty())
        {
            Sender.sendResponse(SpConst.WARNING+"No results found for \""+query+"\"", event);
            return false;
        }
        Sender.sendResponse("<@"+event.getAuthor().getId()+"> \uD83D\uDDBC "+urls.get((int)(Math.random()*urls.size())), event);
        return true;
    }
}
