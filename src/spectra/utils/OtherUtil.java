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
package spectra.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class OtherUtil {
    
    public static ArrayList<String> readFileLines(String filename)
    {
        BufferedReader reader;
        try{
            reader = new BufferedReader(new FileReader(filename));
        }catch(FileNotFoundException e){return null;}
        ArrayList<String> items = new ArrayList<>();
        try{
            while(true)
            {
                String next = reader.readLine();
                if(next==null)
                    break;
                items.add(next.trim());
            }
            reader.close();
            return items;
        }catch(IOException e){
            return null;
        }
    }
    
    public static File writeArchive(String text, String txtname)
    {
        File f = new File("WrittenFiles"+File.separatorChar+txtname+".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) 
        {
                String lines[] = text.split("\n");
                for(String line: lines)
                {
                    writer.write(line+"\r\n");
                }
                writer.flush();
        }catch(IOException e){System.err.println("ERROR saving file: "+e);}
        return f;
    }
    
    public static BufferedImage imageFromUrl(String url)
    {
        if(url==null)
            return null;
        try {
            URL u = new URL(url);
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36");
            //urlConnection.setRequestProperty("authorization", jda.getAuthToken());
            
            return ImageIO.read(urlConnection.getInputStream());
        } catch(IOException|IllegalArgumentException e) {
            System.err.println("[ERROR] Retrieving image: "+e);
        }
        return null;
    }
}
