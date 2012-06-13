/* Copyright (c) 2009 Geodan, published under the MIT license.

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

package com.geodan.ngr.serviceintegration;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * Servlet implementation class CSWNamespaceContext
 *
 */
public class CSWNamespaceContext implements NamespaceContext {

	//static Logger logger = Logger.getLogger("CSProxyLogger");
	
    public String getNamespaceURI(String prefix) {
    	//logger.debug(prefix);
    	
        if (prefix == null) throw new NullPointerException("Null prefix");
        else if ("csw".equals(prefix)) return "http://www.opengis.net/cat/csw/2.0.2";
        else if ("ogc".equals(prefix)) return "http://www.opengis.net/ogc";
        else if ("ows".equals(prefix)) return "http://www.opengis.net/ows";
        else if ("gml".equals(prefix)) return "http://www.opengis.net/gml";
        else if ("gmd".equals(prefix)) return "http://www.isotc211.org/2005/gmd";
        else if ("xlink".equals(prefix)) return "http://www.w3.org/1999/xlink";
        else if ("xsi".equals(prefix)) return "http://www.w3.org/2001/XMLSchema-instance";
        else if ("gco".equals(prefix)) return "http://www.isotc211.org/2005/gco";
        else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
        return XMLConstants.NULL_NS_URI;
    }
    
    // This method isn't necessary for XPath processing.
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    // This method isn't necessary for XPath processing either.
    @SuppressWarnings("unchecked")
	public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}
