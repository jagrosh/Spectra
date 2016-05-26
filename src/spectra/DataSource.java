/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author John Grosh (jagrosh)
 */
public abstract class DataSource {
    final protected ArrayList<String[]> data = new ArrayList<>();
    protected String filename = "discordbot.null";
    protected boolean save = false;
    protected int size;
    
    
    protected boolean changed;
    
    /*public synchronized void remove(String[] item)
    {
        data.remove(item);
        changed=true;
    }
    
    public synchronized String[] get(int index)
    {
        return data.get(index).clone();
    }
    
    public synchronized ArrayList<String[]> getCopy()
    {
        return new ArrayList<>(data);
    }
    
    public synchronized void add(String[] item)
    {
        data.add(item);
        changed = true;
    }
    
    public synchronized void removeAll(Collection<String[]> items)
    {
        data.removeAll(items);
        changed = true;
    }
    
    public synchronized boolean contains(String[] item)
    {
        return data.contains(item);
    }*/
    
    
    
    public boolean read()
    {
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new FileReader(filename));
        }catch(FileNotFoundException e){System.err.println("WARNING - "+filename+" not found : "+e.toString());}
        ArrayList<String[]> newData = new ArrayList<>();
        if(reader!=null)
        {
            try{
                String str;
                do{
                str = reader.readLine();
                if(str!=null && !str.trim().isEmpty())
                {
                    String[] stra = Arrays.copyOf(str.split((char)31+""),size);
                    for(int i=0;i<stra.length;i++)
                        stra[i]=stra[i] == null ? "" : stra[i].replaceAll((char)30+"", "\n");
                    newData.add(stra);
                }
                }while(str!=null);
                reader.close();
                synchronized(data)
                {
                    data.clear();
                    data.addAll(newData);
                }
                return true;
            }catch(IOException e){}
        }
        return false;
    }
    
    public boolean write()
    {
        ArrayList<String[]> copy;
        synchronized(data)
        {
            copy = new ArrayList<>(data);
        }
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            for(String[] s: copy)
            {
                String str = s[0];
                for(int i=1;i<s.length;i++)
                {
                    str+=(char)31;
                    if(s[i]!=null)
                        str+=s[i].replace((char)30+"","").replace((char)31+"","").replace("\n", (char)30+"");
                }
                writer.write(str);
                writer.newLine();
            }
            writer.flush();
            writer.close();
            }catch(IOException e){System.err.println("Error writing to "+filename); return false;}
        return true;
    }
    
}
