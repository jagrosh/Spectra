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
package spectra.misc;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.OnlineStatus;

/**
 *
 * @author John Grosh (jagrosh)
 */
public enum SafeEmote {
    
    VOICEJOIN("216568548984750090","\uD83C\uDF9B"),
    //VOICEMOVE("","\uD83D\uDD18"),
    VOICELEAVE("216568542764597249","\uD83D\uDED1"),
    ONLINE("212789758110334977","\uD83D\uDCD7"),
    AWAY("212789859071426561","\uD83D\uDCD9"),
    OFFLINE("212790005943369728","\uD83D\uDCD3"),
    STREAMING("212789640799846400","\uD83D\uDCD8"),
    DND("236744731088912384","\uD83D\uDCD5"),
    BOT("229463554749628416","\uD83E\uDD16")
    ;
    
    private final String emoteId;
    private final String altEmoji;
    private SafeEmote(String id, String alt)
    {
        this.emoteId = id;
        this.altEmoji = alt;
    }
    public String get(JDA jda)
    {
        if(jda.getEmoteById(emoteId)==null)
            return altEmoji;
        return "<:"+this.toString().toLowerCase()+":"+emoteId+">";
    }
    
    public final static SafeEmote map(OnlineStatus status)
    {
        switch(status)
        {
            case ONLINE:
                return ONLINE;
            case AWAY:
                return AWAY;
            case DO_NOT_DISTURB:
                return DND;
            default:
                return OFFLINE;
        }
    };
}
