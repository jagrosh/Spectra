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

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Overrides;
import spectra.datasources.Settings;
import spectra.datasources.Tags;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Tag extends Command{
    public Tag()
    {
        this.command = "tag";
        this.aliases = new String[]{"t"};
        this.help = "displays a tag; tag commands (try `"+SpConst.PREFIX+"tag help`)";
        this.longhelp = "This command is used to create, edit, and view \"tags\". "
                + "Tags are a method of storing text for easy recollection later. "
                + "Additionally, some scripting-esque elements can be used to provide "
                + "extra functionality.";
        this.arguments = new Argument[]{
            new Argument("tagname",Argument.Type.SHORTSTRING,true),
            new Argument("tag arguments",Argument.Type.LONGSTRING,false)
        };
        this.children = new Command[]{
            new TagCreate(),
            new TagDelete(),
            new TagEdit(),
            new TagList(),
            new TagOwner(),
            new TagRandom(),
            new TagRaw(),
            new TagRaw2(),
            new TagSearch()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) 
    {
        String tagname = (String)(args[0]);
        String tagargs = (String)(args[1]);
        boolean local = false;
        boolean nsfw = true;
        if(!event.isPrivate())
        {
            local = "local".equalsIgnoreCase(Settings.getInstance().getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
        }
        String[] tag = null;
        if(!event.isPrivate())
            tag = Overrides.getInstance().findTag(event.getGuild(), tagname, nsfw);
        if(tag==null)
            tag = Tags.getInstance().findTag(tagname, event.getGuild(), local, nsfw);
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
            this.aliases= new String[]{"add"};
            this.help = "saves a tag for later recollection";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true),
                new Argument("tag contents",Argument.Type.LONGSTRING,true)
            };
            this.cooldown = 60;
        }
        @Override
        protected String cooldownKey(MessageReceivedEvent event) {return event.getAuthor().getId()+"tagcreate";}
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = (String)(args[1]);
            String[] tag = Tags.getInstance().findTag(tagname);
            if(tag==null)//good to make it
            {
                Tags.getInstance().setTag(new String[]{
                event.getAuthor().getId(),
                tagname,
                contents
                });
                Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tagname+"\" created successfully.", event.getChannel(), event.getMessage().getId());
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tag[Tags.TAGNAME]+"\" already exists.", event.getChannel(), event.getMessage().getId());
                return false;
            }
        }
    }
    
    private class TagDelete extends Command
    {
        private TagDelete()
        {
            this.command = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "deletes a tag if it belongs to you";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String[] tag = Tags.getInstance().findTag(tagname);
            if(tag==null)//nothing to edit
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event.getChannel(), event.getMessage().getId());
                return false;
            }
            else if(tag[Tags.OWNERID].equals(event.getAuthor().getId()))
            {
                Tags.getInstance().removeTag(tag[Tags.TAGNAME]);
                Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tag[Tags.TAGNAME]+"\" deleted successfully.", event.getChannel(), event.getMessage().getId());
                return true;
            }
            else
            {
                String owner;
                User u = event.getJDA().getUserById(tag[Tags.OWNERID]);
                if(u!=null)
                    owner = "**"+u.getUsername()+"**";
                //else if(tag[Tags.OWNERID].startsWith("g"))
                    //owner = "the server *"+event.getGuild().getName()+"*";
                else
                    owner = "an unknown user (ID:"+tag[Tags.OWNERID]+")";
                Sender.sendResponse(SpConst.ERROR+"You cannot delete tag \""+tag[Tags.TAGNAME]+"\" because it belongs to **"+owner+"**", event.getChannel(), event.getMessage().getId());
                return false;
            }
        }
    }
    
    private class TagEdit extends Command
    {
        private TagEdit()
        {
            this.command = "edit";
            this.help = "edits a tag if it belongs to you";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true),
                new Argument("new contents",Argument.Type.LONGSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = (String)(args[1]);
            String[] tag = Tags.getInstance().findTag(tagname);
            if(tag==null)//nothing to edit
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event.getChannel(), event.getMessage().getId());
                return false;
            }
            else if(tag[Tags.OWNERID].equals(event.getAuthor().getId()))
            {
                Tags.getInstance().setTag(new String[]{
                event.getAuthor().getId(),
                tag[Tags.TAGNAME],
                contents
                });
                Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tag[Tags.TAGNAME]+"\" edited successfully.", event.getChannel(), event.getMessage().getId());
                return true;
            }
            else
            {
                String owner;
                User u = event.getJDA().getUserById(tag[Tags.OWNERID]);
                if(u!=null)
                    owner = "**"+u.getUsername()+"**";
                //else if(tag[Tags.OWNERID].startsWith("g"))
                    //owner = "the server *"+event.getGuild().getName()+"*";
                else
                    owner = "an unknown user (ID:"+tag[Tags.OWNERID]+")";
                Sender.sendResponse(SpConst.ERROR+"You cannot edit tag \""+tag[Tags.TAGNAME]+"\" because it belongs to **"+owner+"**", event.getChannel(), event.getMessage().getId());
                return false;
            }
        }
    }
    
    private class TagList extends Command
    {
        private TagList()
        {
            this.command = "list";
            this.help = "shows a list of all tags owned by you, or another user";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("username",Argument.Type.USER,false)
            };
            this.cooldown = 10;
        }
        @Override
        protected String cooldownKey(MessageReceivedEvent event) {return event.getAuthor().getId()+"taglist";}
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            User user = (User)(args[0]);
            if(user==null)
                user = event.getAuthor();
            boolean nsfw = true;
            if(!event.isPrivate())
                nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
            ArrayList<String[]> tags = Tags.getInstance().findTagsByOwner(user, nsfw);
            StringBuilder builder;
            builder = new StringBuilder(SpConst.SUCCESS+tags.size()+" tags owned by **"+user.getUsername()+"**: \n");
            tags.stream().forEach((tag) -> {
                builder.append(tag[Tags.TAGNAME]).append(" ");
            });
            Sender.sendResponse(builder.toString(), event.getChannel(), event.getMessage().getId());
            return true;
        }
    }
    
    private class TagOwner extends Command
    {
        private TagOwner()
        {
            this.command = "owner";
            this.help = "shows the owner of a tag";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String[] tag = null;
            if(!event.isPrivate())
                tag = Overrides.getInstance().findTag(event.getGuild(), tagname, true);
            if(tag==null)
                tag = Tags.getInstance().findTag(tagname, event.getGuild(), false, true);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event.getChannel(), event.getMessage().getId());
                return false;
            }
            String owner;
            User u = event.getJDA().getUserById(tag[Tags.OWNERID]);
            if(u!=null)
                owner = "**"+u.getUsername()+"**";
            else if(tag[Tags.OWNERID].startsWith("g"))
                owner = "the server *"+event.getGuild().getName()+"*";
            else
                owner = "an unknown user (ID:"+tag[Tags.OWNERID]+")";
            Sender.sendResponse("Tag \""+tag[Tags.TAGNAME]+"\" belongs to "+owner, event.getChannel(), event.getMessage().getId());
            return true;
        }
    }
    
    private class TagRandom extends Command
    {
        private TagRandom()
        {
            this.command = "random";
            this.help = "shows a random tag";
            //this.longhelp = "";
            this.arguments = new Argument[]{
            new Argument("tag arguments",Argument.Type.LONGSTRING,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagargs = (String)(args[0]);
            boolean local = false;
            boolean nsfw = true;
            if(!event.isPrivate())
            {
                local = "local".equalsIgnoreCase(Settings.getInstance().getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
                nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
            }
            List<String[]> tags = Tags.getInstance().findTags(null, event.getGuild(), local, nsfw);
            if(tags.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"No tags found!", event.getChannel(), event.getMessage().getId());
                return false;
            }
            String[] tag = tags.get((int)(Math.random()*tags.size()));
            Sender.sendResponse("Tag \""+tag[Tags.TAGNAME]+"\":\n"
                    +Tags.convertText(tag[Tags.CONTENTS], tagargs, event.getAuthor(), event.getGuild(), event.getChannel()),
                    event.getChannel(), event.getMessage().getId());
            return true;
        }
    }
    
    private class TagRaw extends Command
    {
        private TagRaw()
        {
            this.command = "raw";
            this.help = "shows the raw (non-dynamic) tag text";
            //this.longhelp = "";
            this.arguments = new Argument[]{
            new Argument("tagname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            boolean local = false;
            boolean nsfw = true;
            if(!event.isPrivate())
            {
                local = "local".equalsIgnoreCase(Settings.getInstance().getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
                nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
            }
            String[] tag = null;
            if(!event.isPrivate())
                tag = Overrides.getInstance().findTag(event.getGuild(), tagname, nsfw);
            if(tag==null)
                tag = Tags.getInstance().findTag(tagname, event.getGuild(), local, nsfw);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event.getChannel(), event.getMessage().getId());
                return false;
            }
            Sender.sendResponse("\u180E"+tag[Tags.CONTENTS], event.getChannel(), event.getMessage().getId());
            return true;
        }
    }
    
    private class TagRaw2 extends Command
    {
        private TagRaw2()
        {
            this.command = "raw2";
            this.help = "shows the raw tag text in a code block";
            //this.longhelp = "";
            this.arguments = new Argument[]{
            new Argument("tagname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            boolean local = false;
            boolean nsfw = true;
            if(!event.isPrivate())
            {
                local = "local".equalsIgnoreCase(Settings.getInstance().getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
                nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
            }
            String[] tag = null;
            if(!event.isPrivate())
                tag = Overrides.getInstance().findTag(event.getGuild(), tagname, nsfw);
            if(tag==null)
                tag = Tags.getInstance().findTag(tagname, event.getGuild(), local, nsfw);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event.getChannel(), event.getMessage().getId());
                return false;
            }
            Sender.sendResponse("```\n"+tag[Tags.CONTENTS]+"```", event.getChannel(), event.getMessage().getId());
            return true;
        }
    }
    
    private class TagSearch extends Command
    {
        private TagSearch()
        {
            this.command = "search";
            this.help = "searches for tags that include the given query";
            //this.longhelp = "";
            this.arguments = new Argument[]{
            new Argument("query",Argument.Type.SHORTSTRING,false)
        };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String query = (String)(args[0]);
            boolean local = false;
            boolean nsfw = true;
            if(!event.isPrivate())
            {
                local = "local".equalsIgnoreCase(Settings.getInstance().getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
                nsfw = event.getTextChannel().getName().contains("nsfw") || event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}");
            }
            List<String[]> tags = Tags.getInstance().findTags(query, event.getGuild(), local, nsfw);
            if(tags.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"No tags found matching \""+query+"\"!", event.getChannel(), event.getMessage().getId());
                return false;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS).append(tags.size()).append(" tags found");
            if(query!=null)
                builder.append(" containing \"").append(query).append("\"");
            builder.append(":\n");
            tags.stream().forEach((tag) -> {
                builder.append(tag[Tags.TAGNAME]).append(" ");
            });
            Sender.sendResponse(builder.toString(),event.getChannel(), event.getMessage().getId());
            return true;
        }
    }
}
