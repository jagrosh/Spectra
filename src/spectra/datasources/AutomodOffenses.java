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
public class AutomodOffenses extends DataSource {
    
    public AutomodOffenses()
    {
        this.filename = "discordbot.amoffenses";
        this.size = 4;
        this.generateKey = item -> item[GUILDID]+"|"+item[USERID];
    }
    
    final public static int GUILDID   = 0;
    final public static int USERID = 1;
    final public static int STRIKES = 2;
    final public static int LASTSTRIKE = 3;
}
