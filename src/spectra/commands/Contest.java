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

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Contests;
import spectra.datasources.Entries;
import spectra.entities.Tuple;
import spectra.utils.FormatUtil;
import spectra.utils.OtherUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Contest extends Command {
    private final Contests contests;
    private final Entries entries;
    public Contest(Contests contests, Entries entries)
    {
        this.contests = contests;
        this.entries = entries;
        this.command = "contest";
        this.help = "commands for automated contests";
        this.availableInDM = false;
        this.children = new Command[]{
            new ContestList(),
            new ContestRules(),
            new ContestSubmit(),
            new ContestTime(),
            
            new ContestCreate(),
            new ContestEdit(),
            new ContestGetentries()
        };
        this.arguments = new Argument[]{
            new Argument("subcommand",Argument.Type.SHORTSTRING,true)
        };
    }
    
    private class ContestList extends Command
    {
        private ContestList()
        {
            this.command = "list";
            this.help = "lists contests on the server";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("current|ended|upcoming",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String type = (String)args[0];
            if(type.equalsIgnoreCase("current"))
            {
                List<String[]> list = contests.getCurrentContestsForGuild(event.getGuild().getId());
                if(list.isEmpty())
                {
                    Sender.sendResponse(SpConst.WARNING+"There are currently no contests on **"+event.getGuild().getName()+"**", event);
                    return false;
                }
                StringBuilder builder = new StringBuilder("\uD83C\uDFC6 Current contests on **"+event.getGuild().getName()+"**:");
                long now = OffsetDateTime.now().toInstant().toEpochMilli();
                for(String[] contest : list)
                {
                    builder.append("\n").append(contest[Contests.CONTESTNAME])
                            .append(" (ends in ")
                            .append(FormatUtil.secondsToTime((Long.parseLong(contest[Contests.ENDTIME])-now)/1000)).append(")");
                }
                Sender.sendResponse(builder.toString(), event);
                return true;
            }
            else if (type.equalsIgnoreCase("upcoming"))
            {
                List<String[]> list = contests.getCurrentContestsForGuild(event.getGuild().getId());
                if(list.isEmpty())
                {
                    Sender.sendResponse(SpConst.WARNING+"There are no upcoming contests on **"+event.getGuild().getName()+"**", event);
                    return false;
                }
                StringBuilder builder = new StringBuilder("\uD83C\uDFC6 Upcoming contests on **"+event.getGuild().getName()+"**:");
                long now = OffsetDateTime.now().toInstant().toEpochMilli();
                for(String[] contest : list)
                {
                    builder.append("\n").append(contest[Contests.CONTESTNAME])
                            .append(" (starts in ")
                            .append(FormatUtil.secondsToTime((Long.parseLong(contest[Contests.STARTTIME])-now)/1000)).append(")");
                }
                Sender.sendResponse(builder.toString(), event);
                return true;
            }
            else if (type.equalsIgnoreCase("ended"))
            {
                List<String[]> list = contests.getCurrentContestsForGuild(event.getGuild().getId());
                if(list.isEmpty())
                {
                    Sender.sendResponse(SpConst.WARNING+"There are no ended contests on **"+event.getGuild().getName()+"**", event);
                    return false;
                }
                StringBuilder builder = new StringBuilder("\uD83C\uDFC6 Ended contests on **"+event.getGuild().getName()+"**:");
                long now = OffsetDateTime.now().toInstant().toEpochMilli();
                for(String[] contest : list)
                {
                    builder.append("\n").append(contest[Contests.CONTESTNAME])
                            .append(" (ended ")
                            .append(FormatUtil.secondsToTime((now-Long.parseLong(contest[Contests.ENDTIME]))/1000)).append(" ago)");
                }
                Sender.sendResponse(builder.toString(), event);
                return true;
            }
            else
            {
                Sender.sendResponse(SpConst.ERROR+"Valid lists are `current`, `upcoming`, and `ended`", event);
                return false;
            }
        }
    }
    
    private class ContestRules extends Command
    {
        private ContestRules()
        {
            this.command = "rules";
            this.help = "shows the rules of a contest";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            String[] contest = contests.getContest(event.getGuild().getId(), name);
            if(contest==null)
            {
                Sender.sendResponse(SpConst.ERROR+"No contest \""+name+"\" found on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            Sender.sendResponse("Rules for **"+contest[Contests.CONTESTNAME]+"**:\n"+contest[Contests.DETAILS], event);
            return true;
        }
    }
    
    private class ContestTime extends Command
    {
        private ContestTime()
        {
            this.command = "time";
            this.help = "shows the time remaining in a contest";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            String[] contest = contests.getContest(event.getGuild().getId(), name);
            if(contest==null)
            {
                Sender.sendResponse(SpConst.ERROR+"No contest \""+name+"\" found on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            long now = OffsetDateTime.now().toInstant().toEpochMilli();
            long start = Long.parseLong(contest[Contests.STARTTIME]);
            long end = Long.parseLong(contest[Contests.ENDTIME]);
            if(now<start)
                Sender.sendResponse("Contest **"+contest[Contests.CONTESTNAME]+"** starts in "+FormatUtil.secondsToTime((start-now)/1000), event);
            else if (now<end)
                Sender.sendResponse("Contest **"+contest[Contests.CONTESTNAME]+"** ends in "+FormatUtil.secondsToTime((end-now)/1000), event);
            else
                Sender.sendResponse("Contest **"+contest[Contests.CONTESTNAME]+"** ended "+FormatUtil.secondsToTime((now-end)/1000)+" ago", event);
            return true;
        }
    }
    
    private class ContestSubmit extends Command
    {
        private ContestSubmit()
        {
            this.command = "submit";
            this.help = "submits an entry to a contest";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true),
                new Argument("submission", Argument.Type.LONGSTRING, true)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            String submit = (String)args[1];
            String[] contest = contests.getContest(event.getGuild().getId(), name);
            if(contest==null)
            {
                Sender.sendResponse(SpConst.ERROR+"No contest \""+name+"\" found on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            if(!Contests.isLive(contest))
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot make a submission because \""+contest[Contests.CONTESTNAME]+"\" is not currently running.", event);
                return false;
            }
            String[] submission = new String[4];
            submission[Entries.CONTESTNAME] = contest[Contests.CONTESTNAME];
            submission[Entries.ENTRY] = submit;
            submission[Entries.SERVERID] = contest[Contests.SERVERID];
            submission[Entries.USERID] = event.getAuthor().getId();
            entries.set(submission);
            Sender.sendResponse(SpConst.SUCCESS+"Your entry has been received. Thank you!", event);
            return true;
        }
    }
    
    private class ContestGetentries extends Command
    {
        private ContestGetentries()
        {
            this.command = "getentries";
            this.help = "gets the current entries for a contest";
            this.availableInDM = false;
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true)
            };
            this.requiredPermissions = new Permission[]{
                Permission.MESSAGE_ATTACH_FILES
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            String[] contest = contests.getContest(event.getGuild().getId(), name);
            if(contest==null)
            {
                Sender.sendResponse(SpConst.ERROR+"No contest \""+name+"\" found on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            List<String[]> list = entries.getEntries(event.getGuild().getId(), contest[Contests.CONTESTNAME]);
            if(list.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"There are no entries for "+contest[Contests.CONTESTNAME], event);
                return false;
            }
            Sender.sendFileResponse(()->{
                StringBuilder builder = new StringBuilder("-- Entries for "+contest[Contests.CONTESTNAME]+" --");
                list.stream().forEach((entry) -> {
                    builder.append("\n\n")
                            .append(event.getJDA().getUserById(entry[Entries.USERID])==null ? "AN UNKNOWN USER" : event.getJDA().getUserById(entry[Entries.USERID]).getUsername())
                            .append(" (ID:").append(entry[Entries.USERID]).append(") : ").append(entry[Entries.ENTRY]);
                });
                File f = OtherUtil.writeArchive(builder.toString(), contest[Contests.CONTESTNAME]+"_entries.txt");
                return new Tuple<>("Entries for **"+contest[Contests.CONTESTNAME]+"**",f);
            }, event);
            return true;
        }
    }
    
    private class ContestCreate extends Command
    {
        private ContestCreate()
        {
            this.command = "create";
            this.aliases = new String[]{"add"};
            this.availableInDM = false;
            this.help = "creates a new contest";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true),
                new Argument("time until start",Argument.Type.TIME,true,0,2592000),
                new Argument("| length of contest",Argument.Type.TIME,true,0,31536000),
                new Argument("rules",Argument.Type.LONGSTRING,true)
            };
            this.separatorRegex = "\\s+\\|\\s+";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            long secTilStart = (long)args[1];
            long secLength = (long)args[2];
            String rules = (String)args[3];
            String[] contest = contests.getContest(event.getGuild().getId(), name);
            if(contest!=null)
            {
                Sender.sendResponse(SpConst.ERROR+"A contest with that name already exists!", event);
                return false;
            }
            contest = new String[7];
            contest[Contests.CHANNELID] = event.getTextChannel().getId();
            contest[Contests.CONTESTNAME] = name;
            contest[Contests.DETAILS] = rules;
            contest[Contests.SERVERID] = event.getGuild().getId();
            contest[Contests.STATUS] = "0";
            long start = event.getMessage().getTime().toInstant().toEpochMilli()+(secTilStart*1000);
            contest[Contests.STARTTIME] = start+"";
            contest[Contests.ENDTIME] = (start+(secLength*1000))+"";
            contests.set(contest);
            Sender.sendResponse(SpConst.SUCCESS+"Contest \""+name+"\" set to start in "+FormatUtil.secondsToTime(secTilStart), event);
            return true;
        }
    }
    
    private class ContestEdit extends Command
    {
        private ContestEdit()
        {
            this.command = "edit";
            this.availableInDM = false;
            this.help = "edits an existing contest";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true),
                new Argument("change in starttime",Argument.Type.TIME,true),
                new Argument("| change in endtime",Argument.Type.TIME,true),
                new Argument("rules",Argument.Type.LONGSTRING,false)
            };
            this.separatorRegex = "\\s+\\|\\s+";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            String name = (String)args[0];
            long changeInStart = (long)args[1];
            long changeInEnd = (long)args[2];
            String rules = args[3]==null ? null : (String)args[3];
            String[] contest = contests.getContest(event.getGuild().getId(), name);
            if(contest==null)
            {
                Sender.sendResponse(SpConst.ERROR+"No contest \""+name+"\" found on **"+event.getGuild().getName()+"**", event);
                return false;
            }
            //contest[Contests.CHANNELID] = event.getTextChannel().getId();
            //contest[Contests.CONTESTNAME] = name;
            if(rules!=null)
                contest[Contests.DETAILS] = rules;
            //contest[Contests.SERVERID] = event.getGuild().getId();
            //contest[Contests.STATUS] = "0";
            if(changeInStart!=0)
                contest[Contests.STARTTIME] = (Long.parseLong(contest[Contests.STARTTIME])+(changeInStart*1000))+"";
            if(changeInEnd!=0)
                contest[Contests.ENDTIME] = (Long.parseLong(contest[Contests.ENDTIME])+(changeInStart*1000))+"";
            contests.set(contest);
            Sender.sendResponse(SpConst.SUCCESS+"Contest \""+name+"\" edited.", event);
            return true;
        }
    }
}           