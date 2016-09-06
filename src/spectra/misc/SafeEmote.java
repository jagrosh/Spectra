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

/**
 *
 * @author John Grosh (jagrosh)
 */
public enum SafeEmote {
    
    VOICEJOIN("216568548984750090","\uD83C\uDF9B"),VOICEMOVE("","\uD83D\uDD18"),VOICELEAVE("216568542764597249","\uD83D\uDED1");
    
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
}
