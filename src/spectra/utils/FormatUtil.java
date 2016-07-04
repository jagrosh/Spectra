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
package spectra.utils;

import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FormatUtil {
    
    //Cleanly splits on whitespace in 2
    public static String[] cleanSplit(String input){return cleanSplit(input,"\\s+",2);}
    
    //Cleanly splits on whitespace to specified size
    public static String[] cleanSplit(String input, int size){return cleanSplit(input,"\\s+",size);}
    
    //Cleanly splits in 2 on specified regex
    public static String[] cleanSplit(String input, String regex){return cleanSplit(input,regex,2);}
    
    //Cleanly splits on given to specified size, padding with null
    public static String[] cleanSplit(String input, String regex, int size)
    {
        return Arrays.copyOf(input.split(regex, size), size);
    }
    
    public static String appendAttachmentUrls(Message message)
    {
        String content = message.getRawContent();
        if(message.getAttachments()!=null)
            content = message.getAttachments().stream().map((att) -> " "+att.getUrl()).reduce(content, String::concat);
        return content;
    }
    
    public static String demention(String input)
    {
        return input.replace("<@", "<@\u180E");
    }
    
    public static String unembed(String input)
    {
        return input.replaceAll("(?:\\s|^)<?(https?:\\/\\/.+?)>?(?=\\s|$)", " <$1>").trim();
    }
    
    public static String secondsToTime(long timeseconds)
    {
        StringBuilder builder = new StringBuilder();
        int years = (int)(timeseconds / (60*60*24*365));
        if(years>0)
        {
            builder.append("**").append(years).append("** years, ");
            timeseconds = timeseconds % (60*60*24*365);
        }
        int weeks = (int)(timeseconds / (60*60*24*365));
        if(weeks>0)
        {
            builder.append("**").append(weeks).append("** weeks, ");
            timeseconds = timeseconds % (60*60*24*7);
        }
        int days = (int)(timeseconds / (60*60*24));
        if(days>0)
        {
            builder.append("**").append(days).append("** days, ");
            timeseconds = timeseconds % (60*60*24);
        }
        int hours = (int)(timeseconds / (60*60));
        if(hours>0)
        {
            builder.append("**").append(hours).append("** hours, ");
            timeseconds = timeseconds % (60*60);
        }
        int minutes = (int)(timeseconds / (60));
        if(minutes>0)
        {
            builder.append("**").append(minutes).append("** minutes, ");
            timeseconds = timeseconds % (60);
        }
        if(timeseconds>0)
            builder.append("**").append(timeseconds).append("** seconds");
        String str = builder.toString();
        if(str.endsWith(", "))
            str = str.substring(0,str.length()-2);
        if(str.equals(""))
            str="**No time**";
        return str;
    }
    
    public static String listOfUsers(List<User> list, String query)
    {
        String out = String.format(SpConst.MULTIPLE_FOUND, "users", query);
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getUsername()+" #"+list.get(i).getDiscriminator();
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    public static String listOfChannels(List<TextChannel> list, String query)
    {
        String out = String.format(SpConst.MULTIPLE_FOUND, "text channels", query);
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" (<#"+list.get(i).getId()+">)";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    public static String listOfRoles(List<Role> list, String query)
    {
        String out = String.format(SpConst.MULTIPLE_FOUND, "roles", query);
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" (ID:"+list.get(i).getId()+")";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
}
