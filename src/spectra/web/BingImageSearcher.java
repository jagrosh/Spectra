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
package spectra.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class BingImageSearcher {
    private final HashMap<String,Pair<ArrayList<String>,OffsetDateTime>> cache;
    private final List<String> apiKeysEncrypted;
    private final String bingImagePattern = "https://api.datamarket.azure.com/Bing/Search/v1/Image?Query=%%27%s%%27&$format=JSON";
    public BingImageSearcher(List<String> apiKeys)
    {
        cache = new HashMap<>();
        apiKeysEncrypted = new ArrayList<>();
        apiKeys.stream().forEach((key) -> {
            apiKeysEncrypted.add(Base64.getEncoder().encodeToString((key + ":" + key).getBytes()));
        });
    }
    
    public List<String> search(String query)
    {
        ArrayList<String> list;
        String truequery;
        try{
        truequery = URLEncoder.encode("'"+query+"'", Charset.defaultCharset().name());
        }catch(UnsupportedEncodingException e){
            return null;
        }
        synchronized(cache)
        {
            list = cache.get(truequery)==null ? null : cache.get(truequery).getKey();
        }
        if(list!=null)
            return list;
        try {
            //System.out.println("New Image Request: "+truequery);
            String searchUrl = String.format(bingImagePattern, truequery);
            URL url = new URL(searchUrl);
            final URLConnection connection = url.openConnection();
            connection.setRequestProperty("Authorization", "Basic " + apiKeysEncrypted.get((int)(Math.random()*apiKeysEncrypted.size())));
            
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            JSONObject json = new JSONObject(response.toString());
            final JSONObject d = json.getJSONObject("d");
            final JSONArray results = d.getJSONArray("results");
            final int resultsLength = results.length();
            list = new ArrayList<>();
            for (int i = 0; i < resultsLength && i < 20; i++) {
                final JSONObject aResult = results.getJSONObject(i);
                list.add(aResult.get("MediaUrl").toString());
            }
            synchronized(cache)
            {
                cache.put(truequery, new Pair<>(list,OffsetDateTime.now()));
            }
            return list;
        } catch (IOException | JSONException ex) {
            return null;
        }
    }
    
    public void clearCache()
    {
        synchronized(cache)
        {
            ArrayList<String> deleteList = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();
            cache.keySet().stream().forEach((truequery) -> {
                Pair<ArrayList<String>,OffsetDateTime> tuple = cache.get(truequery);
                if (now.isAfter(tuple.getValue().plusHours(6))) {
                    deleteList.add(truequery);
                }
            });
            deleteList.stream().forEach((str) -> {
                cache.remove(str);
            });
        }
    }
}
