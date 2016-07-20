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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;
import spectra.utils.OtherUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Draw extends Command{
    public Draw()
    {
        this.command = "draw";
        this.help = "draws an electric wave";
        this.longhelp = "This command draws an electric wave using the same algorithm that "
                + ""+SpConst.BOTNAME+" uses to generate avatars. If no color is provided, it "
                + "will generate it in a random color. To provide a color use hex (like #8FC73E) "
                + "or an integer value (like 9422654).";
        this.cooldown = 10;
        this.cooldownKey = (event) -> {return event.getAuthor().getId()+"|draw";};
        this.arguments = new Argument[]{
            new Argument("hexcolor",Argument.Type.SHORTSTRING,false)
        };
        this.requiredPermissions = new Permission[]{
            Permission.MESSAGE_ATTACH_FILES
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String in = args[0]==null?null:(String)(args[0]);
        Color color;
        if(in==null)
            color = Color.getHSBColor((float)Math.random(), 1.0f, .5f);
        else
        {
            if(in.matches("#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])"))
                in = in.replaceAll("#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])", "#$1$1$2$2$3$3");
            try{
                color = Color.decode(in);
            }catch(NumberFormatException e){
                Sender.sendResponse(SpConst.ERROR+"\""+in+"\" is not a valid color.", event);
                return false;
            }
        }
        Sender.sendFileResponse(()->{
            BufferedImage bi = OtherUtil.makeWave(color);
                File f = new File("wave.png");
                try {
                    ImageIO.write(bi, "png", f);
                } catch (IOException ex) {
                    System.out.println("[ERROR] An error occured drawing the wave.");
                }
            return new Pair<>(null,f);
        }, event);
        return true;
    }
}
