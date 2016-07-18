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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Reminders;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Reminder extends Command {
    
    private final Reminders reminders;
    public Reminder(Reminders reminders)
    {
        this.reminders = reminders;
        this.command = "reminder";
        this.help = "sets a reminder";
        this.longhelp = "This command is used to set reminders. The reminder will be sent "
                + "to the channel it was set in (or in DMs if you set it in DMs or the channel "
                + "is no longer available).";
        this.aliases = new String[]{"remind","remindme"};
        this.cooldown = 30;
        this.cooldownKey = event -> event.getAuthor().getId()+"|reminder";
        this.arguments = new Argument[]{
            new Argument("time",Argument.Type.TIME,true,60,63072000),
            new Argument("message",Argument.Type.LONGSTRING,true)
        };
        this.children = new Command[]{
            new RemindList(),
            new RemindRemove()
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        long seconds = (long)args[0];
        String message = (String)args[1];
        String[] reminder = new String[reminders.getSize()];
        reminder[Reminders.USERID] = event.getAuthor().getId();
        reminder[Reminders.CHANNELID] = event.isPrivate() ? "private" : event.getTextChannel().getId();
        reminder[Reminders.EXPIRETIME] = (OffsetDateTime.now().toInstant().toEpochMilli() + (seconds * 1000)) + "";
        reminder[Reminders.MESSAGE] = message +"  ~set "+OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        reminders.set(reminder);
        Sender.sendResponse(SpConst.SUCCESS+"Reminder set to expire in "+FormatUtil.secondsToTime(seconds), event);
        return true;
    }
    
    private class RemindList extends Command
    {
        private RemindList()
        {
            this.command = "list";
            this.help = "shows a list of your current reminders";
            this.longhelp = "This command lists all of the reminders you have set, as well as where they are set for.";
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            List<String[]> list = reminders.getRemindersForUser(event.getAuthor().getId());
            if(list.isEmpty())
            {
                Sender.sendResponse(SpConst.WARNING+"You don't have any reminders set!", event);
                return false;
            }
            StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"**"+list.size()+"** reminders:");
            for(int i=0; i<list.size(); i++)
            {
                builder.append("\n`").append(i).append(".` ");
                String channelid = list.get(i)[Reminders.CHANNELID];
                builder.append(channelid.equals("private") ? "Direct Message - \"" : "<#"+channelid+"> - \"");
                String message = list.get(i)[Reminders.MESSAGE].length() > 20 ? list.get(i)[Reminders.MESSAGE].substring(0,20)+"..." : list.get(i)[Reminders.MESSAGE];
                builder.append(message).append("\" in ")
                        .append(FormatUtil.secondsToTime((Long.parseLong(list.get(i)[Reminders.EXPIRETIME]) - OffsetDateTime.now().toInstant().toEpochMilli())/1000));
            }
            Sender.sendResponse(builder.toString(), event);
            return true;
        }
    }
    
    private class RemindRemove extends Command
    {
        private RemindRemove()
        {
            this.command = "remove";
            this.aliases = new String[]{"delete","cancel","clear"};
            this.help = "cancels a reminder from the list";
            this.longhelp = "This command cancels a reminder. The index must be the number "
                    + "of the given reminder when using `"+SpConst.PREFIX+"remind list`";
            this.arguments = new Argument[]{
                new Argument("index",Argument.Type.INTEGER,true,0,100)
            };
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            long index = (long)args[0];
            List<String[]> list = reminders.getRemindersForUser(event.getAuthor().getId());
            if(list.isEmpty())
            {
                Sender.sendResponse(SpConst.ERROR+"You cannot clear a reminder because you do not have any set!", event);
                return false;
            }
            if(index+1 > list.size())
            {
                Sender.sendResponse(SpConst.ERROR+"There is no reminder at that index! Please enter a number 0 - "+(list.size()-1)+", or try `"+SpConst.PREFIX+"remind list`", event);
                return false;
            }
            reminders.removeReminder(list.get((int)index));
            Sender.sendResponse(SpConst.SUCCESS+"You have removed a reminder.", event);
            return true;
        }
        
    }
    
}
