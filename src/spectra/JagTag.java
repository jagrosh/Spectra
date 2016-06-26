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
package spectra;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import spectra.utils.FinderUtil;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class JagTag {
    
    public static String description = "JagTag™ is a simple, interpreted scripting language.";
    
    public static String convertText(String input, String arguments, User user, Guild guild, MessageChannel channel)
    {
        String[] args = new String[0];
        
        if(arguments!=null && !arguments.trim().equals(""))
        {
            arguments = arguments.trim().replace("{","\u0013").replace("}","\u0014");
            args = arguments.split("\\s+");
        }
        if(arguments==null)
            arguments = "";
        String output = input;
        
        //direct replacements
        output = output.replace("{nsfw}","")
                .replace("{user}", user.getUsername())
                .replace("{userid}",user.getId())
                .replace("{nick}",((guild==null || guild.getNicknameForUser(user)==null) ? user.getUsername() : guild.getNicknameForUser(user)))
                .replace("{discrim}",user.getDiscriminator())
                .replace("{server}",(guild==null)?"Direct Message":guild.getName())
                .replace("{serverid}",(guild==null)?"0":guild.getId())
                .replace("{servercount}",(guild==null)?"1":guild.getUsers().size()+"")
                .replace("{channel}",(guild==null)?"#Direct Message":((TextChannel)channel).getName())
                .replace("{channelid}",(guild==null)?"0":((TextChannel)channel).getId())
                .replace("{args}",arguments)
                .replace("{argslen}",args.length+"")
                .replace("{avatar}",(user.getAvatarUrl()==null?"":user.getAvatarUrl()))
                ;
        
        JDA jda = (channel instanceof TextChannel ? ((TextChannel)channel).getJDA() : ((PrivateChannel)channel).getJDA());
                
        //random replacements
        int ind;
        
        while( (ind = output.indexOf("{randuser}")) != -1)
            output = output.substring(0, ind)+
                    ( (guild==null) ? jda.getUsers().get((int)(jda.getUsers().size()*Math.random())).getUsername() : guild.getUsers().get((int)(guild.getUsers().size()*Math.random())).getUsername())
                    +output.substring(ind+10);
        
        List<User> onlines = new ArrayList<>();
        if(output.contains("{randonline}") && guild!=null)
            guild.getUsers().stream().filter((u) -> (u.getOnlineStatus().equals(OnlineStatus.ONLINE))).forEach((u) -> {
                onlines.add(u);
            });
        while( (ind = output.indexOf("{randonline}")) != -1)
            output = output.substring(0, ind)+
                    ((guild==null) ? jda.getUsers().get((int)(jda.getUsers().size()*Math.random())).getUsername() : onlines.get((int)(onlines.size()*Math.random())).getUsername())
                    +output.substring(ind+12);
        
        while( (ind = output.indexOf("{randchannel}")) != -1)
            output = output.substring(0, ind)+
                    ((guild==null) ? "#sometextchannel" : guild.getTextChannels().get((int)(guild.getTextChannels().size()*Math.random())).getName())
                    +output.substring(ind+13);
        
        int iterations = 0;
        String lastoutput = "";
        HashMap<String,String> vars = new HashMap<>();
        while(!lastoutput.equals(output) && iterations<200 && output.length()<=4000)
        {
            lastoutput = output;
            iterations++;

            //int i2 = output.lastIndexOf("{");
            //int i1 = output.indexOf("}",i2);
            int i1 = output.indexOf("}");
            int i2 = (i1==-1 ? -1 : output.lastIndexOf("{", i1));
            
            if(i1!=-1 && i2!=-1)//otherwise, we're done
            { //evaluate between {}
                String toEval = output.substring(i2+1,i1);
                if(toEval.startsWith("arg:"))
                {
                    String num = toEval.substring(4);
                    try{
                        int argnum = Integer.parseInt(num);
                        if(args.length>0)
                            toEval = args[argnum % args.length];
                        else
                            toEval = "";
                    } catch(NumberFormatException e){}
                }
                else if (toEval.startsWith("set:"))
                {
                    String[] parts = Arrays.copyOf(toEval.substring(4).split("\\|",2),2);
                    vars.put(parts[0], parts[1]);
                    toEval = "";
                }
                else if (toEval.startsWith("get:"))
                {
                    toEval = vars.get(toEval.substring(4));
                }
                else if (toEval.startsWith("note:"))
                {
                    toEval="";
                }
                else if (toEval.startsWith("choose:"))
                {
                    String[] choices = toEval.substring(7).split("\\|");
                    if(choices.length == 0)
                        toEval = "";
                    else
                        toEval = choices[(int)(choices.length*Math.random())];
                }
                else if (toEval.startsWith("range:"))
                {
                    String[] ends = toEval.substring(6).split("\\|",2);
                    if(ends.length == 0)
                        toEval = "";
                    else if(ends.length == 1)
                        toEval = ends[0];
                    else{
                        try{
                        int first = (int)(Double.parseDouble(ends[0]));
                        int second = (int)(Double.parseDouble(ends[1]));
                        if(second<first)
                        {
                            int tmp = second;
                            second = first;
                            first = tmp;
                        }
                        toEval = ""+(first+(int)(Math.random()*(second-first)));
                        }catch(NumberFormatException e){}
                    }
                }
                else if (toEval.startsWith("replace:"))
                {
                    int index1 = toEval.indexOf("|with:");
                    int index2 = toEval.indexOf("|in:",index1);
                    if(index1!=-1 && index2!=-1)
                    {
                        String rep = toEval.substring(8,index1);
                        String rwith = toEval.substring(index1+6,index2);
                        String rin = toEval.substring(index2+4);
                        if(rep.length()>0)
                        toEval = rin.replace(rep,rwith);
                    }
                }
                else if (toEval.startsWith("replaceregex:"))
                {
                    int index1 = toEval.indexOf("|with:");
                    int index2 = toEval.indexOf("|in:",index1);
                    if(index1!=-1 && index2!=-1)
                    {
                        String rep = toEval.substring(13,index1);
                        String rwith = toEval.substring(index1+6,index2);
                        String rin = toEval.substring(index2+4);
                        if(rep.length()>0)
                            try{
                                toEval = rin.replaceAll(rep.replace("\u0013","{").replace("\u0014","}"),rwith).replace("{","\u0013").replace("}","\u0014");
                               }catch(Exception e){}
                    }
                }
                else if (toEval.startsWith("url:"))
                {
                    try {
                        toEval = URLEncoder.encode(toEval.substring(4), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        toEval = toEval.substring(4).replace("-","--").replace("_","__").replace("%","~p").replace("?", "~q").replace(" ", "_");
                    }
                }
                else if (toEval.startsWith("lower:"))
                {
                    toEval = toEval.substring(6).toLowerCase();
                }
                else if (toEval.startsWith("upper:"))
                {
                    toEval = toEval.substring(6).toUpperCase();
                }
                else if (toEval.startsWith("length:"))
                {
                    toEval = toEval.substring(7).length()+"";
                }
                else if (toEval.startsWith("user:"))
                {
                    String in = toEval.substring(5).trim();
                    if(in.equals(""))
                        toEval = "";
                    else
                    {
                        List<User> users=null;
                        if(guild!=null)
                            users = FinderUtil.findUsers(in, guild);
                        if(users==null || users.isEmpty())
                            users = FinderUtil.findUsers(in, jda);
                        if(users.isEmpty())
                        {
                            return String.format(SpConst.NONE_FOUND, "users", in);
                        }
                        else if (users.size()>1)
                        {
                            return FormatUtil.listOfUsers(users, in);
                        }
                        toEval = users.get(0).getUsername();
                    }
                }
                else if (toEval.startsWith("nick:"))
                {
                    String in = toEval.substring(5).trim();
                    if(in.equals(""))
                        toEval = "";
                    else
                    {
                        List<User> users=null;
                        if(guild!=null)
                            users = FinderUtil.findUsers(in, guild);
                        if(users==null || users.isEmpty())
                            users = FinderUtil.findUsers(in, jda);
                        if(users.isEmpty())
                        {
                            return String.format(SpConst.NONE_FOUND, "users", in);
                        }
                        else if (users.size()>1)
                        {
                            return FormatUtil.listOfUsers(users, in);
                        }
                        if(guild==null || guild.getNicknameForUser(users.get(0))==null)
                            toEval = users.get(0).getUsername();
                        else
                            toEval = guild.getNicknameForUser(users.get(0));
                    }
                }
                else if (toEval.startsWith("if:"))
                {
                    int index1 = toEval.indexOf("|then:");
                    int index2 = toEval.indexOf("|else:",index1);
                    if(index1!=-1 && index2!=-1)
                    {
                        String statement = toEval.substring(3,index1);
                        String sthen = toEval.substring(index1+6,index2);
                        String selse = toEval.substring(index2+6);
                        if(evaluateStatement(statement))
                            toEval = sthen;
                        else
                            toEval = selse;
                    }
                }
                else if(toEval.startsWith("math:"))
                {
                    toEval = evaluateMath(toEval.substring(5));
                }
                else
                    toEval = "\u0013"+toEval+"\u0014";
                output = output.substring(0,i2) + toEval + output.substring(i1+1);
            }
        }

        lastoutput = lastoutput.replace("\u0013","{").replace("\u0014","}").replaceAll("\n\n\n+", "\n\n\n");
        if(lastoutput.length()>2000)
            lastoutput = lastoutput.substring(0,2000);
        return lastoutput;
    }
    
    private static String evaluateMath(String statement)
    {
        int index = statement.lastIndexOf("|+|");
        if(index==-1)
            index = statement.lastIndexOf("|-|");
        if(index==-1)
            index = statement.lastIndexOf("|*|");
        if(index==-1)
            index = statement.lastIndexOf("|%|");
        if(index==-1)
            index = statement.lastIndexOf("|/|");
        if(index==-1)
            return statement;
        String first = evaluateMath(statement.substring(0,index));
        String second = evaluateMath(statement.substring(index+3));
        Double val1;
        Double val2;
        try{
            val1 = Double.parseDouble(first);
            val2 = Double.parseDouble(second);
            switch (statement.substring(index, index+3)){
                case "|+|":
                    return ""+(val1+val2);
                case "|-|":
                    return ""+(val1-val2);
                case "|*|":
                    return ""+(val1*val2);
                case "|%|":
                    return ""+(val1%val2);
                case "|/|":
                    return ""+(val1/val2);
            }
        }catch(Exception e){}
        switch (statement.substring(index, index+3)){
                case "|+|":
                    return first+second;
                case "|-|":
                    int loc = first.indexOf(second);
                    if(loc!=-1)
                        return first.substring(0,loc)+(loc+second.length()<first.length()?first.substring(loc+second.length()):"");
                    return first+"-"+second;
                case "|*|":
                    return first+"*"+second;
                case "|%|":
                    return first+"%"+second;
                case "|/|":
                    return first+"/"+second;
            }
        return statement;
    }
    
    private static boolean evaluateStatement(String statement)
    {
        int index = statement.indexOf("|=|");
        if(index==-1)
            index = statement.indexOf("|<|");
        if(index==-1)
            index = statement.indexOf("|>|");
        if(index==-1)
            index = statement.indexOf("|~|");
        if(index==-1)
            return false;
        String s1 = statement.substring(0, index);
        String s2 = statement.substring(index+3);
        
        try{
            double i1 = Double.parseDouble(s1);
            double i2 = Double.parseDouble(s2);
            switch(statement.substring(index, index+3))
        {
            case "|=|":
                return (i1==i2);
            case "|~|":
                return (((int)(i1*100))==((int)(i2*100)));
            case "|>|":
                return (i1>i2);
            case "|<|":
                return (i1<i2);
        }
        }catch(NumberFormatException e){}
        
        switch(statement.substring(index, index+3))
        {
            case "|=|":
                return (s1.equals(s2));
            case "|~|":
                return (s1.equalsIgnoreCase(s2));
            case "|>|":
                return (s1.compareTo(s2)>0);
            case "|<|":
                return (s1.compareTo(s2)<0);
        }
        return false;
    }
}
