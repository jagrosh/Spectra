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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.Spectra;
import spectra.datasources.Feeds;
import spectra.datasources.LocalTags;
import spectra.datasources.Overrides;
import spectra.datasources.Settings;
import spectra.datasources.Tags;
import spectra.utils.TagUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Tag extends Command{
    private final Tags tags;
    private final LocalTags localtags;
    private final Overrides overrides;
    private final Settings settings;
    private final FeedHandler handler;
    private final Spectra spectra;
    public Tag(Tags tags, LocalTags localtags, Overrides overrides, Settings settings, FeedHandler handler, Spectra spectra)
    {
        this.tags = tags;
        this.localtags = localtags;
        this.settings = settings;
        this.handler = handler;
        this.spectra = spectra;
        this.overrides = overrides;
        this.command = "tag";
        this.aliases = new String[]{"t"};
        this.help = "displays a tag; tag commands (try `"+SpConst.PREFIX+"tag help`)";
        this.longhelp = "This command is used to create, edit, and view \"tags\". "
                + "Tags are a method of storing text for easy recollection later. "
                + "Additionally, some scripting-esque elements can be used to provide "
                + "extra functionality. See <"+TagUtil.URL+"> for available elements.";
        this.arguments = new Argument[]{
            new Argument("tagname",Argument.Type.SHORTSTRING,true),
            new Argument("tag arguments",Argument.Type.LONGSTRING,false)
        };
        this.children = new Command[]{
            new TagCreate(),
            new TagCreateGlobal(),
            new TagDelete(),
            new TagEdit(),
            new TagList(),
            new TagOwner(),
            new TagPull(),
            new TagRandom(),
            new TagRaw(),
            new TagRaw2(),
            new TagSearch(),
            
            new TagOverride(),
            
            new TagImport(),
            new TagMirror(),
            new TagMode(),
            new TagUnimport(),
            new TagUnmirror(),
            
            new TagMigrate()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) 
    {
        String tagname = (String)(args[0]);
        String tagargs = args[1]==null ? null :(String)(args[1]);
        boolean local = false;
        boolean nsfw = TagUtil.isNSFWAllowed(event);
        if(!event.isPrivate())
            local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
        String[] tag = spectra.findTag(tagname,event.getGuild(),local,nsfw);
        if(tag==null)
        {
            Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
            return false;
        }
        Sender.sendResponse("\u200B"+TagUtil.parser.clear()
                                        .put("args",tagargs)
                                        .put("user", event.getAuthor())
                                        .put("channel", event.getTextChannel())
                                        .parse(TagUtil.getContents(tag)), event);
        return true;
    }
    
    //subcommands
    private class TagCreate extends Command
    {
        private TagCreate()
        {
            this.command = "create";
            this.aliases= new String[]{"add"};
            this.help = "saves a local tag for later recollection";
            this.longhelp = "This command creates a new local tag for the current guild (if the tag doesn't exist). This "
                    + "tag is owned by the creator, and can only be edited or deleted by that user. "
                    + "Note: Anyone found creating offensive or not-safe-for-work tags is at risk of being "
                    + "blacklisted from using "+SpConst.BOTNAME;
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true,1,80),
                new Argument("tag contents",Argument.Type.LONGSTRING,true)
            };
            this.availableInDM = false;
            this.cooldown = 90;
            this.cooldownKey = (event) -> {return event.getAuthor().getId()+"tagcreate";};
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = (String)(args[1]);
            boolean local = false;
            if(!event.isPrivate())
                local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            String[] tag = spectra.findTag(tagname, event.getGuild(), local, false);
            if(tag==null)//good to make it
            {
                tag = new String[localtags.getSize()];
                tag[LocalTags.OWNERID] = event.getAuthor().getId();
                tag[LocalTags.GUILDID] = event.getGuild().getId();
                tag[LocalTags.TAGNAME] = tagname;
                tag[LocalTags.CONTENTS] = contents;
                localtags.set(tag);
                Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tagname+"\" created for "+event.getGuild().getName()+".", event);
                handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") created local tag **"+tagname+"** ");
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+TagUtil.getTagname(tag)+"\" already exists.", event);
                return false;
            }
        }
    }
    
    private class TagCreateGlobal extends Command
    {
        private TagCreateGlobal()
        {
            this.command = "createglobal";
            this.aliases= new String[]{"addglobal","creategeneric"};
            this.help = "saves a global tag for later recollection";
            this.longhelp = "This command creates a new global tag. This "
                    + "tag is owned by the creator, and can only be edited or deleted by that user. "
                    + "Note: Anyone found creating offensive or not-safe-for-work tags is at risk of "
                    + "being blacklisted from using "+SpConst.BOTNAME;
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true,1,80),
                new Argument("tag contents",Argument.Type.LONGSTRING,true)
            };
            this.cooldown = 360;
            this.cooldownKey = (event) -> {return event.getAuthor().getId()+"tagcreateglobal";};
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = (String)(args[1]);
            String[] tag = spectra.findTag(tagname, event.getGuild(), false, false);
            if(tag==null)//good to make it
            {
                tag = new String[tags.getSize()];
                tag[Tags.OWNERID] = event.getAuthor().getId();
                tag[Tags.TAGNAME] = tagname;
                tag[Tags.CONTENTS] = contents;
                tags.set(tag);
                Sender.sendResponse(SpConst.SUCCESS+"Global tag \""+tagname+"\" created successfully.", event);
                ArrayList<Guild> guildlist = new ArrayList<>();
                event.getJDA().getGuilds().stream().filter((g) -> (g.isMember(event.getAuthor()) || g.getId().equals(SpConst.JAGZONE_ID))).forEach((g) -> {
                    guildlist.add(g);
                });
                handler.submitText(Feeds.Type.TAGLOG, guildlist, 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") created global tag **"+tagname+"** "
                                +(event.isPrivate() ? "in a Direct Message":("on **"+event.getGuild().getName()+"**")));
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+TagUtil.getTagname(tag)+"\" already exists.", event);
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
            this.longhelp = "This command deletes a tag if you own it. If you are a server moderator "
                    + "and don't want a tag to be displayed, consider using `tag override` to \"delete\" it "
                    + "from the current server.";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String[] tag = spectra.findTag(tagname,event.getGuild(),false,true);
            if(tag==null)//nothing to edit
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                return false;
            }
            else if(TagUtil.getOwnerId(tag).equals(event.getAuthor().getId()) || SpConst.JAGROSH_ID.equals(event.getAuthor().getId()))
            {
                boolean global = tag.length==3;
                if(global)
                    tags.removeTag(tag[Tags.TAGNAME]);
                else
                {
                    localtags.removeTag(tag[LocalTags.TAGNAME],tag[LocalTags.GUILDID]);
                }
                Sender.sendResponse(SpConst.SUCCESS+(global ? "Global tag" : "Local tag (*"+event.getJDA().getGuildById(tag[LocalTags.GUILDID]).getName()+"*)")+" \""+TagUtil.getTagname(tag)+"\" deleted successfully.", event);
                ArrayList<Guild> guildlist = new ArrayList<>();
                if(global)
                    event.getJDA().getGuilds().stream().filter((g) -> (g.isMember(event.getAuthor()) || g.getId().equals(SpConst.JAGZONE_ID))).forEach((g) -> {
                        guildlist.add(g);
                    });
                else
                    guildlist.add(event.getJDA().getGuildById(tag[LocalTags.GUILDID]));
                
                handler.submitText(Feeds.Type.TAGLOG, guildlist, 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") deleted "+(tag.length==3 ? "global" : "local")+" tag **"+TagUtil.getTagname(tag)+"** "
                                +(event.isPrivate() ? "in a Direct Message":("on **"+event.getGuild().getName()+"**")));
                return true;
            }
            else
            {
                String owner;
                String ownerid = TagUtil.getOwnerId(tag);
                User u = event.getJDA().getUserById(ownerid);
                if(u!=null)
                    owner = "**"+u.getUsername()+"**";
                else if(tag[Tags.OWNERID].startsWith("g"))
                    owner = "the server *"+event.getJDA().getGuildById(ownerid.substring(1)).getName()+"*";
                else
                    owner = "an unknown user (ID:"+tag[Tags.OWNERID]+")";
                Sender.sendResponse(SpConst.ERROR+"You cannot delete tag \""+TagUtil.getTagname(tag)+"\" because it belongs to "+owner, event);
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
            this.longhelp = "This command edits a tag you own.";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true),
                new Argument("new contents",Argument.Type.LONGSTRING,true)
            };
            this.cooldown = 20;
            this.cooldownKey = (event) -> {return event.getAuthor().getId()+"tagedit";};
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = (String)(args[1]);
            String[] tag = spectra.findTag(tagname, event.getGuild(), false, true);
            if(tag==null)//nothing to edit
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                return false;
            }
            else if(TagUtil.getOwnerId(tag).equals(event.getAuthor().getId()) || SpConst.JAGROSH_ID.equals(event.getAuthor().getId()))
            {
                boolean global = tag.length==3;
                if(global)
                {
                    tagname = tag[Tags.TAGNAME];
                    tags.set(new String[]{
                    tag[Tags.OWNERID],
                    tagname,
                    contents
                    });
                }
                else
                {
                    tagname = tag[LocalTags.TAGNAME];
                    localtags.set(new String[]{
                        tag[LocalTags.OWNERID],
                        tag[LocalTags.GUILDID],
                        tagname,
                        contents
                    });
                }
                
                Sender.sendResponse(SpConst.SUCCESS+(global ? "Global tag" : "Local tag (*"+event.getJDA().getGuildById(tag[LocalTags.GUILDID]).getName()+"*)")+" \""+TagUtil.getTagname(tag)+"\" edited successfully.", event);
                ArrayList<Guild> guildlist = new ArrayList<>();
                if(global)
                    event.getJDA().getGuilds().stream().filter((g) -> (g.isMember(event.getAuthor()) || g.getId().equals(SpConst.JAGZONE_ID))).forEach((g) -> {
                        guildlist.add(g);
                    });
                else
                    guildlist.add(event.getJDA().getGuildById(tag[LocalTags.GUILDID]));
                handler.submitText(Feeds.Type.TAGLOG, guildlist, 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") edited "+(tag.length==3 ? "global" : "local")+" tag **"+TagUtil.getTagname(tag)+"** "
                                +(event.isPrivate() ? "in a Direct Message":("on **"+event.getGuild().getName()+"**")));
                return true;
            }
            else
            {
                String owner;
                String ownerid = TagUtil.getOwnerId(tag);
                User u = event.getJDA().getUserById(ownerid);
                if(u!=null)
                    owner = "**"+u.getUsername()+"**";
                else if(tag[Tags.OWNERID].startsWith("g"))
                    owner = "the server *"+event.getJDA().getGuildById(ownerid.substring(1)).getName()+"*";
                else
                    owner = "an unknown user (ID:"+ownerid+")";
                Sender.sendResponse(SpConst.ERROR+"You cannot edit tag \""+TagUtil.getTagname(tag)+"\" because it belongs to "+owner, event);
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
            this.longhelp = "This command shows the list of tags you have created, or the list "
                    + "a different user has created, if you specify a username.";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("username",Argument.Type.USER,false)
            };
            this.cooldown = 10;
            this.cooldownKey = (event) -> {return event.getAuthor().getId()+"taglist";};
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            User user = (User)(args[0]);
            if(user==null)
                user = event.getAuthor();
            boolean nsfw = TagUtil.isNSFWAllowed(event);
            ArrayList<String> taglist = tags.findTagsByOwner(user, nsfw);
            Collections.sort(taglist);
            StringBuilder builder1;
            builder1 = new StringBuilder();
            builder1.append(SpConst.SUCCESS).append(taglist.size()).append(" global tags owned by **").append(user.getUsername()).append("**:\n");
            taglist.stream().forEach((tag) -> builder1.append(tag).append(" "));
            StringBuilder builder2 = new StringBuilder();
            if(!event.isPrivate())
            {
                ArrayList<String> localtaglist = localtags.findTagsByOwner(user, event.getGuild(), nsfw);
                if(localtaglist.size()>0)
                {
                    Collections.sort(localtaglist);
                    builder2.append("\n" + SpConst.SUCCESS).append(localtaglist.size()).append(" tags on *").append(event.getGuild().getName()).append("*:\n");
                    localtaglist.stream().forEach((tag) -> builder2.append(tag).append(" "));
                }
                String[] mirrors = Settings.tagMirrorsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMIRRORS]);
                for(String guildid: mirrors)
                {
                    Guild g = event.getJDA().getGuildById(guildid);
                    if(g==null)
                        continue;
                    ArrayList<String> mirrortaglist = localtags.findTagsByOwner(user, g, nsfw);
                    if(mirrortaglist.size()>0)
                    {
                        Collections.sort(mirrortaglist);
                        builder2.append("\n" + SpConst.SUCCESS).append(mirrortaglist.size()).append(" tags on *").append(g.getName()).append("*:\n");
                        mirrortaglist.stream().forEach((tag) -> builder2.append(tag).append(" "));
                    }
                }
            }
            
            Sender.sendResponse(((taglist.size()>0||builder2.length()==0 ? builder1.toString() : "")+builder2.toString()).trim(), event);
            return true;
        }
    }
    
    private class TagOwner extends Command
    {
        private TagOwner()
        {
            this.command = "owner";
            this.help = "shows the owner of a tag";
            this.longhelp = "This command shows who the owner of a tag is. If a tag is "
                    + "overriden, the tag will appear to be \"owned\" by the server.";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String[] tag = spectra.findTag(tagname, event.getGuild(), false, true);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                return false;
            }
            String owner;
            String ownerid = TagUtil.getOwnerId(tag);
            User u = event.getJDA().getUserById(ownerid);
            if(u!=null)
                owner = "**"+u.getUsername()+"** #"+u.getDiscriminator();
            else if(ownerid.startsWith("g"))
                owner = "the server";
            else
                owner = "an unknown user (ID:"+tag[Tags.OWNERID]+")";
            String tagtype = "Global tag";
            if(tag.length==4)
                tagtype = "Local tag (*"+event.getJDA().getGuildById(tag[LocalTags.GUILDID]).getName()+"*)";
            Sender.sendResponse(tagtype+" \""+TagUtil.getTagname(tag)+"\" belongs to "+owner, event);
            return true;
        }
    }
    
    private class TagRandom extends Command
    {
        private TagRandom()
        {
            this.command = "random";
            this.help = "shows a random tag";
            this.longhelp = "This command brings up a random tag from all of the existing "
                    + "tags (or all the tags by users on the server in local mode).";
            //this.longhelp = "";
            this.arguments = new Argument[]{
            new Argument("tag arguments",Argument.Type.LONGSTRING,false)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagargs = args[0]==null?null:(String)(args[0]);
            boolean local = false;
            boolean nsfw = TagUtil.isNSFWAllowed(event);
            if(!event.isPrivate())
                local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            List<String[]> taglist = tags.findTags(null, event.getGuild(), local, nsfw);
            List<String[]> taglist2;
            if(!event.isPrivate())
                taglist2 = localtags.findTags(null, event.getGuild(), nsfw);
            else
                taglist2 = new ArrayList<>();
            if(taglist.isEmpty() && taglist2.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"No tags found!", event);
                return false;
            }
            int num = (int)( Math.random() * (taglist.size()+taglist2.size()) );
            String[] tag = num<taglist.size() ? taglist.get(num) : taglist2.get(num - taglist.size());
            Sender.sendResponse("Tag \""+TagUtil.getTagname(tag)+"\":\n"
                    +TagUtil.parser.clear()
                            .put("args", tagargs)
                            .put("user", event.getAuthor())
                            .put("channel", event.getTextChannel())
                            .parse(TagUtil.getContents(tag)),
                    event);
            return true;
        }
    }
    
    private class TagRaw extends Command
    {
        private TagRaw()
        {
            this.command = "raw";
            this.help = "shows the raw (non-dynamic) tag text";
            this.longhelp = "This command shows the contents of a tag without "
                    + "interpretting any of the scripting.";
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
            boolean nsfw = TagUtil.isNSFWAllowed(event);
            if(!event.isPrivate())
                local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            String[] tag = spectra.findTag(tagname,event.getGuild(),local,nsfw);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                return false;
            }
            Sender.sendResponse("\u200B"+TagUtil.getContents(tag), event);
            return true;
        }
    }
    
    private class TagRaw2 extends Command
    {
        private TagRaw2()
        {
            this.command = "raw2";
            this.aliases = new String[]{"code"};
            this.help = "shows the raw tag text in a code block";
            this.longhelp = "This command shows the contents of a tag without "
                    + "interpretting any of the scripting. It also puts it in a "
                    + "code block to escape most formatting.";
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
            boolean nsfw = TagUtil.isNSFWAllowed(event);
            if(!event.isPrivate())
                local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            String[] tag = spectra.findTag(tagname,event.getGuild(),local,nsfw);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                return false;
            }
            Sender.sendResponse("```\n"+TagUtil.getContents(tag)+"```", event);
            return true;
        }
    }
    
    private class TagSearch extends Command
    {
        private TagSearch()
        {
            this.command = "search";
            this.help = "searches for tags that include the given query";
            this.longhelp = "This command searches for tag names that contain the "
                    + "given query. If no query is provided, the full tag list will be "
                    + "returned.";
            //this.longhelp = "";
            this.arguments = new Argument[]{
            new Argument("query",Argument.Type.SHORTSTRING,false)
            };
            this.cooldown = 10;
            this.cooldownKey = (event) -> {return event.getAuthor().getId()+"tagsearch";};
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String query = args[0]==null?null:(String)(args[0]);
            boolean local = false;
            boolean nsfw = TagUtil.isNSFWAllowed(event);
            if(!event.isPrivate())
                local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            List<String> tagnames = tags.findTags(query, event.getGuild(), local, nsfw).stream().map(tag -> tag[Tags.TAGNAME]).collect(Collectors.toList());
            if(!event.isPrivate())
            {
                tagnames.addAll(localtags.findTags(query, event.getGuild(), nsfw).stream().map(tag -> tag[LocalTags.TAGNAME]).collect(Collectors.toList()));
                String[] mirrors = Settings.tagMirrorsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMIRRORS]);
                for(String guildid: mirrors)
                {
                    Guild g = event.getJDA().getGuildById(guildid);
                    if(g==null)
                        continue;
                    tagnames.addAll(localtags.findTags(query, g, nsfw).stream().map(tag -> tag[LocalTags.TAGNAME]).collect(Collectors.toList()));
                }
            }
            
            if(tagnames.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"No tags found matching \""+query+"\"!", event);
                return false;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS).append(tagnames.size()).append(" tags found");
            if(query!=null)
                builder.append(" containing \"").append(query).append("\"");
            builder.append(":\n");
            if(tagnames.size()<200)
            {
                Collections.sort(tagnames);
            }
            tagnames.stream().distinct().forEach((tag) -> builder.append(tag).append(" "));
            Sender.sendResponse(builder.toString(),event);
            return true;
        }
    }
    
    private class TagOverride extends Command
    {
        private TagOverride()
        {
            this.command = "override";
            this.help = "creates an override for the current server";
            this.longhelp = "This command overrides or deletes local tags on the server. This can be used "
                    + "to \"disable\" unwanted tags, or to use an existing tag for something else.";
            //this.longhelp = "";
            this.arguments = new Argument[]{
                new Argument("tagname",Argument.Type.SHORTSTRING,true),
                new Argument("new contents",Argument.Type.LONGSTRING,false)
            };
            this.level = PermLevel.MODERATOR;
            this.availableInDM = false;
            this.children = new Command[]{
                new TagOverrideList()
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event)
        {
            String tagname = (String)(args[0]);
            String contents = args[1]==null?null:(String)(args[1]);
            if(contents==null)
                contents = "This tag was removed by **"+event.getAuthor().getUsername()+"**";
            boolean local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
            String[] tag = spectra.findTag(tagname, event.getGuild(), local, false);
            if(tag==null)//nothing to edit
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                return false;
            }
            else
            {
                if(tag.length==3)//global
                {
                    String[] newtag = new String[localtags.getSize()];
                    newtag[LocalTags.GUILDID] = event.getGuild().getId();
                    newtag[LocalTags.OWNERID] = "g"+event.getGuild().getId();
                    newtag[LocalTags.TAGNAME] = tagname;
                    newtag[LocalTags.CONTENTS] = contents;
                    localtags.set(newtag);
                    Sender.sendResponse(SpConst.SUCCESS+"Global tag \""+TagUtil.getTagname(tag)+"\" overriden successfully.", event);
                    handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") overrode global tag **"+TagUtil.getTagname(tag)+"**");
                    return true;
                }
                else
                {
                    if(tag[LocalTags.GUILDID].equals(event.getGuild().getId()) && args[1]==null)//delete
                    {
                        localtags.removeTag(tagname, event.getGuild().getId());
                        Sender.sendResponse(SpConst.SUCCESS+"Local tag \""+TagUtil.getTagname(tag)+"\" deleted.", event);
                        handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                            "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") deleted local tag **"+TagUtil.getTagname(tag)+"**");
                        return true;
                    }
                    else
                    {
                        String[] newtag = new String[localtags.getSize()];
                        newtag[LocalTags.GUILDID] = event.getGuild().getId();
                        newtag[LocalTags.OWNERID] = "g"+event.getGuild().getId();
                        newtag[LocalTags.TAGNAME] = tag[LocalTags.TAGNAME];
                        newtag[LocalTags.CONTENTS] = contents;
                        localtags.set(newtag);
                        Sender.sendResponse(SpConst.SUCCESS+"Local tag \""+TagUtil.getTagname(tag)+"\" overriden successfully.", event);
                        handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                            "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") overrode local tag **"+TagUtil.getTagname(tag)+"**");
                        return true;
                    }
                }
            }
        }
        
        private class TagOverrideList extends Command
        {
            private TagOverrideList()
            {
                this.command = "list";
                this.help = "lists tag overrides on the current server";
                this.longhelp = "This command shows which tags have been overridden on the server.";
                this.level = PermLevel.MODERATOR;
                this.availableInDM = false;
            }
            @Override
            protected boolean execute(Object[] args, MessageReceivedEvent event)
            {
                List<String> list = localtags.findTagsByGuild(event.getGuild(), true);
                if(list.isEmpty())
                    Sender.sendResponse(SpConst.WARNING+"No tags have been overriden on **"+event.getGuild().getName()+"**", event);
                else
                {
                    Collections.sort(list);
                    StringBuilder builder = new StringBuilder(SpConst.SUCCESS+list.size()+" tag overrides on **"+event.getGuild().getName()+"**:\n");
                    list.stream().forEach((tag) -> {
                        builder.append(tag).append(" ");
                    });
                    Sender.sendResponse(builder.toString(), event);
                }
                return true;
            }
        }
    }
    
    /*private class TagRestore extends Command
    {
        private TagRestore()
            {
                this.command = "restore";
                this.help = "restores a tag from overriden state";
                this.longhelp = "This command restores a tag from being overridden.";
                this.arguments = new Argument[]{
                    new Argument("tagname",Argument.Type.SHORTSTRING,true),
                };
                this.level = PermLevel.MODERATOR;
                this.availableInDM = false;
            }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String tagname = (String)(args[0]);
            String[] tag = overrides.findTag(event.getGuild(), tagname, true);
            if(tag==null)
            {
                Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" is not currently overriden", event);
                return false;
            }
            else
            {
                tagname = tag[Tags.TAGNAME];
                overrides.removeTag(tag);
                Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tag[Tags.TAGNAME]+"\" restored successfully.", event);
                handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") restored tag **"+tagname
                                +"** on **"+event.getGuild().getName()+"**");
                return true;
            }
        }
    }*/
    
    private class TagMode extends Command 
    {
        private TagMode()
            {
                this.command = "mode";
                this.help = "sets the tag mode to local or global";
                this.longhelp = "This command sets the tag mode. In global mode, all existing tags are "
                        + "available on the server. In local mode, the only available tags will be those that "
                        + "were created by users on the server.";
                this.arguments = new Argument[]{
                    new Argument("mode",Argument.Type.SHORTSTRING,true),
                };
                this.level = PermLevel.ADMIN;
                this.availableInDM = false;
            }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String mode = (String)(args[0]);
            if(mode.equalsIgnoreCase("local") || mode.equalsIgnoreCase("global"))
            {
                settings.setSetting(event.getGuild().getId(), Settings.TAGMODE, mode.toUpperCase());
                Sender.sendResponse(SpConst.SUCCESS+"Tag mode has been set to `"+mode.toUpperCase()+"`", event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Valid tag modes are `LOCAL` and `GLOBAL`", event);
                return false;
            }
        }
    }
    
    private class TagImport extends Command
    {
        private TagImport()
            {
                this.command = "import";
                this.help = "imports a tag from a tag command";
                this.longhelp = "This command imports a tag as a psuedo-command on the server. "
                        + "For example, if the tag `hug` was imported, you would be able to use `"
                        + SpConst.PREFIX+"hug` instead of `"+SpConst.PREFIX+"tag hug`";
                this.arguments = new Argument[]{
                    new Argument("tagname",Argument.Type.SHORTSTRING,true),
                };
                this.children = new Command[]{
                    new TagImportList()
                };
                this.level = PermLevel.ADMIN;
                this.availableInDM = false;
            }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String tagname = (String)(args[0]);
            String[] imports = Settings.tagCommandsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGIMPORTS]);
            boolean found = false;
            for(String tag : imports)
                if(tag.equalsIgnoreCase(tagname))
                    found = true;
            if(!found)
            {
                boolean local = "local".equalsIgnoreCase(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMODE]);
                String[] tag = spectra.findTag(tagname, event.getGuild(), local, true);
                if(tag==null)//nothing to import
                {
                    Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" could not be found", event);
                    return false;
                }
                else
                {
                    tagname = TagUtil.getTagname(tag);
                    String cmds = settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGIMPORTS];
                    if(cmds==null || cmds.equals(""))
                        cmds = tagname;
                    else
                        cmds+=" "+tagname;
                    settings.setSetting(event.getGuild().getId(), Settings.TAGIMPORTS, cmds);
                    Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tagname+"\" has been added a tag command", event);
                    handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") imported tag **"+tagname
                                +"** on **"+event.getGuild().getName()+"**");
                    return true;
                }
            }
            Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" is already a tag command!", event);
            return false;
        }
        private class TagImportList extends Command
        {
            private TagImportList()
            {
                this.command = "list";
                this.help = "lists tag imports on the current server";
                this.longhelp = "This command shows the list of tags that have been imported to the server as commands. "
                        + "There are some tags that are imported by default; you can unimport these if you wish.";
                this.level = PermLevel.MODERATOR;
                this.availableInDM = false;
            }
            @Override
            protected boolean execute(Object[] args, MessageReceivedEvent event)
            {
                String importlist= settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGIMPORTS];
                if(importlist==null || importlist.equals(""))
                {
                    Sender.sendResponse(SpConst.WARNING+"No tags have been imported on **"+event.getGuild().getName()+"**", event);
                    return true;
                }
                String[] imports = importlist.split("\\s+");
                ArrayList<String> list = new ArrayList<>(Arrays.asList(imports));
                Collections.sort(list);
                StringBuilder builder = new StringBuilder(SpConst.SUCCESS+list.size()+" tag imports on **"+event.getGuild().getName()+"**:\n");
                list.stream().forEach((tag) -> {
                    builder.append(tag).append(" ");
                });
                Sender.sendResponse(builder.toString(), event);
                return true;
            }
        }
    }
    
    private class TagUnimport extends Command
    {
        private TagUnimport()
            {
                this.command = "unimport";
                this.help = "un-imports a tag from a tag command";
                this.longhelp = "This command un-imports a tag from being a pseudo-command.";
                this.arguments = new Argument[]{
                    new Argument("tagname",Argument.Type.SHORTSTRING,true),
                };
                this.level = PermLevel.ADMIN;
                this.availableInDM = false;
            }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String tagname = (String)(args[0]);
            String[] imports = Settings.tagCommandsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGIMPORTS]);
            boolean found = false;
            StringBuilder builder = new StringBuilder();
            for(String tag : imports)
            {
                if(tag.equalsIgnoreCase(tagname))
                {
                    found = true;
                    tagname = tag;
                }
                else
                    builder.append(tag).append(" ");
            }
            if(found)
            {
                settings.setSetting(event.getGuild().getId(), Settings.TAGIMPORTS, builder.toString().trim());
                Sender.sendResponse(SpConst.SUCCESS+"Tag \""+tagname+"\" is no longer a tag command", event);
                handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") unimported tag **"+tagname
                                +"** on **"+event.getGuild().getName()+"**");
                return true;
            }
            Sender.sendResponse(SpConst.ERROR+"Tag \""+tagname+"\" is not currently a tag command!", event);
            return false;
        }
    }
    
    private class TagMirror extends Command
    {
        private TagMirror()
            {
                this.command = "mirror";
                this.help = "allows the curent server to use all local tags from the provided server";
                this.longhelp = "This command allows the current server to \"mirror\" all tags from the provided server. "
                        + "This means that calling a tag here can use the version from any mirrored servers if the local version "
                        + "does not exist for this server.";
                this.arguments = new Argument[]{
                    new Argument("servername",Argument.Type.GUILD,true),
                };
                this.children = new Command[]{
                    new TagMirrorList()
                };
                this.level = PermLevel.ADMIN;
                this.availableInDM = false;
            }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Guild mirror = (Guild)(args[0]);
            if(mirror.equals(event.getGuild()))
            {
                Sender.sendResponse(SpConst.ERROR+"You can't mirror the current server!", event);
                return false;
            }
            String[] mirrors = Settings.tagMirrorsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMIRRORS]);
            boolean found = false;
            for(String mid : mirrors)
                if(mid.equalsIgnoreCase(mirror.getId()))
                    found = true;
            if(!found)
            {
                String mrrs = settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMIRRORS];
                if(mrrs==null || mrrs.equals(""))
                    mrrs = mirror.getId();
                else
                    mrrs+=" "+mirror.getId();
                settings.setSetting(event.getGuild().getId(), Settings.TAGMIRRORS, mrrs);
                Sender.sendResponse(SpConst.SUCCESS+"Now mirroring tags from **"+mirror.getName()+"**", event);
                handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") added the server **"+mirror.getName()
                        +"** as a tag mirror.");
                return true;
            }
            Sender.sendResponse(SpConst.ERROR+"Server **"+mirror.getName()+"** is already mirrored!", event);
            return false;
        }
        private class TagMirrorList extends Command
        {
            private TagMirrorList()
            {
                this.command = "list";
                this.help = "lists tag mirrors on the current server";
                this.longhelp = "This command shows the list of servers that have their tags mirrored onto the server.";
                this.level = PermLevel.ADMIN;
                this.availableInDM = false;
            }
            @Override
            protected boolean execute(Object[] args, MessageReceivedEvent event)
            {
                String[] mirrorlist= Settings.tagMirrorsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMIRRORS]);
                if(mirrorlist.length==0)
                {
                    Sender.sendResponse(SpConst.WARNING+"No tag mirrors have been set up on **"+event.getGuild().getName()+"**", event);
                    return true;
                }
                StringBuilder builder = new StringBuilder(SpConst.SUCCESS+mirrorlist.length+" tag mirrors on **"+event.getGuild().getName()+"**:");
                for(String id: mirrorlist)
                {
                    Guild g = event.getJDA().getGuildById(id);
                    if(g==null)
                        continue;
                    builder.append("\n").append(g.getName());
                }
                Sender.sendResponse(builder.toString(), event);
                return true;
            }
        }
    }
    
    private class TagUnmirror extends Command
    {
        private TagUnmirror()
            {
                this.command = "unmirror";
                this.help = "un-mirrors a server";
                this.longhelp = "This command un-mirrors a server, so its tags are no longer available on the current server.";
                this.arguments = new Argument[]{
                    new Argument("servername",Argument.Type.GUILD,true),
                };
                this.level = PermLevel.ADMIN;
                this.availableInDM = false;
            }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            Guild mirror = (Guild)(args[0]);
            String[] mirrorlist= Settings.tagMirrorsFromList(settings.getSettingsForGuild(event.getGuild().getId())[Settings.TAGMIRRORS]);
            boolean found = false;
            StringBuilder builder = new StringBuilder();
            for(String gid : mirrorlist)
            {
                if(gid.equalsIgnoreCase(mirror.getId()))
                    found = true;
                else if(event.getJDA().getGuildById(gid)!=null)
                    builder.append(gid).append(" ");
            }
            if(found)
            {
                settings.setSetting(event.getGuild().getId(), Settings.TAGMIRRORS, builder.toString().trim());
                Sender.sendResponse(SpConst.SUCCESS+"Server **"+mirror.getName()+"** is no longer mirrored", event);
                handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") removed the server **"+mirror.getName()
                                +"** from tag mirrors.");
                return true;
            }
            Sender.sendResponse(SpConst.ERROR+"The server **"+mirror.getName()+"** is not currently mirrored!", event);
            return false;
        }
    }
    
    private class TagPull extends Command
    {
        private TagPull()
        {
            this.command = "pull";
            this.help = "converts global tags to local tags";
            this.longhelp = "Converts all tags in the given list (that belong to the user) from global tags to local tags for the current server.";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("tag(s)",Argument.Type.LONGSTRING,true)
            };
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String[] taglist = ((String)(args[0])).split("\\s+");
            StringBuilder success = new StringBuilder();
            StringBuilder notFound = new StringBuilder();
            StringBuilder notOwn = new StringBuilder();
            StringBuilder failure = new StringBuilder();
            for(String tagname: taglist)
            {
                String[] tag = tags.findTag(tagname, null, false, true);
                if(tag==null)
                    notFound.append(tagname).append(" ");
                else if(!tag[Tags.OWNERID].equals(event.getAuthor().getId()))
                    notOwn.append(tag[Tags.TAGNAME]).append(" ");
                else if(localtags.findTag(tagname, event.getGuild(), true)!=null)
                    failure.append(tag[Tags.TAGNAME]).append(" ");
                else
                {
                    String[] newTag = new String[localtags.getSize()];
                    newTag[LocalTags.TAGNAME] = tag[Tags.TAGNAME];
                    newTag[LocalTags.OWNERID] = tag[Tags.OWNERID];
                    newTag[LocalTags.GUILDID] = event.getGuild().getId();
                    newTag[LocalTags.CONTENTS] = tag[Tags.CONTENTS];
                    localtags.set(newTag);
                    tags.removeTag(tagname);
                    success.append(tag[Tags.TAGNAME]).append(" ");
                }
            }
            String out = "";
            if(success.length()>0)
                out+=SpConst.SUCCESS+"The following tags were successfully pulled to **"+event.getGuild().getName()+"**:\n"+success.toString();
            if(notFound.length()>0)
                out+="\n\n"+SpConst.WARNING+"The following tags were not found:\n"+notFound.toString();
            if(notOwn.length()>0)
                out+="\n\n"+SpConst.ERROR+"You do not own, and cannot pull, the following tags:\n"+notOwn.toString();
            if(failure.length()>0)
                out+="\n\n"+SpConst.ERROR+"The following tags already exist on the server:\n"+failure.toString();
            Sender.sendResponse(out.trim(), event);
            if(success.length()>0)
                handler.submitText(Feeds.Type.TAGLOG, event.getGuild(), 
                        "\uD83C\uDFF7 **"+event.getAuthor().getUsername()+"** (ID:"+event.getAuthor().getId()+") pulled tags:\n"+success.toString());
            return true;
        }
    }
    
    private class TagMigrate extends Command
    {
        private TagMigrate()
        {
            this.command = "migrate";
            this.help = "mass-transfers tags";
            this.longhelp = "This command converts all tag overrides to local tags, and then attempts to pull tags for users that only have 1 shared server.";
            this.level = PermLevel.JAGROSH;
        }

        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            int total = overrides.allTags().size();
            overrides.allTags().stream().forEach((tag)->{
                Guild guild = event.getJDA().getGuildById(tag[Overrides.OWNERID].substring(1));
                if(guild!=null && localtags.findTag(tag[Overrides.TAGNAME], guild, true)==null)
                {
                    String[] newTag = new String[localtags.getSize()];
                    newTag[LocalTags.TAGNAME] = tag[Overrides.TAGNAME];
                    newTag[LocalTags.OWNERID] = tag[Overrides.OWNERID];
                    newTag[LocalTags.GUILDID] = guild.getId();
                    newTag[LocalTags.CONTENTS] = tag[Overrides.CONTENTS];
                    overrides.removeTag(tag);
                    localtags.set(newTag);
                }
            });
            int remaining = overrides.allTags().size();
            Sender.sendResponse(SpConst.SUCCESS+"**"+(total-remaining)+"** overrides migrated (**"+remaining+"** remaining)", event);
            ArrayList<String[]> allTags = tags.findTags(null, null, false, true);
            event.getJDA().getUsers().stream().forEach((u) -> {
                Guild single = null;
                int count = 0;
                for(Guild g: event.getJDA().getGuilds())
                    if(g.isMember(u))
                    {
                        single = g;
                        count++;
                    }
                if (count==1 && single!=null) {
                    for(String[] tag : allTags)
                        if(tag[Tags.OWNERID].equals(u.getId()))
                        {
                            String[] newTag = new String[localtags.getSize()];
                            newTag[LocalTags.TAGNAME] = tag[Tags.TAGNAME];
                            newTag[LocalTags.OWNERID] = tag[Tags.OWNERID];
                            newTag[LocalTags.GUILDID] = single.getId();
                            newTag[LocalTags.CONTENTS] = tag[Tags.CONTENTS];
                            tags.removeTag(tag[Tags.TAGNAME]);
                            localtags.set(newTag);
                        }
                }
            });
            int count = allTags.size() - tags.findTags(null, null, false, true).size();
            Sender.sendResponse(SpConst.SUCCESS+"Migrated **"+count+"** tags from global to local", event);
            return true;
        }
    }
    
}
