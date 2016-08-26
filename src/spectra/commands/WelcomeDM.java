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
import spectra.datasources.Guides;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class WelcomeDM extends Command {
    private final Guides guides;
    public WelcomeDM(Guides guides)
    {
        this.guides = guides;
        this.command = "welcomedm";
        this.help = "sets the direct message that gets sent to new users";
        this.longhelp = "This command sets or clears a page of the Direct Message that is "
                + "sent to users when they join the server.";
        this.availableInDM = false;
        this.level = PermLevel.ADMIN;
        this.children = new Command[]{
            new WelcomeClear(),
            new WelcomeSet()
        };
        this.arguments = new Argument[]{
            new Argument("clear|set",Argument.Type.SHORTSTRING,true),
            new Argument("pagenum",Argument.Type.INTEGER,true,1,5)
        };
    }
    
    private class WelcomeSet extends Command
    {
        private WelcomeSet()
        {
            this.command = "set";
            this.help = "sets a page of the welcome DM";
            this.longhelp = "This command sets a page of the Direct Message that is sent to new users when they join the server";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("pagenum",Argument.Type.INTEGER,true,1,5),
                new Argument("contents",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            long pagenum = (long)args[0];
            String contents = (String)args[1];
            guides.setPage(event.getGuild().getId(), (int)pagenum, contents);
            Sender.sendResponse(SpConst.SUCCESS+"You have set page **"+pagenum+"**", event);
            return true;
        }
    }
    
    private class WelcomeClear extends Command
    {
        private WelcomeClear()
        {
            this.command = "clear";
            this.help = "clears a page of the welcome DM";
            this.longhelp = "This command clears a page of the Direct Message that is sent to new users when they join the server";
            this.level = PermLevel.ADMIN;
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("pagenum",Argument.Type.INTEGER,true,1,5)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            long pagenum = (long)args[0];
            guides.setPage(event.getGuild().getId(), (int)pagenum, "");
            Sender.sendResponse(SpConst.SUCCESS+"You have cleared page "+pagenum, event);
            return true;
        }
    }
}
