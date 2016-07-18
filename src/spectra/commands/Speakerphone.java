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
import spectra.Command;
import spectra.Sender;
import spectra.tempdata.PhoneConnections;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Speakerphone extends Command {
    private final PhoneConnections phones;
    public Speakerphone(PhoneConnections phones)
    {
        this.phones = phones;
        this.command = "speakerphone";
        this.help = "picks up the phone";
        this.longhelp = "This command (upon connection) joins the current text "
                + "channel to another text channel where the command is used. This could be "
                + "anywhere, so you never know who you might talk to! To hang up, use the command again.";
        this.availableInDM = false;
        this.cooldown = 0;
        this.cooldownKey = event -> event.getTextChannel().getId()+"|speakerphone";
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        if(phones.isConnected(event.getTextChannel()))
        {
            if(phones.disconnect(event.getTextChannel()))
                Sender.sendResponse(PhoneConnections.YOU_HUNG_UP, event);
            else
                Sender.sendResponse(PhoneConnections.NO_RESPONSE, event);
        }
        else
        {
            if(phones.connect(event.getTextChannel()))
                Sender.sendResponse(PhoneConnections.CONNECTION_MADE, event);
            else
                Sender.sendResponse(PhoneConnections.CALLING, event);
        }
        return true;
    }
}
