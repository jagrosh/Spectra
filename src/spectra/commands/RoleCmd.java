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
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class RoleCmd extends Command {
    
    public RoleCmd()
    {
        this.command = "role";
        this.help = "role management";
        this.availableInDM = false;
        this.level = PermLevel.ADMIN;
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_ROLES
        };
        this.arguments = new Argument[]{
            new Argument("color|create|give|take",Argument.Type.SHORTSTRING,true)
        };
        this.children = new Command[]{
            new RoleColor(),
            new RoleCreate(),
            new RoleGive(),
            new RoleTake()
        };
    }
    
    private class RoleColor extends Command
    {
        private RoleColor()
        {
            this.command = "color";
            this.help = "sets the color of a role";
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
                event.getGuild().createRole().revoke(Permission.values()).setName(name).update();
            } catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to create the role", event);
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
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true),
                new Argument("to <username>",Argument.Type.LOCALUSER,true)
            };
            this.separatorRegex = "\\s+to\\s+";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Role role = (Role)args[0];
            User user = (User)args[1];
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
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true),
                new Argument("from <username>",Argument.Type.LOCALUSER,true)
            };
            this.separatorRegex = "\\s+from\\s+";
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
}
