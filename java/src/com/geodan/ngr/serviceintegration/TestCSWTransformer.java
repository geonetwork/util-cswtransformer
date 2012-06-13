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

package com.geodan.ngr.serviceintegration;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

/**
 * Test class CSWTransformer using Spring's mock HTTPServletRequest.
 * TODO convert to Junit test
 *
 * @author heikki doeleman - heikki.doeleman@geocat.net
 * 
 */
public class TestCSWTransformer {

    public static void main(String[] args){

        TestCSWTransformer t = new TestCSWTransformer();
        //t.test();
        t.test2();

    }

    private void test2() {
        try {
        URL u = new URL("http://www.nationaalgeoregister.nl");
        }
        catch(Throwable t) {
            System.out.println(t.getMessage());
        }
    }
    private void test() {
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setParameter("startPosition", "1");
            request.setParameter("maxRecords", "3");
            //request.setParameter("ElementSetName", "full");
            //request.setParameter("ElementSetName", "minimal");
            //request.setParameter("searchterms-1", "niEuWe");
            //request.setParameter("field-1", "AnyText");
            //request.setParameter("searchterms-2", "heeeeee");
            //request.setParameter("field-2", "title");
            //request.setParameter("searchterms-3", "OGC:WMS");
            //request.setParameter("searchterms-3", "OGC:WMS-1.1.1-http-get-capabilities");
            //request.setParameter("searchterms-3", "OGC:WMS-1.1.1-http-get-capabilities");
            //request.setParameter("field-3", "OnlineResourceType");
            //request.setParameter("url","http://nationaalgeoregister.nl:80/geonetwork/srv/en/csw");
            request.setParameter("url","http://nationaalgeoregister.nl/geonetwork/srv/en/csw");
            //request.setParameter("callback", "callbackfunction");

            go(request, response);
        }
        catch(Throwable x) {
            System.out.println(x.getMessage());
            x.printStackTrace();
        }
    }
    private void go(HttpServletRequest request, HttpServletResponse response) {
        try {
            CSWTransformer cswTransformer = new CSWTransformer();
            cswTransformer.doGet(request, response);
        }
        catch (ServletException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
