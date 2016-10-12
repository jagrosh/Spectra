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

import net.dv8tion.jda.JDAInfo;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class About extends Command{

    public About()
    {
        this.command = "about";
        this.aliases = new String[]{"botabout","hello"};
        this.help = "provides some information about "+SpConst.BOTNAME;
        this.longhelp = "This command displays basic information about the current state of the bot, as well as how to get in touch with the author.";
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String aboutText =
            "Hello! I am **"+SpConst.BOTNAME+"**, a bot built by **jagrosh**!"+
            "\nI was written for Discord in Java, using the JDA library ("+JDAInfo.VERSION+")"+
            "\nI'm currently at version "+SpConst.VERSION+
            "\n"+
            "\nMy default prefixes are `"+SpConst.PREFIX+"` and `"+SpConst.ALTPREFIX+"`"+
            "\nType `"+SpConst.PREFIX+"help` and I'll DM you a list of commands you can use!"+
            "\nSee some of my other stats with `"+SpConst.PREFIX+"stats`"+
            "\n"+
            "\nFor additional help, contact **jagrosh** (ID:"+SpConst.JAGROSH_ID+")"+
            "\nYou can send him a Direct Message, or join his server:"+
            "\nhttps://discord.gg/0p9LSGoRLu6Pet0k";
        Sender.sendResponse(aboutText, event);
        return true;
    }
    
}
