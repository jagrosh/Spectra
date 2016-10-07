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
package spectra.entities;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class JagTagParser {
    private final int maxIterations;
    private final HashMap<String,JagTagFunction> functions;
    
    public JagTagParser()
    {
        this(200);
    }
    
    public JagTagParser(int maxIterations)
    {
        this.maxIterations = maxIterations;
        this.functions = new HashMap<>();
    }
    
    public boolean addFunction(String name, JagTagFunctionBuilder builder)
    {
        if(functions.containsKey(name))
            return false;
        functions.put(name,builder.build());
        return true;
    }        
    
    public boolean removeFunction(String name)
    {
        return functions.remove(name)!=null;
    }
    
    public String parse(String input, HashMap<String,Object> parameters)
    {
        //sanitize input
        String output = sanitize(input);
        //run loop
        //while(){}
        
        
        return desanitize(output);
    }
    
    private static String sanitize(String input)
    {
        return input.replace("\\|","\u0012").replace("\\{","\u0013").replace("\\}","\u0014");
    }
    
    private static String desanitize(String input)
    {
        return input.replace("\u0012","|").replace("\u0013","{").replace("\u0014","}").replace("\u0015","{").replace("\u0016","}");
    }
    
    public class JagTagFunctionBuilder {
        private final String name;
        private Supplier<String> base;
        private Function<String,String> advanced;
        private int parts;
        private String[] tokens;
        
        public JagTagFunctionBuilder(String name)
        {
            this.name = name;
        }
        
        public JagTagFunctionBuilder setBaseFunction(Supplier<String> function)
        {
            this.base = function;
            return this;
        }
        
        public JagTagFunctionBuilder setAdvancedFunction(Function<String,String> function, int parts, String... tokens)
        {
            this.advanced = function;
            this.parts = parts;
            this.tokens = tokens;
            return this;
        }
        
        private JagTagFunction build()
        {
            return new JagTagFunction(name,base,advanced,parts,tokens);
        }
    }
    
    private class JagTagFunction {
        private final String name;
        private final Supplier<String> base;
        private final Function<String,String> advanced;
        private final int parts;
        private final String[] tokens;
        private JagTagFunction(String name, Supplier<String> base, Function<String,String> advanced, int parts, String... tokens)
        {
            this.name = name;
            this.base = base;
            this.advanced = advanced;
            this.parts = parts;
            this.tokens = tokens;
        }
    }
}
