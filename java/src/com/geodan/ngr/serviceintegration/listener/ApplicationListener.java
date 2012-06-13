/* Copyright (c) 2009 GeoCat, published under the MIT license.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. */

package com.geodan.ngr.serviceintegration.listener;

import com.geodan.ngr.serviceintegration.util.RealPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * 
 * @author heikki doeleman - heikki.doeleman@geocat.net
 *
 */
public class ApplicationListener implements ServletContextListener {

    private Log log = LogFactory.getLog(ApplicationListener.class);

	public void contextDestroyed(ServletContextEvent arg0) {
        log.debug("context destroyed");
	}

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.debug("context initializing..");        
        ServletContext servletContext = servletContextEvent.getServletContext();
        String ctxName = servletContext.getServletContextName();
        String realP = servletContext.getRealPath(ctxName);
        String fixedRealPath = realP.substring(0, realP.lastIndexOf(ctxName));
        log.debug("realpath: " + fixedRealPath);
        RealPath.init(fixedRealPath);
        log.debug("context initialized");
    }
}
