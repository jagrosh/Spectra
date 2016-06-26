/*
 * Copyright 2016 jagrosh.
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
package spectra;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public enum PermLevel {
    EVERYONE(0), MODERATOR(1), ADMIN(2), JAGROSH(3);
    
    final int value;
    private PermLevel(int value)
    {
        this.value = value;
    }
    
    public boolean isAtLeast(PermLevel other)
    {
        return value >= other.value;
    }
    
    public static PermLevel getPermLevelForUser(User user, Guild guild, String[] currentSettings)
    {
        PermLevel perm = EVERYONE;//start with everyone
        if(user.getId().equals(SpConst.JAGROSH_ID))
            perm = JAGROSH;
        if(guild==null)
            return perm;
        if(PermissionUtil.checkPermission(user, Permission.MANAGE_SERVER, guild))
            perm = ADMIN;
        else
        {
            if(currentSettings==null)
                return perm;
            if(currentSettings[Settings.MODIDS].contains(user.getId()))
                perm = MODERATOR;
            else
            {
                for(Role r:guild.getRolesForUser(user))
                    if(currentSettings[Settings.MODIDS].contains("r"+r.getId()))
                    {
                        perm = PermLevel.MODERATOR;
                        break;
                    }
            }
        }
        return perm;
    }
    
    public static PermLevel getPermLevelForUser(User user, Guild guild)
    {
        return getPermLevelForUser(user,guild,guild!=null ? Settings.getInstance().getSettingsForGuild(guild.getId()) : null);
    }
}
