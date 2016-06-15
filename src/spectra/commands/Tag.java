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
package spectra.commands;

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Settings;
import spectra.datasources.Tags;

/**
 *
 * @author johna
 */
public class Tag extends Command{

    public Tag()
    {
        this.command = "tag";
        this.aliases = new String[]{"t"};
        this.help = "displays a tag; tag commands (try `"+SpConst.PREFIX+"tag help`)";
        //this.longhelp = "";
        this.arguments = new Argument[]{
            new Argument("tagname",Argument.Type.SHORTSTRING,true),
            new Argument("tag arguments",Argument.Type.LONGSTRING,false)
        };
        this.children = new Command[]{
            new TagCreate(),
            new TagList()
        };
    }
    
    @Override
    protected boolean execute(Object[] args, String[] settings, MessageReceivedEvent event) 
    {
        String tagname = (String)(args[0]);
        String tagargs = (String)(args[1]);
        boolean local = false;
        boolean nsfw = true;
        if(!event.isPrivate())
        {
            local = "local".equalsIgnoreCase(settings[Settings.TAGMODE]);
            nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
        }
        String[] tag = Tags.getInstance().findTag(tagname, event.getGuild(), local, nsfw);
        if(tag==null)
        {
            Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event.getChannel(), event.getMessage().getId());
            return false;
        }
        Sender.sendResponse("\u180E"+Tags.convertText(tag[Tags.CONTENTS], tagargs, event.getAuthor(), event.getGuild(), event.getChannel()), event.getChannel(), event.getMessage().getId());
        return true;
    }
    
    //subcommands
    private class TagCreate extends Command
    {
        private TagCreate()
        {
            this.command = "create";
            this.help = "";
            this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true),
                new Argument("tag contents",Argument.Type.LONGSTRING,true)
            };
            this.cooldown = 60;
        }
        @Override
        protected boolean execute(Object[] args, String[] settings, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = (String)(args[1]);
        }
    }
    
    private class TagList extends Command
    {
        private TagList()
        {
            this.command = "list";
            this.help = "";
            this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("username",Argument.Type.USER,false)
            };
            this.cooldown = 10;
        }
        @Override
        protected boolean execute(Object[] args, String[] settings, MessageReceivedEvent event)
        {
            User user = (User)(args[0]);
            if(user==null)
                user = event.getAuthor();
        }
    }
}
