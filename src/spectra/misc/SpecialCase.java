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
package spectra.misc;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.priv.PrivateMessageReceivedEvent;

/**
 *
 * @author John Grosh (jagrosh)
 * 
 * This class is to handle all the things that only work on one server, or that may change,
 * and I don't want to have to track them down in random places in the code. Some things
 * are handled through the SpConst class, but those are for things that related to
 * the bot's function (like it's "home" server)
 * 
 */
public class SpecialCase {
    
    /*
        This bot is not a "Staff Bot", so it shouldn't be allowed to send messages
        in general. However, it has some useful tags for quick reference. Therefore,
        there is a check so that only tag commands can be used in general
    */
    public static String JDA_GUILD_GENERAL_ID = "125227483518861312";
    
    /*
        This bot (at present) can Manage Roles to assign a role to opt-out of a 
        channel, but it should not change it's role's color.
    */
    public static String DISCORD_BOTS_ID = "110373943822540800";
    
    /*
        This special case is to provide roles to users on the Monster Hunter
        Gathering Hall, via the ranks they give to Neko (Means "Cat") Bot.
    */
    private static final String NEKO_ID = "196047400811495424";
    private static final String FUNKY_ID = "210843560776302592";
    private static final String MHGH_ID = "120889695658967041";
    private static final String MEMBER = "144284726738288641";
    private static final String[] HR_ROLES = new String[]{
    "143408205802766336","143408266959912960","143408353299791872","143408442810433537","143408511206948865"
    };
    private static final int[] MH_LEVELS = new int[]{
        0,4,7,100,999
    };
    public static void giveMonsterHunterRole(PrivateMessageReceivedEvent event)
    {
        if(!event.getAuthor().getId().equals(NEKO_ID) && !event.getAuthor().getId().equals(FUNKY_ID))
            return;
        String[] parts = event.getMessage().getRawContent().split(":");
        User user = event.getJDA().getUserById(parts[0]);
        Guild mhgh = event.getJDA().getGuildById(MHGH_ID);
        if(user==null || mhgh==null || !mhgh.isAvailable() || !mhgh.isMember(user))
            return;
        int hr = Integer.parseInt(parts[1]);
        if(hr==0)
            return;
        Role member = mhgh.getRoleById(MEMBER);
        synchronized(mhgh.getManager()){
            if(!mhgh.getRolesForUser(user).contains(member))
                mhgh.getManager().addRoleToUser(user, member);
            boolean gotten=false;
            for(int i=MH_LEVELS.length-1; i>=0; i--)
            {
                Role role = mhgh.getRoleById(HR_ROLES[i]);
                if(hr>=MH_LEVELS[i] && !gotten)
                {
                    if(!mhgh.getRolesForUser(user).contains(role))
                        mhgh.getManager().addRoleToUser(user, role);
                    gotten=true;
                }
                else
                {
                    if(mhgh.getRolesForUser(user).contains(role))
                        mhgh.getManager().removeRoleFromUser(user, role);
                }
            }
            mhgh.getManager().update();
        }
    }
}
