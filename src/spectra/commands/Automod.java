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

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Automod extends Command {
    private final Settings settings;
    public Automod(Settings settings)
    {
        this.settings = settings;
        this.command = "automod";
        this.aliases = new String[]{"am","auto"};
        this.level = PermLevel.ADMIN;
        this.help = "sets automoderator actions";
        this.longhelp = "This command is used to activate and modify automoderator settings "
                + "for the current server. Please keep in mind that these features can potentially "
                + "have unwanted effects, and the features are currently __in Beta__. Use at your own risk.";
        this.children = new Command[]{
            //new AMPermRoles()
        };
    }
    
    // no cancer - unicode filled names can be automatically replaced
    // -- percentage to replace on (50-100), or 0 for off
    // no ads - links to other servers will be removed + punishment (configurable)
    // no spam - spam detection and automatic cleanup and punishment (configuable)
    // word filter - delete messages with specified words + punishment (configurable)
    // 
    // configurable ->
    // -- mute on X offenses
    // -- kick on X offenses
    // -- ban on X offenses
    // -- ignore roles
    // -- [ignore|apply] channels

    
    private class AMNoCancer extends Command
    {
        
    }
}
