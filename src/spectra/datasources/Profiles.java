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
package spectra.datasources;

import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Profiles extends DataSource {
    
    public Profiles()
    {
        this.filename = "discordbot.profiles";
        this.size = profCall.length;
        this.generateKey = (item)->{return item[0];};
    }
    
    public static final String[] profCall = {
        "","battle","email","league","maplestory","minecraft","monhun","nnid","steam","twitter","youtube","about","reddit","3ds","splatoon","twitch","smash","timezone","psn","xbox","warframe","smite"
    };
    public static final String[] profName = {
        "","Battle.net account","E-mail address","League of Legends username","Maplestory username","Minecraft username","MonsterHunter info","NNID (Nintendo Network ID)","Steam account","Twitter handle",
        "YouTube channel","AboutMe","Reddit username","3DS Friend Code","Splatoon info","Twitch channel","Smash Bros info","Timezone","PlayStation Network info","XBox Live info","Warframe info","Smite username"
    };
    
    public static int indexForField(String field)
    {
        for(int i=1;i<profCall.length;i++)
            if(profCall[i].equalsIgnoreCase(field))
                return i;
        return -1;
    }
    
    public static String contructProfile(String[] profile)
    {
        StringBuilder builder = new StringBuilder();
        int[] profOrder = {1,2,3,4,5,6,7,16,14,13,18,19,8,9,10,15,12,20,21,17,11};
        for(int j=0;j<profOrder.length;j++){
            int i = profOrder[j];
            if(profile[i]!=null && !profile[i].equals(""))
                builder.append("\n**").append(profName[i].split(" ")[0]).append("**: ").append(profile[i]);
        }
        String str = builder.toString().trim();
        if(str.equals(""))
            str = "*looks like there's nothing here*";
        if(str.length()>1600)
            str=str.substring(0,1600)+"\n*(...profile too long!)*";
        return "\n"+str;
    }
}
