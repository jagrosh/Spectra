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
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.RoleGroups;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ColorMe extends Command {
    private final RoleGroups groups;
    public ColorMe(RoleGroups groups)
    {
        this.groups = groups;
        this.command = "colorme";
        this.help = "changes the color of your role";
        this.availableInDM = false;
        this.longhelp = "This command sets the color of your role. It can only be used if your "
                + "color-determinant role (the role that controls your username color) has been added to the colorme list. The colormust be a #hex or integer code.";
        this.children = new Command[]{
            new ColorMeList(),
            new ColorMeAdd(),
            new ColorMeRemove()
        };
        this.arguments = new Argument[]{
            new Argument("color",Argument.Type.SHORTSTRING,true)
        };
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_ROLES
        };
        this.cooldown = 15;
        this.cooldownKey = event -> event.getAuthor().getId()+"|colorme";
        this.hidden = true;
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String in = (String)args[0];
        Color color;
        if(in.matches("#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])"))
            in = in.replaceAll("#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])", "#$1$1$2$2$3$3");
        try{
            color = Color.decode(in);
        }catch(NumberFormatException e){
            Sender.sendResponse(SpConst.ERROR+"\""+in+"\" is not a valid color.", event);
            return false;
        }
        if(color.getRed()==0 && color.getBlue()==0 && color.getGreen()==0)
            color = new Color(1,1,1);
        Role top = event.getGuild().getColorDeterminantRoleForUser(event.getAuthor());
        if(top.getId().equals(event.getGuild().getId()))
        {
            Sender.sendResponse(SpConst.ERROR+"You don't have any colored roles!", event);
            return false;
        }
        if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), top))
        {
            Sender.sendResponse(SpConst.ERROR+"I cannot recolor *"+top.getName()+"* due to role hierarchy.", event);
            return false;
        }
        String[] ids = groups.getIdsForGroup(event.getGuild().getId(), "colorme");
        for(String id : ids)
            if(id.equals(top.getId()))
            {
                try{
                    top.getManager().setColor(color).update();
                    Sender.sendResponse(SpConst.SUCCESS+"The role *"+top.getName()+"* was successfully changed to `"+in+"`", event);
                    return true;
                } catch (Exception e)
                {
                    Sender.sendResponse(SpConst.ERROR+"Something went wrong when trying to set the role color", event);
                    return false;
                }
            }
        Sender.sendResponse(SpConst.ERROR+"Your role (*"+top.getName()+"*) is not enabled for colorme!", event);
        return false;
    }
    
    private class ColorMeAdd extends Command
    {
        private ColorMeAdd()
        {
            this.command = "add";
            this.availableInDM = false;
            this.help = "adds a role to colorme";
            this.longhelp = "This command adds a role that can be recolored using the colorme command.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true)
            };
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Role role = (Role)args[0];
            if(role.getId().equals(event.getGuild().getId()))
            {
                Sender.sendResponse(SpConst.ERROR+"That role cannot be recolored, so it cannot be added", event);
                return false;
            }
            if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))
            {
                Sender.sendResponse(SpConst.ERROR+"I cannot recolor *"+role.getName()+"* due to role hierarchy.", event);
                return false;
            }
            if(groups.addIdToGroup(event.getGuild().getId(), "colorme", role.getId()))
            {
                Sender.sendResponse(SpConst.SUCCESS+"Added *"+role.getName()+"* to colorme", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"*"+role.getName()+"* is already a colorme role!", event);
                return false;
            }
        }
    }
    
    private class ColorMeRemove extends Command
    {
        private ColorMeRemove()
        {
            this.command = "remove";
            this.availableInDM = false;
            this.help = "removes a role to colorme";
            this.longhelp = "This command removes a role that can be recolored from the colorme command.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("rolename",Argument.Type.ROLE,true)
            };
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Role role = (Role)args[0];
            if(groups.removeIdFromGroup(event.getGuild().getId(), "colorme", role.getId()))
            {
                Sender.sendResponse(SpConst.SUCCESS+"Removed *"+role.getName()+"* from colorme", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"*"+role.getName()+"* is not a colorme role!", event);
                return false;
            }
        }
    }
    
    private class ColorMeList extends Command
    {
        private ColorMeList()
        {
            this.command = "list";
            this.availableInDM = false;
            this.help = "lists colorme roles";
            this.longhelp = "This command lists the roles that can be recolored from the colorme command.";
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] list = groups.getIdsForGroup(event.getGuild().getId(), "colorme");
            if(list.length==0)
            {
                Sender.sendResponse(SpConst.WARNING+"There are no colorme roles on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            StringBuilder builder = new StringBuilder("\uD83D\uDD8C ColorMe roles on **"+event.getGuild().getName()+"**:");
            for(String id : list)
            {
                Role role = event.getGuild().getRoleById(id);
                if(role==null)
                    groups.removeIdFromGroup(event.getGuild().getId(), "colorme", id);
                else
                    builder.append("\n").append(SpConst.LINESTART).append(role.getName());
            }
            builder.append("\nSee `").append(SpConst.PREFIX).append("colorme help` for how to add or remove roles");
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
}
