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

/**
 * Servlet implementation class WMSResource
 */
public class WMSResource extends Object{
	
	private String title;
	private String url;
	private String protocol;
	private String name;
	private String error;
	private String description;
	
	WMSResource (String title, String url, String protocol, String name, String description) {
		this.setTitle(title);
		this.setUrl(url);
		this.setProtocol(protocol);
		this.setName(name);
    this.setDescription(description);

	}
	
	WMSResource (String error){
		this.setError(error);
	}
	
	public void setError(String error){
		this.error = error;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDescription(String description) {
    this.description = description;
  }
	
	public String getUrl() {
		return url;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public String getError(){
		return error;
	}
	
	public String getDescription() {
    return description;
  }
}
