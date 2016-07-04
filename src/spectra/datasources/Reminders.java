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
package spectra.datasources;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Reminders extends DataSource {
    
    public Reminders()
    {
        this.filename = "discordbot.reminders";
        this.size = 4;
        this.generateKey = item -> item[USERID]+"|"+item[CHANNELID]+"|"+item[EXPIRETIME];
    }
    
    public List<String[]> getRemindersForUser(String userId)
    {
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            data.values().stream().filter((item) -> (item[USERID].equals(userId))).forEach((item) -> {
                list.add(item.clone());
            });
        }
        return list;
    }
    
    public List<String[]> getExpiredReminders()
    {
        long now = OffsetDateTime.now().toInstant().toEpochMilli();
        ArrayList<String[]> list = new ArrayList<>();
        synchronized(data)
        {
            for(String[] item : data.values())
                if(now>Long.parseLong(item[EXPIRETIME]))
                    list.add(item.clone());
        }
        return list;
    }
    
    public void removeReminder(String[] reminder)
    {
        remove(generateKey.apply(reminder));
    }
    
    final public static int USERID   = 0;
    final public static int CHANNELID = 1;
    final public static int EXPIRETIME  = 2;
    final public static int MESSAGE = 3;
}
