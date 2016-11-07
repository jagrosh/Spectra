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
package spectra.utils;

import jagtag.Parser;
import jagtag.JagTag;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.datasources.LocalTags;
import spectra.datasources.Tags;
import spectra.jagtag.libraries.Discord;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class TagUtil {
    
    public static String URL = "https://github.com/jagrosh/Spectra/wiki/JagTag";
    
    public static final Parser parser = JagTag.newDefaultBuilder().addMethods(Discord.getMethods()).build();
    
    public static boolean isNSFWAllowed(MessageReceivedEvent event)
    {
        if(event.isPrivate())
            return true;
        if(event.getTextChannel().getName().contains("nsfw"))
            return true;
        if(event.getTextChannel().getTopic()!=null && event.getTextChannel().getTopic().toLowerCase().contains("{nsfw}"))
            return true;
        return false;
    }
    
    public static String getContents(String[] tag)
    {
        if(tag.length==3)
            return tag[Tags.CONTENTS];
        else
            return tag[LocalTags.CONTENTS];
    }
    
    public static String getTagname(String[] tag)
    {
        if(tag.length==3)
            return tag[Tags.TAGNAME];
        else
            return tag[LocalTags.TAGNAME];
    }
    
    public static String getOwnerId(String[] tag)
    {
        if(tag.length==3)
            return tag[Tags.OWNERID];
        else
            return tag[LocalTags.OWNERID];
    }
    
    public static boolean isNSFWTag(String[] tag)
    {
        return getContents(tag).toLowerCase().contains("{nsfw}");
    }
}
