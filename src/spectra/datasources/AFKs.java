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
public class AFKs extends DataSource {
    
    public AFKs()
    {
        this.filename = "discordbot.afks";
        this.generateKey = (item)-> {return item[USERID];};
        this.size = 2;
    }
    
    final public static int USERID  = 0;
    final public static int MESSAGE = 1;
}
