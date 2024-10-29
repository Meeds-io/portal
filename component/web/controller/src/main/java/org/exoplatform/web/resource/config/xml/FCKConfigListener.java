package org.exoplatform.web.resource.config.xml;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


/**
 * todo : julien : is it still used ?
 */
public class FCKConfigListener implements ServletContextListener {
    public static String FCK_CONTEXT_PATH;

    public void contextDestroyed(ServletContextEvent event) {

    }

    public void contextInitialized(ServletContextEvent event) {
        FCK_CONTEXT_PATH = event.getServletContext().getContextPath();
    }

}
