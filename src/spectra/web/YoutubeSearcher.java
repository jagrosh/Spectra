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

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.utils.SimpleLog;
import spectra.SpConst;
/**
 *
 * @author John Grosh (jagrosh)
 */
public class YoutubeSearcher {
    private final YouTube youtube;
    private final Search.List search;
    public YoutubeSearcher(String apiKey)
    {
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), (HttpRequest request) -> {
        }).setApplicationName(SpConst.BOTNAME).build();
        Search.List tmp = null;
        try {
            tmp = youtube.search().list("id,snippet");
        } catch (IOException ex) {
            SimpleLog.getLog("Youtube").fatal("Failed to initialize search: "+ex.toString());
        }
        search = tmp;
        if(search!=null)
        {
            search.setKey(apiKey);
            search.setType("video");
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
        }
    }
    
    public List<String> getResults(String query, int numresults)
    {
        List<String> urls = new ArrayList<>();
        search.setQ(query);
        search.setMaxResults((long)numresults);
        
        SearchListResponse searchResponse;
        try {
            searchResponse = search.execute();
        List<SearchResult> searchResultList = searchResponse.getItems();
        searchResultList.stream().forEach((sr) -> {
            urls.add(sr.getId().getVideoId());
        });
        } catch (IOException ex) {
            SimpleLog.getLog("Youtube").fatal("Search failure: "+ex.toString());
            return null;
        }
        return urls;
    }
}
