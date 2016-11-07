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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.LocalTags;
import spectra.datasources.Tags;
import spectra.utils.FormatUtil;
import spectra.utils.OtherUtil;
import spectra.utils.TagUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Makebot extends Command{
    private final Tags tags;
    private final LocalTags localtags;
    private final String[] SYMBOLS = new String[]{"!","$","%","^","&",":",";",".","<",">","?","/","+","-","="};
    private final String[] NAMES = new String[]{"crap","slee","entro","enthal","thera","cree","jum","loo","poo",
        "skim","scrap","grum","flip","droo","chemothera","pup","cano","hap","unhap","chop","trip","bum","slop","cris","soa","wis","lum"};
    private final String START = "# DEFINITIONS BEGIN HERE";
    private final String END = "# DEFINITIONS END HERE";
    public Makebot(Tags tags, LocalTags localtags)
    {
        this.tags = tags;
        this.localtags = localtags;
        this.command = "makebot";
        this.help = "makes a bot";
        this.longhelp = "This command writes a new bot in python for the discord.py library. The commands for the bot are selected from "+SpConst.BOTNAME+"'s tag database.";
        this.cooldown = 60;
        this.cooldownKey = event -> event.getAuthor().getId()+"|makebot";
        this.hidden = true;
        this.requiredPermissions = new Permission[]{Permission.MESSAGE_ATTACH_FILES};
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        event.getChannel().sendTyping();
        String prefix = SYMBOLS[(int)(Math.random()*SYMBOLS.length)]+SYMBOLS[(int)(Math.random()*SYMBOLS.length)];
        List<String> botlines = OtherUtil.readTrueFileLines("bottemplate.py");
        List<String> customs = new ArrayList<>();
        customs.add("ownerid = \""+event.getAuthor().getId()+"\"");
        customs.add("prefix = \"\"\""+prefix+"\"\"\"");
        customs.add("tags = {");
        
        List<String[]> taglist = tags.findTags(null, event.getGuild(), false, false);
        List<String[]> taglist2;
        if(!event.isPrivate())
            taglist2 = localtags.findTags(null, event.getGuild(), false);
        else
            taglist2 = new ArrayList<>();
        if(taglist.isEmpty() && taglist2.isEmpty())
        {
            Sender.sendResponse(SpConst.WARNING+"No tags found!", event);
            return false;
        }
        for(int i=0; i<Math.random()*10+5; i++)
        {
            int num = (int)( Math.random() * (taglist.size()+taglist2.size()) );
            String[] tag = num<taglist.size() ? taglist.get(num) : taglist2.get(num - taglist.size());
            customs.add((i==0?"":",")+"\"\"\""+TagUtil.getTagname(tag)+"\"\"\" : \"\"\""+TagUtil.getContents(tag)+" \"\"\"");
        }
        customs.add("}");
        
        File file = new File("WrittenFiles"+File.separatorChar+NAMES[(int)(Math.random()*NAMES.length)]+".py");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) 
        {
            boolean replacementSection = false;
            for(String line : botlines)
            {
                if(replacementSection)
                {
                    if(line.equals(END))
                        replacementSection = false;
                }
                else
                {
                    if(!line.equals(START))
                        writer.write(line+"\r\n");
                    else
                    {
                        replacementSection = true;
                        for(String line2 : customs)
                        {
                            writer.write(line2+"\r\n");
                        }
                    }
                }
            }
            writer.flush();
            Sender.sendFileResponse(() -> {
                return new Pair<>(SpConst.SUCCESS+"Here is your new bot:\nOwner: "+FormatUtil.shortUser(event.getAuthor())+"\nPrefix: **"+prefix
                        +"**\n\nPlease include a bot token at the bottom of the file, and then run using the discord.py library.", file);
            }, event);
            return true;
        }catch(IOException e)
        {
            Sender.sendResponse(SpConst.ERROR+"An internal error occurred; please try again later.", event);
            return false;
        }
    }
}
