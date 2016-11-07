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

import java.awt.Color;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Guild.VerificationLevel;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
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
public class RoleCmd extends Command {
    private final Settings settings;
    public RoleCmd(Settings settings)
    {
        this.settings = settings;
        this.command = "role";
        this.help = "role management";
        this.longhelp = "This command is for basic role management on the server";
        this.availableInDM = false;
        this.level = PermLevel.ADMIN;
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_ROLES
        };
        this.arguments = new Argument[]{
            new Argument("subcommand",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new RoleAuto(),
            new RoleColor(),
            new RoleCreate(),
            new RoleGive(),
            new RoleKeep(),
            new RoleTake()
        };
    }
    
    private class RoleAuto extends Command
    {
        private RoleAuto()
        {
            this.command = "auto";
            this.availableInDM= false;
            this.level = PermLevel.ADMIN;
            this.help = "sets a role to be automatically given when a user sends a message";
            this.longhelp = "This command is used to set a role that can automatically be given to users. "
                    + "If no phrase is assigned, the role will be given the first time they send any message "
                    + "(within 30 minutes of joining). If the server's verification is set to None, the role "
                    + "will be given right when they join. If a phrase is provided, they will be given the role "
                    + "when they type that exact phrase. Only 1 auto role can be set; any new auto role will "
                    + "override the previous.";
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true,"|"),
                new Argument("phrase",Argument.Type.LONGSTRING,false)
            };
            this.children = new Command[]{
                new RoleAutoClear()
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Role role = (Role)args[0];
            String phrase = args[1]==null ? null : (String)args[1];
            String str = role.getId()+(phrase==null ? "" : "|"+phrase);
            settings.setSetting(event.getGuild().getId(), Settings.AUTOROLE, str);
            boolean verifOff = event.getGuild().getVerificationLevel()==VerificationLevel.NONE;
            Sender.sendResponse(SpConst.SUCCESS+"I will give the role *"+role.getName()
                    +"* to users when they "+(phrase==null ? (verifOff ? "join the server." : "send any message.") : "type `"+phrase+"`"), event);
            return true;
        }
        
        private class RoleAutoClear extends Command
        {
            private RoleAutoClear()
            {
                this.command = "clear";
                this.availableInDM= false;
                this.level = PermLevel.ADMIN;
                this.help = "clears the auto-role";
                this.longhelp = "This command clears the auto-role, so that no roles are given to users when "
                        + "they say a phrase or send a message";
                this.requiredPermissions = new Permission[]{
                    Permission.MANAGE_ROLES
                };
            }

            @Override
            protected boolean execute(Object[] args, MessageReceivedEvent event) {
                settings.setSetting(event.getGuild().getId(), Settings.AUTOROLE, "");
                Sender.sendResponse(SpConst.SUCCESS+"I will not give any roles automatically.", event);
                return true;
            }
        }
    }
    
    private class RoleColor extends Command
    {
        private RoleColor()
        {
            this.command = "color";
            this.help = "sets the color of a role";
            this.longhelp = "This command sets the color of a role, using a hex or integer value.";
            this.arguments = new Argument[]{
                new Argument("color",Argument.Type.SHORTSTRING,true),
                new Argument("rolename",Argument.Type.ROLE,true)
            };
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String colorname = (String)args[0];
            Role role = (Role)args[1];
            Color color;
            try{
                color = Color.decode(colorname);
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"\""+colorname+"\" is not a valid color.", event);
                return false;
            }
            if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))
            {
                Sender.sendResponse(SpConst.ERROR+"I cannot interact with *"+role.getName()+"* due to the order of roles", event);
                return false;
            }
            try{
            role.getManager().setColor(color).update();
            }catch (Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to set the color of the role", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"I have set the color of *"+role.getName()+"* to `"+colorname.toUpperCase()+"`", event);
            return true;
        }
        
    }
    
    private class RoleCreate extends Command
    {
        private RoleCreate()
        {
            this.command = "create";
            this.help = "creates a new, blank role";
            this.longhelp = "This command creates a new, blank role of the specified name.";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("role name",Argument.Type.LONGSTRING,true,1,32)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            try{
                event.getGuild().createRole().setPermissionsRaw(0).setName(name).update();
            } catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to create the role", event);
                System.err.println("Role creation error: "+e);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"New role *"+name+"* created!", event);
            return true;
        }
    }
    
    private class RoleGive extends Command
    {
        private RoleGive()
        {
            this.command = "give";
            this.help = "gives a role to a user";
            this.longhelp = "This command gives the specified role to the specified user.";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true,"to"),
                new Argument("username",Argument.Type.LOCALUSER,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Role role = (Role)args[0];
            User user = (User)args[1];
            
            if(role.isManaged())
            {
                Sender.sendResponse(SpConst.ERROR+"*"+role.getName()+"* is a Managed Role, so I cannot give it to a user", event);
                return false;
            }
            if(event.getGuild().getRolesForUser(user).contains(role))
            {
                Sender.sendResponse(SpConst.ERROR+"**"+user.getUsername()+"** already has *"+role.getName()+"*!", event);
                return false;
            }
            if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))
            {
                Sender.sendResponse(SpConst.ERROR+"I cannot interact with *"+role.getName()+"* due to the order of roles", event);
                return false;
            }
            try{
                event.getGuild().getManager().addRoleToUser(user, role).update();
            } catch (Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to give the role", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"Role *"+role.getName()+"* given to **"+user.getUsername()+"**", event);
            return true;
        }
    }
    
    private class RoleTake extends Command
    {
        private RoleTake()
        {
            this.command = "take";
            this.help = "takes a role from a user";
            this.longhelp = "This command takes the specified role from the specified user.";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true,"from"),
                new Argument("username",Argument.Type.LOCALUSER,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Role role = (Role)args[0];
            User user = (User)args[1];
            if(!event.getGuild().getRolesForUser(user).contains(role))
            {
                Sender.sendResponse(SpConst.ERROR+"**"+user.getUsername()+"** does not have *"+role.getName()+"*!", event);
                return false;
            }
            if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))
            {
                Sender.sendResponse(SpConst.ERROR+"I cannot interact with *"+role.getName()+"* due to the order of roles", event);
                return false;
            }
            try{
                event.getGuild().getManager().removeRoleFromUser(user, role).update();
            } catch (Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to remove the role", event);
                return false;
            }
            Sender.sendResponse(SpConst.SUCCESS+"Role *"+role.getName()+"* taken from **"+user.getUsername()+"**", event);
            return true;
        }
    }
    
    private class RoleKeep extends Command 
    {
        private RoleKeep(){
            this.command = "keep";
            this.aliases = new String[]{"perma"};
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
