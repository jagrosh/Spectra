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
import java.util.Collections;
import java.util.List;
import javafx.util.Pair;
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
public class RoleMe extends Command {
    private final RoleGroups groups;
    public RoleMe(RoleGroups groups)
    {
        this.groups = groups;
        this.command = "roleme";
        this.help = "give yourself a role an admin has set to be self-assignable";
        this.availableInDM = false;
        this.longhelp = "This command gives you a role if an admin has set it to be a self-assignable role. By default, no roles are self-assignable.";
        this.children = new Command[]{
            new RoleMeList(),
            
            new RoleMeAdd(),
            new RoleMeMax(),
            new RoleMeRemove()
        };
        this.arguments = new Argument[]{
            new Argument("rolename",Argument.Type.ROLE,true)
        };
        this.requiredPermissions = new Permission[]{
            Permission.MANAGE_ROLES
        };
        this.cooldown = 10;
        this.goldlistCooldown = 2;
        this.cooldownKey = event -> event.getAuthor().getId()+"|roleme";
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        Role role = (Role)args[0];
        String[] ids = groups.getIdsForGroup(event.getGuild().getId(), "roleme");
        ArrayList<Role> rolemes = new ArrayList<>();
        for(String id: ids)
            if(event.getGuild().getRoleById(id)!=null)
                rolemes.add(event.getGuild().getRoleById(id));
        if(!rolemes.contains(role))
        {
            Sender.sendResponse(SpConst.ERROR+"*"+role.getName()+"* is not enabled for roleme!", event);
            return false;
        }
        
        if(event.getGuild().getRolesForUser(event.getAuthor()).contains(role))//removing
        {
            try
            {
                event.getGuild().getManager().removeRoleFromUser(event.getAuthor(), role).update();
                Sender.sendResponse(SpConst.SUCCESS+"The role *"+role.getName()+"* has been removed", event);
                return true;
            }
            catch (Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to remove the role *"+role.getName()+"*", event);
                return false;
            }
        }
        else //adding
        {
            List<Pair<String,Integer>> list = parseMax(groups.getSettings(event.getGuild().getId(), "roleme"));
            for(Pair<String,Integer> pair : list)
            {
                if(role.getName().contains(pair.getKey()))
                {
                    if(pair.getValue()<=0 || event.getGuild().getRolesForUser(event.getAuthor()).stream().filter(r -> r.getName().contains(pair.getKey()) && rolemes.contains(r)).count() < pair.getValue())
                    {
                        try {
                            event.getGuild().getManager().addRoleToUser(event.getAuthor(), role).update();
                            Sender.sendResponse(SpConst.SUCCESS+"You have been given the role *"+role.getName()+"*", event);
                            return true; 
                        } catch(Exception e)
                        {
                            Sender.sendResponse(SpConst.ERROR+"I was unable to add the role *"+role.getName()+"*", event);
                            return false;
                        }
                    }
                    else
                    {
                        Sender.sendResponse(SpConst.ERROR+"You already have at least `"+pair.getValue()+"` roleme roles"+(pair.getKey().equals("") ? "" : " containing `"+pair.getKey()+"`")+"!", event);
                        return false;
                    }
                }
            }
            try {
                event.getGuild().getManager().addRoleToUser(event.getAuthor(), role).update();
                Sender.sendResponse(SpConst.SUCCESS+"You have been given the role *"+role.getName()+"*", event);
                return true; 
            } catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"I was unable to add the role *"+role.getName()+"*", event);
                return false;
            }
        }
    }
    
    private class RoleMeMax extends Command
    {
        private RoleMeMax()
        {
            this.command = "max";
            this.availableInDM = false;
            this.help = "sets the max number of roleme roles a user can have";
            this.longhelp = "This command sets the max number of self-assignable roles a user can give themself using the roleme command. Set this to 0 for no maximum.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("number",Argument.Type.LONGSTRING,true)
            };
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String max = (String)args[0];
            List<Pair<String,Integer>> list = parseMax(max);
            if(list!=null)
            {
                groups.setSettings(event.getGuild().getId(), "roleme", max);
                if(list.size()==1 && list.get(0).getKey().equals(""))
                    Sender.sendResponse(SpConst.SUCCESS+"Users can now assign themselves "+(list.get(0).getValue()<=0 ? "any number of" : "`"+list.get(0).getValue()+"`")+" roleme roles.", event);
                else
                {
                    StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"Users can now assign themselves:");
                    list.stream().forEach((pair) -> {
                        builder.append("\n").append(pair.getValue()<=0 ? "Any number of" : "`"+pair.getValue()+"`")
                                .append(" roles").append(pair.getKey().equals("") ? "" : " containing `"+pair.getKey()+"`");
                    });
                    builder.append("\n(Checked in that order)");
                    Sender.sendResponse(builder.toString(), event);
                }
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"The following max could not be parsed: `"+max+"`", event);
                return false;
            }
        }
    }
    
    private class RoleMeAdd extends Command
    {
        private RoleMeAdd()
        {
            this.command = "add";
            this.availableInDM = false;
            this.help = "adds a role to roleme";
            this.longhelp = "This command adds a role that can be self-assigned using the roleme command.";
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
                Sender.sendResponse(SpConst.ERROR+"That role is the default role, so it cannot be added", event);
                return false;
            }
            if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), role))
            {
                Sender.sendResponse(SpConst.ERROR+"I cannot give the role *"+role.getName()+"* to users due to role hierarchy.", event);
                return false;
            }
            if(groups.addIdToGroup(event.getGuild().getId(), "roleme", role.getId()))
            {
                Sender.sendResponse(SpConst.SUCCESS+"Added *"+role.getName()+"* to roleme", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"*"+role.getName()+"* is already a roleme role!", event);
                return false;
            }
        }
    }
    
    private class RoleMeRemove extends Command
    {
        private RoleMeRemove()
        {
            this.command = "remove";
            this.availableInDM = false;
            this.help = "removes a role from roleme";
            this.longhelp = "This command removes a role that can be self-assigned from the roleme command.";
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
            if(groups.removeIdFromGroup(event.getGuild().getId(), "roleme", role.getId()))
            {
                Sender.sendResponse(SpConst.SUCCESS+"Removed *"+role.getName()+"* from roleme", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"*"+role.getName()+"* is not a roleme role!", event);
                return false;
            }
        }
    }
    
    private class RoleMeList extends Command
    {
        private RoleMeList()
        {
            this.command = "list";
            this.availableInDM = false;
            this.help = "lists roleme roles";
            this.longhelp = "This command lists the roles that can be self-assigned from the roleme command.";
            this.requiredPermissions = new Permission[]{
                Permission.MANAGE_ROLES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] list = groups.getIdsForGroup(event.getGuild().getId(), "roleme");
            if(list.length==0)
            {
                Sender.sendResponse(SpConst.WARNING+"There are no roleme roles on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            StringBuilder builder = new StringBuilder("\uD83C\uDFAD RoleMe roles on **"+event.getGuild().getName()+"**:");
            for(String id : list)
            {
                Role role = event.getGuild().getRoleById(id);
                if(role==null)
                    groups.removeIdFromGroup(event.getGuild().getId(), "roleme", id);
                else
                    builder.append("\n").append(SpConst.LINESTART).append(role.getName());
            }
            builder.append("\nSee `").append(SpConst.PREFIX).append("roleme help` for how to add or remove roles");
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private List<Pair<String,Integer>> parseMax(String input)
    {
        try{
            return Collections.singletonList(new Pair<>("",input==null||input.equals("") ? 0 : Integer.parseInt(input)));
        } catch(NumberFormatException e) {}
        String[] parts = input.split("\\|");
        ArrayList<Pair<String,Integer>> list = new ArrayList<>();
        for(String str : parts)
        {
            String[] keyval = str.split(":",2);
            if(keyval.length==1)
                return null;
            try{
                list.add(new Pair<>(keyval[1].trim(),Integer.parseInt(keyval[0].trim())));
            } catch(NumberFormatException e){return null;}
        }
        return list;
    }
}
