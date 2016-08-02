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
package spectra.commands;

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Roll extends Command {
    public Roll()
    {
        this.command = "roll";
        this.help = "rolls dice";
        this.longhelp = "This command is used for rolling one or more sets of dice. Each set must must "
                + "include a number of dice, and number of sides, or a modifier (or any combination). "
                + "The format to roll 3 d20 dice with a positive modifier of 4 would be `3d20+4`. Multiple "
                + "sets like this can be used, and should be separated by commas. Additionally, any missing "
                + "numbers will be replaced by the defaults (1d6+0). So, `d30` will roll 1d30+0.";
        this.arguments = new Argument[]{
            new Argument("[numRolls]#[numDice][<d|D>numSides][<+|->modifier]",Argument.Type.LONGSTRING,true)
        };
    }
    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String input = (String)args[0];
        String[] splitinput = input.split("\\s*,\\s*");
        List<String> diceinput = new ArrayList<>();
        for(String str : splitinput)
        {
            if(str.matches("\\d{1,4}\\s*#.+"))
            {
                int num = Integer.parseInt(str.substring(0,str.indexOf("#")).trim());
                for(int i=0; i<num; i++)
                    diceinput.add(str.substring(str.indexOf("#")+1).trim());
            }
            else
            {
                diceinput.add(str);
            }
        }
        ArrayList<Die> dice = new ArrayList<>();
        for(String dieinput : diceinput)
        {
            if(dieinput==null || dieinput.equals("") || !dieinput.matches(DIE_REGEX))
            {
                Sender.sendResponse(SpConst.ERROR+"`"+dieinput+"` is in an invalid format, or the included values are too large", event);
                return false;
            }
            Die die = new Die();
            try{
            String num = dieinput.replaceAll(DIE_REGEX, "$1");
            String sides = dieinput.replaceAll(DIE_REGEX, "$2");
            String mod = dieinput.replaceAll(DIE_REGEX, "$3");
            if(!num.equals(""))
                die.number = Integer.parseInt(num);
            if(!sides.equals(""))
                die.sides = Integer.parseInt(sides.substring(1).trim());
            if(!mod.equals(""))
                die.modifier = Integer.parseInt(mod.replace("\\s", ""));
            }catch(Exception e)
            {
                Sender.sendResponse(SpConst.ERROR+"`"+dieinput+"` was unable to be parsed.", event);
                return false;
            }
            if(die.sides>100 || die.number>100 || die.modifier>999 || die.modifier<-999)
            {
                Sender.sendResponse(SpConst.ERROR+"Each set of dice can only be up to 100 rolls, with 100 sides, and a modifier of up to 999", event);
                return false;
            }
            dice.add(die);
        }
        StringBuilder builder = new StringBuilder("\uD83C\uDFB2 Rolling: ");
        if(dice.size()==1)
        {
            Die die = dice.get(0);
            builder.append("**").append(die).append("**:\n");
            int sum = 0;
            for(int i=0; i<die.number; i++)
            {
                if(i!=0)
                    builder.append("+");
                int num = (int)(Math.random()*die.sides)+1;
                builder.append(num);
                sum+=num;
            }
            if(die.modifier!=0)
            {
                builder.append("**").append(die.modifier>0 ? "+" : "").append(die.modifier).append("**");
                sum+=die.modifier;
            }
            builder.append("\n**Sum**: ").append(sum);
        }
        else
        {
            int sum = 0;
            StringBuilder builder2 = new StringBuilder();
            for(int i=0; i<dice.size(); i++)
            {
                if(i!=0)
                {
                    builder.append("+");
                    builder2.append("+");
                }
                builder.append("(**").append(dice.get(i).toString()).append("**)");
                int roll = dice.get(i).roll();
                builder2.append("(").append(roll).append(")");
                sum+=roll;
            }
            builder.append(":\n").append(builder2.toString()).append("\n**Sum**: ").append(sum);
        }
        Sender.sendResponse(builder.toString(), event);
        return true;
    }
    
    private final String DIE_REGEX = "^(\\d{1,4})?\\s*([Dd]\\s*\\d{1,4})?\\s*([+-]\\s*\\d{1,4})?$";
    
    private class Die
    {
        int number = 1;
        int sides = 6;
        int modifier = 0;
        
        public int roll()
        {
            int total=modifier;
            for(int i=0; i<number; i++)
                total+=(Math.random()*sides+1);
            return total;
        }
        
        @Override
        public String toString() {
            return number+"d"+sides+(modifier==0 ? "" : (modifier>0 ? "+"+modifier : modifier));
        }
    }
}
