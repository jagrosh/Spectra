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
            new AMPermRoles()
        };
    }
    
    private class AMPermRoles extends Command 
    {
        private AMPermRoles(){
            this.command = "keeproles";
            this.aliases = new String[]{"permaroles"};
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.help = "sets if users keep roles when leaving and returning";
            this.longhelp = "This command is to set the role permanence setting on the server. "
                    + "This means that if a user leaves the server, and returns, any roles they had "
                    + "before they left (excluding any roles including or above "+SpConst.BOTNAME+"'s highest) "
                    + "will be reapplied. Note that if a user is off the server for more than a week, "
                    + "the roles will \"expire\" and no longer be applied.";
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("true|false",Argument.Type.SHORTSTRING,true)
            };
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String choice = (String)args[0];
            if(choice.equalsIgnoreCase("true"))
            {
                settings.setSetting(event.getGuild().getId(), Settings.KEEPROLES, "true");
                Sender.sendResponse(SpConst.SUCCESS+"Users will now retain roles if they leave and return", event);
                return true;
            }
            else if (choice.equalsIgnoreCase("false"))
            {
                settings.setSetting(event.getGuild().getId(), Settings.KEEPROLES, "false");
                Sender.sendResponse(SpConst.SUCCESS+"No role actions on a user's return will be taken", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Valid options are `true` and `false`", event);
                return false;
            }
        }
    }
}
