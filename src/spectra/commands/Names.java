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

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.SavedNames;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Names extends Command {
    private final SavedNames savednames;
    public Names(SavedNames savednames)
    {
        this.savednames = savednames;
        this.command = "names";
        this.aliases = new String[]{"pastnames","namehistory"};
        this.help = "shows some previous names for a user";
        this.longhelp = "This command displays a user's (or your, if no user is provided) "
                + "previous usernames. These are only tracked during the time "+SpConst.BOTNAME+" "
                + "can see the user, and nicknames are not tracked.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.USER,false)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User user = (User)(args[0]);
        if(user==null)
            user = event.getAuthor();
        String names = savednames.getNames(user.getId());
        if(names==null)
            Sender.sendResponse(SpConst.WARNING+"No previous names recorded for **"+user.getUsername()+"**!", event);
        else
            Sender.sendResponse(FormatUtil.demention(SpConst.SUCCESS+"Previous names for **"+user.getUsername()+"**:\n"+names), event);
        return true;
    }
}
