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
import spectra.datasources.AutomodFilters;
import spectra.datasources.AutomodOffenses;
import spectra.datasources.AutomodSettings;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Automod extends Command {
    private final Settings settings;
    private final AutomodSettings amsettings;
    private final AutomodOffenses amoffenses;
    private final AutomodFilters amfilters;
    public Automod(Settings settings, AutomodSettings amsettings, AutomodOffenses amoffenses, AutomodFilters amfilters)
    {
        this.settings = settings;
        this.amsettings = amsettings;
        this.amoffenses = amoffenses;
        this.amfilters = amfilters;
        this.command = "automod";
        this.aliases = new String[]{"am","auto"};
        this.level = PermLevel.ADMIN;
        this.availableInDM = false;
        this.help = "sets automoderator actions";
        this.longhelp = "This command is used to activate and modify automoderator settings "
                + "for the current server. Please keep in mind that these features can potentially "
                + "have unwanted effects, and the features are currently __in Beta__. Use at your own risk.";
        this.goldlistCooldown = -1; //gold-list only
        this.children = new Command[]{
            new AMSettings(),
        };
    }
    
    // no cancer - unicode filled names can be automatically replaced
    // -- percentage to replace on (50-100), or 0 for off
    // no ads - links to other servers will be removed + punishment (configurable)
    // no spam - spam detection and automatic cleanup and punishment (configuable)
    // word filter - delete messages with specified words + punishment (configurable)

    
    private class AMSettings extends Command
    {
        private AMSettings()
        {
            this.command = "settings";
            this.aliases = new String[]{"s","set"};
            this.availableInDM = false;
            this.goldlistCooldown = -1;
            this.level = PermLevel.ADMIN;
            this.help = "sets server-wide automod settings";
            this.longhelp = "This command sets settings that apply to all filters and actions taken by the automoderator.";
            this.children = new Command[]{
                new AMSetting("ban","sets the number of strikes to ban at",
                        "This command sets the number of strikes at which "+SpConst.BOTNAME+" will automatically ban the user",
                        AutomodSettings.KICKAT,"ban users at **%s** strikes"),
                new AMSetting("dropoff","sets the number of minutes it takes to drop a strike",
                        "This command sets the number of minutes it takes for a single strike against a user to be removed",
                        AutomodSettings.KICKAT,"remove a strike from all users (with strikes) every **%s** minutes"),
                new AMSetting("kick","sets the number of strikes to kick at",
                        "This command sets the number of strikes at which "+SpConst.BOTNAME+" will automatically kick the user",
                        AutomodSettings.KICKAT,"kick users at **%s** strikes"),
                new AMSetting("mute","sets the number of strikes to mute at",
                        "This command sets the number of strikes at which "+SpConst.BOTNAME+" will automatically mute the user for the pre-defined number of minutes",
                        AutomodSettings.KICKAT,"mute users at **%s** strikes"),
                new AMSetting("mutetime","sets the number of minutes a mute lasts",
                        "This command sets the number of minutes an automatic mute lasts",
                        AutomodSettings.KICKAT,"mute users for **%s** minutes when automatically muted"),
                new AMSetting("warn","sets the number of strikes to warn at",
                        "This command sets the number of strikes at which "+SpConst.BOTNAME+" will automatically send a warning to the user",
                        AutomodSettings.KICKAT,"warn users at **%s** strikes"),
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String out = SpConst.SUCCESS+"AutoMod settings on **"+event.getGuild().getName()+"**:"
                    + "\nStarting Strikes (at join): **"+amsettings.getSetting(event.getGuild(), AutomodSettings.STARTSTRIKES)+"**"
                    + "\nMinutes per Strike dropoff: **"+amsettings.getSetting(event.getGuild(), AutomodSettings.STRIKEDROPOFF)+"**"
                    + "\nStrikes to Warn: **"+amsettings.getSetting(event.getGuild(), AutomodSettings.WARNAT)+"**"
                    + "\nStrikes to Mute: **"+amsettings.getSetting(event.getGuild(), AutomodSettings.MUTEAT)+"**"
                    + "\nMute Minutes: **"+amsettings.getSetting(event.getGuild(), AutomodSettings.MUTEMINUTES)+"**"
                    + "\nStrikes to Kick: **"+amsettings.getSetting(event.getGuild(), AutomodSettings.KICKAT)+"**"
                    + "\nStrikes to Ban: **"+amsettings.getSetting(event.getGuild(), AutomodSettings.BANAT)+"**"
                    ;
            Sender.sendResponse(out, event);
            return true;
        }
        
        private class AMSetting extends Command
        {
            private final int position;
            private final String action;
            private AMSetting(String command, String help, String longhelp, int position, String action)
            {
                this.command = command;
                this.availableInDM = false;
                this.goldlistCooldown = -1;
                this.level = PermLevel.ADMIN;
                this.help = help;
                this.longhelp = longhelp;
                this.position = position;
                this.action = action;
                this.arguments = new Argument[]{
                    new Argument("value",Argument.Type.INTEGER,true,0,5000)
                };
            }

            @Override
            protected boolean execute(Object[] args, MessageReceivedEvent event) {
                String value = (String)args[0];
                amsettings.setSetting(event.getGuild(), position, value);
                Sender.sendResponse(SpConst.SUCCESS+SpConst.BOTNAME+" will now automatically "+String.format(action, value), event);
                return true;
            }
            
        }
    }
    
    private class AMNoAds extends Command
    {
        private AMNoAds()
        {
            this.command = "antiad";
            this.aliases = new String[]{"noads","antiads","anti-ad"};
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.help = "edits the anti-advertisement filter";
            this.longhelp = "This command is used to view or edit the anti-advertisement filter, that removes server invites.";
            this.goldlistCooldown = -1;
            this.children = new Command[]{
                
            };
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] filter = amfilters.getFilter(event.getGuild().getId(), AutomodFilters.FilterType.INVITE);
            if(filter==null)
            {
                Sender.sendResponse(SpConst.WARNING+"The anti-advertisement filter is not enabled on **"+event.getGuild().getName()+"**!", event);
                return false;
            }
            String out = SpConst.SUCCESS+"Anti-Advertisement filter settings for **"+event.getGuild().getName()+"**:"
                    + "";
            return false;
        }
    }
}
