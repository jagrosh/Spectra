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

import java.text.DecimalFormat;
import java.util.List;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;
import spectra.datasources.Donators;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Donator extends Command {
    private final Donators donators;
    public Donator(Donators donators)
    {
        this.donators = donators;
        this.command = "donator";
        this.help = "adds a donator";
        this.longhelp = "This command adds a user to the donator list. If a value of 0 is set, it will "
                + "remove them instead. To add \"donator\" status without a monetary value, use a negative number.";
        this.level = PermLevel.JAGROSH;
        this.arguments = new Argument[]{
            new Argument("cents",Argument.Type.INTEGER,true),
            new Argument("username",Argument.Type.USER,true)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        double amount = ((long)args[0])/100.0;
        User user = (User)args[1];
        
        if(amount==0)
        {
            donators.remove(user.getId());
            Sender.sendResponse(SpConst.SUCCESS+"You have removed **"+user.getUsername()+"** from the donator list", event);
        }
        else
        {
            String[] donation = new String[donators.getSize()];
            donation[Donators.AMOUNT] = amount+"";
            donation[Donators.USERID] = user.getId();
            donators.set(donation);
            Sender.sendResponse(SpConst.SUCCESS+"You have added **"+user.getUsername()+"** as donating `$"+df.format(amount)+"`", event);
        }
        StringBuilder builder = new StringBuilder("** == DONATORS == **");
        for(String[] donator : donators.donatorList())
        {
            double amt = Double.parseDouble(donator[Donators.AMOUNT]);
            if(amt>.01)
                builder.append("\n\n<@").append(donator[Donators.USERID]).append("> -- $").append(df.format(amt));
        }
        TextChannel chan = event.getJDA().getTextChannelById(DONATOR_TC);
        List<Message> list = chan.getHistory().retrieveAll();
        if(list==null || list.isEmpty() || !list.get(0).getAuthor().equals(event.getJDA().getSelfInfo()))
            Sender.sendMsg(builder.toString(), chan);
        else
            list.get(0).updateMessage(builder.toString());
        return true;
    }
    
    private final DecimalFormat df = new DecimalFormat("#,###,##0.00");
    private final String DONATOR_TC = "164051872032751616";
}
