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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.PermissionException;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Search extends Command {
    public Search()
    {
        this.command = "find";
        this.aliases = new String[]{"ctrlf"};
        this.help = "find text in a channel";
        this.longhelp = "This command searches through past messages for messages containing the provided text. The search is not case-sensitive";
        this.arguments = new Argument[]{
            new Argument("query",Argument.Type.LONGSTRING,true,3,2000)
        };
        this.cooldown = 30;
        this.cooldownKey = event -> event.getAuthor().getId()+"|find";
        this.whitelistCooldown = -1;
        this.requiredPermissions = new Permission[]{Permission.MESSAGE_HISTORY};
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String query = (String)args[0];
        String lQuery = query.toLowerCase();
        try{
            event.getMessage().deleteMessage();
        }catch(PermissionException e){}
        List<Message> logs = event.getChannel().getHistory().retrieve(300).stream().filter(m -> m.getRawContent().toLowerCase().contains(lQuery)).collect(Collectors.toList());
        if(logs.isEmpty())
        {
            Sender.sendPrivate(SpConst.WARNING+"No messages found containing `"+query+"`", event.getAuthor().getPrivateChannel());
            return false;
        }
        if(logs.size()>50)
        {
            Sender.sendPrivate(SpConst.WARNING+"Too many results! Try being more specific.", event.getAuthor().getPrivateChannel());
        }
        Collections.reverse(logs);
        StringBuilder builder = new StringBuilder(SpConst.SUCCESS+"`"+logs.size()+"` messages found containing `"+query+"`:");
        logs.stream().forEach(m -> 
                builder.append("\n\n").append(m.getAuthor()==null ? "???" : FormatUtil.shortUser(m.getAuthor())).append(": ").append(m.getRawContent())
        );
        Sender.sendPrivate(builder.toString(), event.getAuthor().getPrivateChannel());
        return true;
    }
}
