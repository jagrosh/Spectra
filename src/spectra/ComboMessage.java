/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

import java.io.File;
import net.dv8tion.jda.entities.MessageChannel;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ComboMessage {
    private String message;
    private File file;
    private String alternate;//if can't send files
    private String dependency;
    
    public ComboMessage(String msg, File f, String dependency, String alternate)
    {
        this.message = msg;
        this.file = f;
        this.dependency = dependency;
        this.alternate = alternate;
    }
    
    public void send(MessageChannel chan)
    {
        send(chan,null);
    }
    
    public void send(MessageChannel chan, MessageChannel fallback)
    {
        message   =   message.replace("@everyone", "@\u180Eeveryone").replace("@here", "@\u180Ehere").trim();
        alternate = alternate.replace("@everyone", "@\u180Eeveryone").replace("@here", "@\u180Ehere").trim();
    }

    @Override
    public String toString() {
        return "DepMessage:[Msg: "+message + "] [Fil: "+file.toString()+"] [Alt: "+alternate+"]";
    }
}
