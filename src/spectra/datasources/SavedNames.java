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

import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SavedNames extends DataSource{
    
    public SavedNames()
    {
        this.filename = "discordbot.names";
        this.size = 2;
        this.generateKey = (item) -> {return item[USERID];};
    }
    
    public String getNames(String id)
    {
        synchronized(data)
        {
            return data.get(id)==null ? null : data.get(id)[NAMES];
        }
    }
    
    public void addName(String id, String newName)
    {
        synchronized(data)
        {
            String current = data.get(id)==null ? newName : newName+", "+data.get(id)[NAMES];
            if(current.length()>1600)
                current = current.substring(0,current.lastIndexOf(",", 1600));
            data.put(id, new String[]{id,current});
            setToWrite();
        }
    }
    
    final public static int USERID   = 0;
    final public static int NAMES   = 1;
}
