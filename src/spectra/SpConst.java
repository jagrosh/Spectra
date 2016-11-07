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
package spectra;

import java.time.OffsetDateTime;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SpConst {
    //bot items
    final public static String BOTNAME = "Spectra";
    final public static String VERSION = "3.2.4";
    final public static String PREFIX = "%";
    final public static String ALTPREFIX = "/";
    final public static String DEFAULT_GAME = "Type "+PREFIX+"help";
    
    final public static OffsetDateTime PUBLIC_DATE = OffsetDateTime.parse("2016-09-18T04:00:00.000-04:00");
    final public static double BOT_COLLECTION_PERCENT = .80;
    
    //discord items
    final public static String JAGROSH_ID = "113156185389092864";
    final public static String JAGZONE_INVITE = "https://discord.gg/0p9LSGoRLu6Pet0k";
    final public static String JAGZONE_ID = "147698382092238848";
    
    //command responses
    final public static String SUCCESS = (char)9989+" ";
    final public static String WARNING = (char)9888+" ";
    final public static String ERROR   = (char)9940+" ";
    final public static String SUCCESS_E = "%E2%9C%85";
    final public static String WARNING_E = "%E2%9A%A0";
    final public static String ERROR_E = "%E2%9B%94";
    
    
    final public static String LINESTART = "  ➣  ";
    
    final public static String NEED_PERMISSION          = ERROR + "**I do not have the proper permissions to do that!**\n"
                                                                + "Please make sure I have the following permissions:\n%s";
    final public static String BANNED_COMMAND           = ERROR + "**That command is unavailable here!**";
    final public static String BANNED_COMMAND_IFADMIN   = "\nTo toggle this command on/off on this server, use `"+PREFIX.replace("%", "%%")+"cmd enable %s`\n"
                                                                + "Alternatively, add `{%s}` to a channel's topic to make it available there";
    final public static String NOT_VIA_DM               = ERROR + "**That command is not available via Direct Message!**";
    final public static String ONLY_WHITELIST           = ERROR + "**That command is only for whitelisted servers!**";
    final public static String ONLY_GOLDLIST           = ERROR + "**That command is only for goldlisted servers!**";
    final public static String TOO_FEW_ARGS             = ERROR + "**Too few arguments provided**\nTry using `"+PREFIX.replace("%", "%%")+"%s help` for more information.";
    
    final private static String INVALID_VALUE            = ERROR + "**Invalid Value:**\n";
    final public static String INVALID_INTEGER          = INVALID_VALUE + "`%s` must be an integer between %s and %s";
    final public static String INVALID_LENGTH          = INVALID_VALUE + "`%s` must be between %s and %s characters";
    final public static String INVALID_TIME             = INVALID_VALUE + "No amount of time could be parsed from \"%s\"";
    final public static String INVALID_TIME_RANGE       = INVALID_VALUE + "`%s` must be in at least %s, and no longer than %s";
    final public static String INVALID_IN_DM            = INVALID_VALUE + "`%s` cannot be included via Direct Message";

    //final public static String ARGUMENT_ERROR_  = ERROR+"**Insufficient or incorrect arguments**:\n";
    //final public static String INVALID_VALUE_   = ERROR+"**Invalid command**\n";
    
    final public static String CANT_HELP                = WARNING + "Help could not be sent because you are blocking Direct Messages!";
    final public static String CANT_SEND                = WARNING + "The command could not be completed because I cannot send messages in %s";
    
    final public static String MULTIPLE_FOUND           = WARNING + "**Multiple %s found matching \"%s\":**";
    final public static String NONE_FOUND               = WARNING + "**No %s found matching \"%s\"**";
    
    final public static String ON_COOLDOWN              = WARNING + "That command is on cooldown for another %s";
    
    final public static String ROOM_WARNING             = "[%s] This room has not seen activity for at least 36 hours. It will be deleted if "
                                                            + "there is no activity within the next 12 hours, or you can delete it yourself with `"+PREFIX.replace("%", "%%")+"room remove`";
}
