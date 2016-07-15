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
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Donators;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Donate extends Command {
    private final Donators donators;
    public Donate(Donators donators)
    {
        this.donators = donators;
        this.command = "donate";
        this.help = "learn how you can donate";
        this.longhelp = "This command provides information on how to donate to the author of "+SpConst.BOTNAME+", jagrosh. "
                + "It includes links for how to help him out, even if you don't want to donate money! Also, it shows the top donators.";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        StringBuilder builder = new StringBuilder("So, you'd like to learn more about how to donate to **jagrosh**? Awesome! \n"
                + "To support jagrosh, check out <https://www.patreon.com/jagrosh> \n"
                + "Also, check out jagrosh's other bots too on his bot server: https://discord.gg/0p9LSGoRLu6Pet0k \n"
                + "*If you don't want to donate money, consider:* \n"
                + "Liking his a cappella group on facebook - <https://www.facebook.com/brickcitysingers> \n"
                + "Subscribing on YouTube - <https://www.youtube.com/user/jagrosh>\n"
                + "Starring/following on github - <https://github.com/jagrosh>\n\n"
                + "__**TOP DONATORS**__\n");
        List<String[]> list = donators.donatorList();
        for(int i=0; i<10 && i<list.size(); i++)
        {
            User u = event.getJDA().getUserById(list.get(i)[Donators.USERID]);
            if(u!=null && Double.parseDouble(list.get(i)[Donators.AMOUNT])>=.01)
                builder.append("\n").append(u.getUsername());
        }
        Sender.sendResponse(builder.toString(), event);
        return true;
    }
}
