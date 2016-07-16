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

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.Spectra;
import spectra.datasources.Feeds;
import spectra.tempdata.Statistics;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SystemCmd extends Command {
    final Spectra spectra;
    final Feeds feeds;
    final Statistics statistics;
    public SystemCmd(Spectra spectra, Feeds feeds, Statistics statistics)
    {
        this.spectra = spectra;
        this.feeds = feeds;
        this.statistics = statistics;
        this.command = "system";
        this.help = "commands for controlling the bot";
        this.level = PermLevel.JAGROSH;
        this.children = new Command[]{
            new SystemIdle(),
            new SystemReady(),
            new SystemShutdown()
        };
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) 
    {
        Sender.sendResponse("\uD83D\uDCDF Please specify a system command.", event);
        return false;
    }
    
    private class SystemIdle extends Command
    {
        public SystemIdle()
        {
            this.command = "idle";
            this.help = "prevents the bot from receiving commands";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(spectra.isIdling())
            {
                Sender.sendResponse(SpConst.ERROR+"**"+SpConst.BOTNAME+"** is already `IDLING`", event);
                return false;
            }
            spectra.setIdling(true);
            event.getJDA().getAccountManager().setIdle(true);
            Sender.sendResponse("\uD83D\uDCF4 **"+SpConst.BOTNAME+"** is now `IDLING`", event);
            return true;
        }
    }
    
    private class SystemReady extends Command
    {
        public SystemReady()
        {
            this.command = "ready";
            this.help = "allows the bot to recieve commands";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(!spectra.isIdling())
            {
                Sender.sendResponse(SpConst.ERROR+"**"+SpConst.BOTNAME+"** is already `READY`", event);
                return false;
            }
            spectra.setIdling(false);
            event.getJDA().getAccountManager().setIdle(false);
            Sender.sendResponse(SpConst.SUCCESS+"**"+SpConst.BOTNAME+"** is now `READY`", event);
            return true;
        }
    }
    
    private class SystemShutdown extends Command
    {
        public SystemShutdown()
        {
            this.command = "shutdown";
            this.help = "shuts down the bot safely";
            this.level = PermLevel.JAGROSH;
        }
        @Override
        protected boolean execute(Object[] args, MessageReceivedEvent event) {
            if(!spectra.isIdling())
            {
                Sender.sendResponse(SpConst.ERROR+"Cannot shutdown if not idling!", event);
                return false;
            }
            try{
            event.getJDA().getTextChannelById(feeds.feedForGuild(event.getJDA().getGuildById(SpConst.JAGZONE_ID), Feeds.Type.BOTLOG)[Feeds.CHANNELID])
                    .sendMessage(FeedHandler.botlogFormat(SpConst.ERROR+"**"+SpConst.BOTNAME+"** is going <@&182294168083628032>"+"\nRuntime: "+FormatUtil.secondsToTime(statistics.getUptime())));
            event.getChannel().sendMessage("\uD83D\uDCDF Shutting down...");}catch(Exception e){System.err.println(e);}
            spectra.shutdown();
            return true;
        }
    }
    }
