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

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import org.json.JSONObject;
import spectra.Argument;
import spectra.Command;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Imgur extends Command {
    private final String clientId;
    public Imgur(String clientId)
    {
        this.clientId = clientId;
        this.command = "imgur";
        this.help = "uploads an image to imgur";
        this.longhelp = "This command uploads the given image URL or attachment to imgur, and returns the link of the new image.";
        this.cooldown = 60;
        this.cooldownKey = event -> event.getAuthor().getId()+"|imgur";
        this.goldlistCooldown = -1;
        this.arguments = new Argument[]{
            new Argument("url|attachment",Argument.Type.SHORTSTRING,true)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String url = (String)args[0];
        event.getChannel().sendTyping();
        if(url.startsWith("<")&&url.endsWith(">"))
            url = url.substring(1, url.length()-1);
        if(event.getMessage().getAttachments()!=null && !event.getMessage().getAttachments().isEmpty())
            try{Thread.sleep(500);}catch(Exception e){}
        HttpResponse<JsonNode> result;
        try {
            result = Unirest.post("https://api.imgur.com/3/image")
                    .header("Content-Type","application/json")
                    .header("Authorization", "Client-ID "+clientId)
                    .body(new JSONObject().put("image", url).toString())
                    .asJson();
        } catch (UnirestException ex) {
            Sender.sendResponse(SpConst.ERROR+"The imgur servers could not be reached.", event);
            return false;
        }
        if(result.getStatus()<200 || result.getStatus()>=300)
        {
            Sender.sendResponse(SpConst.ERROR+"The image could not be processed.", event);
            return false;
        }
        try{
            String link = result.getBody().getObject().getJSONObject("data").getString("link");
            Sender.sendResponse("<@"+event.getAuthor().getId()+"> \uD83D\uDCE8 Image uploaded successfully: **<"+link+">**", event);
            return true;
        }catch(Exception e)
        {
            Sender.sendResponse(SpConst.ERROR+"An invalid status was returned.", event);
            return false;
        }
    }
}
