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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author John Grosh (jagrosh)
 */
public abstract class DataSource {
    final protected ArrayList<String[]> data = new ArrayList<>();
    protected String filename = "discordbot.null";
    protected boolean save = true;
    protected int size;
    
    ExecutorService filewriting = Executors.newSingleThreadExecutor();
    boolean writeScheduled = false;
    
    protected DataSource(){}
    
    public void setToWrite()
    {
        if(!writeScheduled)
        {
            writeScheduled = true;
            filewriting.submit(new Thread(){
                @Override
                public void run()
                {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ex) {}
                    write();
                    writeScheduled=false;
                }
            });
        }
    }
    
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
    
    private boolean write()
    {
        ArrayList<String[]> copy;
        synchronized(data)
        {
            copy = new ArrayList<>(data);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
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
        }catch(IOException e){System.err.println("Error writing to "+filename); return false;}
        return true;
    }
    
    public void shutdown()
    {
        filewriting.shutdown();
    }
}
