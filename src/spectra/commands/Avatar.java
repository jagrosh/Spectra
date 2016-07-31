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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.utils.OtherUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Avatar extends Command {

    public Avatar()
    {
        this.command = "avatar";
        this.aliases = new String[]{"avy","avi"};
        this.help = "shows the avatar of yourself or a user";
        this.longhelp = "This command displays the given user's avatar (image), or your own if no user is "
                + "provided. If possible, it will upload the image; otherwise it will provide a link.";
        this.arguments = new Argument[]{
            new Argument("username",Argument.Type.USER,false)
        };
    }
    
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        User user = (User)(args[0]);
        if(user==null)
            user = event.getAuthor();
        String url = (user.getAvatarUrl()==null ? (user.getId().equals("1") ? "https://discordapp.com/assets/f78426a064bc9dd24847519259bc42af.png" : user.getDefaultAvatarUrl()) : user.getAvatarUrl());
        String str = "Avatar for **"+user.getUsername()+"**:";
        BufferedImage bi = OtherUtil.imageFromUrl(url);
        if(bi==null)
            Sender.sendResponse(str+"\n"+url, event);
        else
        {
            
            Sender.sendFileResponseWithAlternate(() -> {
                File f = new File("avatar"+(int)(Math.random()*10)+".png");//random just in case 2 people use at the exact same time, but I don't want to store a ton
                try {
                    ImageIO.write(bi, "png", f);
                } catch (IOException ex) {System.out.println("[ERROR] Could not save avatar");}
                return new Pair<>(str,f);
            }, str+"\n"+url, event);
        }
        return true;
    }
    
}
