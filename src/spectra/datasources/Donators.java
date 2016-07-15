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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import spectra.DataSource;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Donators extends DataSource {
    
    public Donators()
    {
        this.filename = "discordbot.donators";
        this.size = 2;
        this.generateKey = item -> item[USERID];
    }
    
    public List<String[]> donatorList()
    {
        synchronized(data)
        {
            ArrayList<String[]> list = new ArrayList<>(data.values());
            Collections.sort(list, (String[] a, String[] b) -> (int)(100.0*(Double.parseDouble(b[1])-Double.parseDouble(a[1]))) );
            return list;
        }
    }
    
    final public static int USERID   = 0;
    final public static int AMOUNT = 1;
}
