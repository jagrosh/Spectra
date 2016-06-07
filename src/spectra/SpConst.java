/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SpConst {
    //bot items
    final public static String BOTNAME = "Spectra";
    final public static String VERSION = "3.0";
    final public static String PREFIX = "%%";
    final public static String ALTPREFIX = "//";
    
    
    //discord items
    final public static String JAGROSH_ID = "113156185389092864";
    final public static String JAGZONE_INVITE = "https://discord.gg/0p9LSGoRLu6Pet0k";
    
    //command responses
    final public static String SUCCESS = ""+(char)9989;
    final public static String WARNING = ""+(char)9888;
    final public static String ERROR   = ""+(char)9940;
    
    final public static String NEED_PERMISSION_     = ERROR+" **I do not have the proper permissions to do that!**\nPlease make sure I have the following permissions:\n";
    final public static String BANNED_COMMAND      = ERROR+" **That command is banned on this server!**";
    final public static String BANNED_COMMAND_S    = "To toggle this command on/off on this server, use `"+PREFIX+"toggle ";
    final public static String NOT_VIA_DM          = ERROR+" **That command is not available via Direct Message!**";
    final public static String ARGUMENT_ERROR_      = ERROR+" **Insufficient or incorrect arguments**:\n";
    final public static String INVALID_             = ERROR+" **Invalid command**\n";
    final public static String CANT_HELP           = WARNING+" Help could not be sent because you are blocking Direct Messages!";
    final public static String CANT_SEND_           = WARNING+" The command could not be completed because I cannot send messages in ";
}
