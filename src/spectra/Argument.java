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


/**
 *
 * @author jagrosh
 */
public class Argument {
    final boolean required;
    final String name;
    final Type type;
    
    final String separator;
    
    final long min,max;
    
    public Argument(String name, Type type, boolean required)
    {
        this(name, type, required, null, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    public Argument(String name, Type type, boolean required, String separator)
    {
        this(name, type, required, separator, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    public Argument(String name, Type type, boolean required, long min, long max)
    {
        this(name, type, required, null, min, max);
    }
    
    public Argument(String name, Type type, boolean required, String separator, long min, long max)
    {
        this.name = name;
        this.type = type;
        this.required = required;
        this.separator = separator;
        this.min = min;
        this.max = max;
    }
    
    @Override
    public String toString()
    {
        String str;
        if(required)
            str = "<"+name+">";
        else
            str = "["+name+"]";
        if(separator!=null)
            str+=" "+separator;
        return str;
    }
    
    public static String arrayToString(Argument[] args)
    {
        if(args.length==0)
            return "";
        StringBuilder builder = new StringBuilder(" ");
        builder.append(args[0].toString());
        for(int i = 1; i<args.length; i++)
            builder.append(" ").append(args[i].toString());
        return builder.toString();
    }
    
    public enum Type {
        INTEGER, SHORTSTRING, LONGSTRING, TIME, USER, LOCALUSER, BANNEDUSER, TEXTCHANNEL, ROLE, GUILD
    }
    
}
