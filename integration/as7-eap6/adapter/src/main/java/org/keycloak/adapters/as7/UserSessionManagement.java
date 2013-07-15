package org.keycloak.adapters.as7;

import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages relationship to users and sessions so that forced admin logout can be implemented
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserSessionManagement implements SessionListener
{
   private static final Logger log = Logger.getLogger(UserSessionManagement.class);
   protected ConcurrentHashMap<String, Map<String, Session>> userSessionMap = new ConcurrentHashMap<String, Map<String, Session>>();

   protected void login(Session session, String username)
   {
      Map<String, Session> map = userSessionMap.get(username);
      if (map == null)
      {
         final Map<String, Session> value = new HashMap<String, Session>();
         map = userSessionMap.putIfAbsent(username, value);
         if (map == null)
         {
            map = value;
         }
      }
      synchronized (map)
      {
         map.put(session.getId(), session);
      }
      session.addSessionListener(this);
   }

   public void logoutAll()
   {
      List<String> users = new ArrayList<String>();
      users.addAll(userSessionMap.keySet());
      for (String user : users) logout(user);
   }

   public void logoutAllBut(String but)
   {
      List<String> users = new ArrayList<String>();
      users.addAll(userSessionMap.keySet());
      for (String user : users)
      {
         if (!but.equals(user)) logout(user);
      }
   }


   public void logout(String user)
   {
      log.debug("logoutUser: " + user);
      Map<String, Session> map = userSessionMap.remove(user);
      if (map == null)
      {
         log.debug("no session for user: " + user);
         return;
      }
      log.debug("found session for user");
      synchronized (map)
      {
         for (Session session : map.values())
         {
            log.debug("invalidating session for user: " + user);
            session.setPrincipal(null);
            session.setAuthType(null);
            session.getSession().invalidate();
         }
      }

   }

   public void sessionEvent(SessionEvent event)
   {
      // We only care about session destroyed events
      if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType())
              && (!Session.SESSION_PASSIVATED_EVENT.equals(event.getType())))
         return;

      // Look up the single session id associated with this session (if any)
      Session session = event.getSession();
      GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
      if (principal == null) return;
      session.setPrincipal(null);
      session.setAuthType(null);

      String username = principal.getUserPrincipal().getName();
      Map<String, Session> map = userSessionMap.get(username);
      if (map == null) return;
      synchronized (map)
      {
         map.remove(session.getId());
         if (map.isEmpty()) userSessionMap.remove(username);
      }


   }
}
