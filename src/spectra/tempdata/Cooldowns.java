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
package spectra.tempdata;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Cooldowns {
    
    private static final Cooldowns cooldownsInstance = new Cooldowns();
    private final HashMap<String,OffsetDateTime> cooldowns;
    
    private Cooldowns()
    {
        cooldowns = new HashMap<>();
    }
    
    public static Cooldowns getInstance()
    {
        return cooldownsInstance;
    }
    
    public synchronized long check(String key)
    {
        if(key==null)
            return 0;
        OffsetDateTime time = cooldowns.get(key);
        if(time==null)
            return 0;
        long seconds = OffsetDateTime.now().until(time, ChronoUnit.SECONDS);
        if(seconds <= 0)
            return 0;
        return seconds;
    }
    
    public synchronized long checkAndApply(String key, int newseconds)
    {
        if(key==null || newseconds==0)
            return 0;
        OffsetDateTime time = cooldowns.get(key);
        long seconds = 0;
        if(time!=null)
            seconds = OffsetDateTime.now().until(time, ChronoUnit.SECONDS);
        if(seconds <= 0)
        {
            cooldowns.put(key, OffsetDateTime.now().plusSeconds(newseconds));
            return 0;
        }
        return seconds;
    }
    
    public synchronized void resetCooldown(String key)
    {
        cooldowns.remove(key);
    }
}
