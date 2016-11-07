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
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import spectra.Argument;
import spectra.Command;
import spectra.PermLevel;
import spectra.Sender;
import spectra.SpConst;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class RestCmd extends Command {
    public RestCmd()
    {
        this.command = "rest";
        this.level = PermLevel.JAGROSH;
        this.help = "sends a REST request";
        this.longhelp = "This command makes a REST call";
        this.arguments = new Argument[]{
            new Argument("method",Argument.Type.SHORTSTRING,true),
            new Argument("url",Argument.Type.SHORTSTRING,true),
            new Argument("authorization",Argument.Type.SHORTSTRING,true),
            new Argument("body",Argument.Type.LONGSTRING, false)
        };
    }

    @Override
    protected boolean execute(Object[] args, MessageReceivedEvent event) {
        String method = ((String)args[0]).toUpperCase();
        String url = (String)args[1];
        if(url.startsWith("/"))
            url = "https://discordapp.com/api"+url;
        String auth = (String)args[2];
        String body = args[3]==null ? null : (String)args[3];
        
        try {
            HttpResponse<JsonNode> response;
            if(method.equals("GET"))
            {
                GetRequest request = Unirest.get(url).header("Content-Type", "application/json");
                switch(auth.toLowerCase())
                {
                    case "none":
                        break;
                    case "bot":
                        request.header("Authorization", event.getJDA().getAuthToken());
                        break;
                    default:
                        request.header("Authorization", auth);
                        break;
                }
                response = request.asJson();
            }
            else
            {
                HttpRequestWithBody request;
                switch(method)
                {
                    case "POST":
                        request = Unirest.post(url);
                        break;
                    case "PATCH":
                        request = Unirest.patch(url);
                        break;
                    case "DELETE":
                        request = Unirest.delete(url);
                        break;
                    case "PUT":
                        request = Unirest.put(url);
                        break;
                    default:
                        Sender.sendResponse(SpConst.ERROR+"Invalid method", event);
                        return false;
                }
                request.header("Content-Type", "application/json");
                switch(auth.toLowerCase())
                {
                    case "none":
                        break;
                    case "bot":
                        request.header("Authorization", event.getJDA().getAuthToken());
                        break;
                    default:
                        request.header("Authorization", auth);
                        break;
                }
                if(body!=null)
                    request.body(body);
                response = request.asJson();
            }
            String rbody = null;
            try{
            rbody = response.getBody().getObject().toString(2);
            }catch(Exception e){}
            Sender.sendResponse("\uD83D\uDCC7 `"+response.getStatus()+"` "+response.getStatusText()+(rbody==null ? "" : "\n```json\n"+rbody+" ```"), event);
            return true;
        }catch(UnirestException e){
            Sender.sendResponse(SpConst.ERROR+"Unirest exception",event);
            return false;
        }
    }
}
