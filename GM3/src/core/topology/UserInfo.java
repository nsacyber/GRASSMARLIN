/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class UserInfo {
    
    Map<String,String> usernames;
    
    public UserInfo() {
        usernames = new HashMap<>();
    }
    
    public void addUser( String user ) {
        usernames.putIfAbsent(user, "");
    }
    
    public void addPassword( String user, String password ) {
        usernames.put(user, password);
    }
    
}
