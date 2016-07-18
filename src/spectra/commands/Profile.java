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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Profiles;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Profile extends Command {
    
    private final Profiles profiles;
    public Profile(Profiles profiles)
    {
        this.profiles = profiles;
        this.command = "profile";
        this.aliases = new String[]{"p"};
        this.help = "displays or edits a profile";
        this.longhelp = "This command is used to keep track of user profiles, with information for various "
                + "games and services. Profiles are maintains across all servers.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,false)
        };
        
        this.children = new Command[profiles.getSize()];
        children[0] = new ProfileClear();
        ArrayList<String> fields = new ArrayList<>(Arrays.asList(Profiles.profCall));
        fields.remove(0);
        Collections.sort(fields);
        for(int i=0; i<fields.size(); i++)
            children[i+1] = new ProfileField(fields.get(i));
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User user = args[0]==null ? event.getAuthor() : (User)args[0];
        String[] profile = profiles.get(user.getId());
        if(profile==null)
        {
            String response = SpConst.WARNING+"No profile found for **"+user.getUsername()+"**.";
            if(user.equals(event.getAuthor()))
                response+=" Set a profile field to create a profile. Use `"+SpConst.PREFIX+"profile help` for more information.";
            Sender.sendResponse(response, event);
            return false;
        }
        Sender.sendResponse("\uD83D\uDCD6 Profile for **"+user.getUsername()+"** #"+user.getDiscriminator()+":"+Profiles.contructProfile(profile), event);
        return true;
    }
    
    private class ProfileClear extends Command
    {
        private ProfileClear()
        {
            this.command = "clear";
            this.help = "clears a field in the profile";
            this.longhelp = "Thic command clears the specified field in the profile.";
            this.arguments = new Argument[]{
                new Argument("field",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String field = (String)args[0];
            int index = Profiles.indexForField(field);
            if(index==-1)
            {
                Sender.sendResponse(SpConst.WARNING+"That is not a valid profile field!", event);
                return false;
            }
            String[] profile = profiles.get(event.getAuthor().getId());
            if(profile==null)
            {
                Sender.sendResponse(SpConst.WARNING+"You cannot clear a profile field when you do not have a profile", event);
                return false;
            }
            profile[index] =null;
            profiles.set(profile);
            Sender.sendResponse(SpConst.SUCCESS+"Your *"+field.toLowerCase()+"* field has been cleared.", event);
            return true;
        }
        
    }
    
    private class ProfileField extends Command
    {
        private final String longname;
        private ProfileField(String fieldname)
        {
            this.command = fieldname;
            longname = Profiles.profName[Profiles.indexForField(fieldname)];
            this.help = "sets your "+longname;
            this.longhelp = "This command sets the "+longname+" field in the profile.";
            this.arguments = new Argument[]{
                new Argument("info",Argument.Type.LONGSTRING,true)
            };
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] profile = profiles.get(event.getAuthor().getId());
            String contents = (String)args[0];
            String response = "";
            if(profile==null)
            {
                response = "\uD83D\uDCD6 New profile created for **"+event.getAuthor().getUsername()+"** #"+event.getAuthor().getDiscriminator()+"\n";
                profile = new String[profiles.getSize()];
                profile[0] = event.getAuthor().getId();
            }
            profile[Profiles.indexForField(command)] = contents;
            profiles.set(profile);
            response+=SpConst.SUCCESS+"Your *"+longname+"* has been set.";
            Sender.sendResponse(response, event);
            return true;
        }
    }
}
