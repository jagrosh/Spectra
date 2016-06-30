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
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import spectra.Argument;
import spectra.Command;
import spectra.FeedHandler;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Feeds;
import spectra.datasources.Mutes;
import spectra.datasources.Settings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Softban extends Command {
    final FeedHandler handler;
    final Settings settings;
    final Mutes mutes;
    public Softban(FeedHandler handler, Settings settings, Mutes mutes)
    {
        this.handler = handler;
        this.settings = settings;
        this.mutes = mutes;
        this.command = "softban";
        this.help = "mutes and temporarily bans a user from the server";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.LOCALUSER,true),
            new Argument("for <reason>",Argument.Type.LONGSTRING,false)
        };
        this.separatorRegex = "\\s+for\\s+";
        this.availableInDM=false;
        this.level = PermLevel.MODERATOR;
        this.requiredPermissions = new Permission[] {
            Permission.BAN_MEMBERS
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User target = (User)(args[0]);
        String reason = args[1]==null?null:(String)(args[1]);
        if(reason==null)
            reason = "[no reason specified]";
        PermLevel targetLevel = PermLevel.getPermLevelForUser(target, event.getGuild(),settings.getSettingsForGuild(event.getGuild().getId()));
        //check perm level of other user
        if(targetLevel.isAtLeast(level))
        {
            Sender.sendResponse(SpConst.WARNING+"**"+target.getUsername()+"** cannot be softbanned because they are listed as "+targetLevel, event);
            return false;
        }
        
        //check if bot can interact with the other user
        if(!PermissionUtil.canInteract(event.getJDA().getSelfInfo(), target, event.getGuild()))
        {
            Sender.sendResponse(SpConst.WARNING+"I cannot softban **"+target.getUsername()+"** due to permission hierarchy", event);
            return false;
        }
        
        //attempt to softban
        try{
            String id = target.getId();
            event.getGuild().getManager().ban(id, 1);
            String[] prevmute = mutes.getMute(id, event.getGuild().getId());
            if(prevmute==null || OffsetDateTime.now().plusDays(1).isAfter(OffsetDateTime.parse(prevmute[Mutes.UNMUTETIME])))
                mutes.set(new String[]{id,event.getGuild().getId(),OffsetDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)});
            Sender.sendResponse(SpConst.SUCCESS+"**"+target.getUsername()+"** was softbanned from the server \uD83C\uDF4C", event);
            handler.submitText(Feeds.Type.MODLOG, event.getGuild(), 
                    "\uD83C\uDF4C **"+event.getAuthor().getUsername()+"** softbanned **"+target.getUsername()+"** for "+reason);
            new Thread(){
            public void run(){
                try{Thread.sleep(10000);}catch(InterruptedException e){}
                event.getGuild().getManager().unBan(id);
            }}.start();
            return true;
        }catch(Exception e)
        {
            Sender.sendResponse(SpConst.ERROR+"Failed to softban **"+target.getUsername()+"**", event);
            return false;
        }
    }
}
