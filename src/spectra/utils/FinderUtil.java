/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spectra.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

/**
 *
 * @author jagrosh
 */
public class FinderUtil {
    
    public static List<User> findUsers(String query, List<User> users)
    {
        String id;
        String discrim = null;
        if(query.matches("<@!?\\d+>"))
        {
            id = query.replaceAll("<@!?(\\d+)>", "$1");
            for(User u: users)
                if(u.getId().equals(id))
                    return Collections.singletonList(u);
        }
        else if(query.matches("^.*#\\d{4}$"))
        {
            discrim = query.substring(query.length()-4);
            query = query.substring(0,query.length()-5).trim();
        }
        ArrayList<User> exact = new ArrayList<>();
        ArrayList<User> wrongcase = new ArrayList<>();
        ArrayList<User> startswith = new ArrayList<>();
        ArrayList<User> contains = new ArrayList<>();
        for(User u: users)
        {
            if(discrim!=null && !u.getDiscriminator().equals(discrim))
                continue;
            if(u.getUsername().equals(query))
                exact.add(u);
            else if (u.getUsername().equalsIgnoreCase(query) && exact.isEmpty())
                wrongcase.add(u);
            else if (u.getUsername().toLowerCase().startsWith(query) && wrongcase.isEmpty())
                startswith.add(u);
            else if (u.getUsername().toLowerCase().contains(query) && startswith.isEmpty())
                contains.add(u);
        }
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<TextChannel> findTextChannel(String query, List<TextChannel> channels)
    {
        
    }
    
    public static List<Role> findRole(String query, List<Role> roles)
    {
        
    }
}
