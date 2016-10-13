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
import javafx.util.Pair;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Contests;
import spectra.datasources.Entries;
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
        this.longhelp = "This command is the hub for the contest commands. "+SpConst.BOTNAME+" "
                + "Contests are automated competitions. They are created with set time boundaries "
                + "and rules, and only accept entries between the given times.";
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
            this.longhelp = "This command lists the current contests on the server. A time "
                    + "period must be specified (current contests, contests that have ended, "
                    + "or contests that have not started yet).";
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
                List<String[]> list = contests.getUpcomingContestsForGuild(event.getGuild().getId());
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
                List<String[]> list = contests.getEndedContestsForGuild(event.getGuild().getId());
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
            this.longhelp = "This command displays the rules that were set for a contest at creation, or upon last edit.";
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
            this.longhelp = "This command shows the time for a contest. For contests that have ended, "
                    + "it will show how long ago they ended. For current contests, it will show how much "
                    + "time remains. For upcoming contests, it will show how long until it starts.";
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
            this.longhelp = "This command is used to submit an entry to a contest. Entries "
                    + "are in the form of text (or links). If a contest has image submission, it "
                    + "is recommened to use some image-hosting site, rather than Discord-hosting. "
                    + "Each user may only submit one entry; any entry after the first will overwrite "
                    + "the previous.";
            this.availableInDM = false;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true),
                new Argument("submission", Argument.Type.LONGSTRING, true)
            };
            this.cooldown = 30;
            this.cooldownKey = event -> event.getAuthor().getId()+"|contestsubmit";
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
            this.longhelp = "This contests compiles all the entries for a contest and uploads "
                    + "them (in a .txt file) to the current chat.";
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
                File f = OtherUtil.writeArchive(builder.toString(), contest[Contests.CONTESTNAME]+"_entries");
                return new Pair<>("Entries for **"+contest[Contests.CONTESTNAME]+"**",f);
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
            this.longhelp = "Thic command creates a new contest on the server. A name must be provided for the "
                    + "contest, as well as how long until the contest should start, how long the contest should run "
                    + "for, and the rules of the contest. A | (vertical pipe) MUST be included between the first set "
                    + "of time units (time until start) and the second set (length of contest).";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true),
                new Argument("time until start",Argument.Type.TIME,true,"|",0,2592000),
                new Argument("length of contest",Argument.Type.TIME,true,0,31536000),
                new Argument("rules",Argument.Type.LONGSTRING,true)
            };
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
            long start = OffsetDateTime.now().toInstant().toEpochMilli()+(secTilStart*1000);
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
            this.longhelp = "Thic command is used to edit an existing contest. The time values are the "
                    + "difference in time from the already-established endpoints. If rules are included they "
                    + "will replace the current rules. If no rules are included, the existing rules will be"
                    + "kept.";
            this.level = PermLevel.ADMIN;
            this.arguments = new Argument[]{
                new Argument("contestname",Argument.Type.SHORTSTRING,true),
                new Argument("change in starttime",Argument.Type.TIME,true,"|"),
                new Argument("change in endtime",Argument.Type.TIME,true),
                new Argument("rules",Argument.Type.LONGSTRING,false)
            };
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
                contest[Contests.ENDTIME] = (Long.parseLong(contest[Contests.ENDTIME])+(changeInEnd*1000))+"";
            contests.set(contest);
            Sender.sendResponse(SpConst.SUCCESS+"Contest \""+name+"\" edited.", event);
            return true;
        }
    }
}           