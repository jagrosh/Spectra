/*
 * Copyright 2016 johna.
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

/**
 *
 * @author johna
 */
public class Info extends Command{

    public Info()
    {
        this.command = "info";
        this.aliases = new String[]{"i","userinfo"};
        this.help = "gets information about a given user";
        this.longhelp = "This command provides basic information about the given user, "
                + "or the caller of the command if no user is provided. If used within a "
                + "guild, and the given user is in the guild, additional information about "
                + "the user in the guild will also be provided.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.USER,false)
        };
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User u = (User)(args[0]);
        if(u==null)
            u = event.getAuthor();
    }
    
}
