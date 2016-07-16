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
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.AFKs;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AFK extends Command {
    private final AFKs afks;
    public AFK(AFKs afks)
    {
        this.afks = afks;
        this.command = "afk";
        this.help = "relays mentions via DM; can autoreply message";
        this.arguments = new Argument[]{
            new Argument("message",Argument.Type.LONGSTRING,false,0,200)
        };
        this.longhelp = "This command marks you as Away From Keyboard. While away, whenever you are directly mentioned "
                + "(and "+SpConst.BOTNAME+" can see the mention), it will relay the info to you via a Direct Message. If "
                + "you include a message when using the command, "+SpConst.BOTNAME+" will also autoreply to the mention with "
                + "your message. The next time you start typing or send a message, your AFK status will be automatically revoked.";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String message = args[0]==null ? null : (String)args[0];
        afks.set(new String[]{event.getAuthor().getId(),message});
        Sender.sendResponse(SpConst.SUCCESS+"**"+event.getAuthor().getUsername()+"** has gone AFK", event);
        return true;
    }
}
