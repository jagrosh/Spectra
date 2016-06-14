/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    
    final int min,max;
    
    public Argument(String name, Type type, boolean required)
    {
        this(name,type,required,Integer.MIN_VALUE,Integer.MAX_VALUE);
    }
    
    public Argument(String name, Type type, boolean required, int min, int max)
    {
        this.name = name;
        this.type = type;
        this.required = required;
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
        return str;
    }
    
    public static String arrayToString(Argument[] args)
    {
        if(args.length==0)
            return "";
        StringBuilder builder = new StringBuilder(" `");
        builder.append(args[0].toString());
        for(int i = 1; i<args.length; i++)
            builder.append(" ").append(args[i].toString());
        return builder.append("`").toString();
    }
    
    public enum Type {
        INTEGER, SHORTSTRING, LONGSTRING, TIME, USER, LOCALUSER, TEXTCHANNEL, ROLE
    }
}
