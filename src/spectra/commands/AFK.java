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
        this.help = "relays mentions to you via DM, and can autoreply if given a message";
        this.arguments = new Argument[]{
            new Argument("message",Argument.Type.LONGSTRING,false,0,200)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String message = args[0]==null ? null : (String)args[0];
        afks.set(new String[]{event.getAuthor().getId(),message});
        Sender.sendResponse(SpConst.SUCCESS+"**"+event.getAuthor().getUsername()+"** has gone AFK", event);
        return true;
    }
}
