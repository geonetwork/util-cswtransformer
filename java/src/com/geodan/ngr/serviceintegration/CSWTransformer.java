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

import com.geodan.ngr.serviceintegration.util.RealPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  Servlet that acts as a proxy to a CSW server.
 *
 * This class is a rewrite of the original implementation made by Geodan, in order to:
 *
 * - ensure it actually generates valid CSW GetRecord requests;
 * - de response moet alle elementen uit het Nederlands profiel bevatten, i.p.v. slechts 4 of 5 velden;
 * - de clients moeten willekeurige filters kunnen zetten in het request
 *
 * The implementation retains the possibility for clients to receive the original, simple response format as developed
 * by Geodan; this is why about half this class is the old code.
 *
 *
 * @author heikki doeleman
 *
 */
public class CSWTransformer extends HttpServlet {

    public CSWTransformer() {
        super();
    }
    
    private Log log = LogFactory.getLog(CSWTransformer.class);

	private enum ResultType {
	  SHORT_RESULT,
	  LONG_RESULT
	}

	/**
	 * Processes incoming HTTP request. The response is used to send content back to the browser.
	 *
	 * @param request          The HTTP request
	 * @param httpServletResponse The HTTP response
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        try {
            log.debug("doGet start");
            httpServletResponse.setContentType("text/html");
            PrintWriter responseWriter = httpServletResponse.getWriter();

            String url = request.getParameter("url");
            log.debug("url from request: " + url);
            String callback = request.getParameter("callback");
            log.debug("callback from request: " + callback);
            String currentIdx = request.getParameter("currentIdx");
            log.debug("currentIdx from request: " + currentIdx);
            if(currentIdx == null || currentIdx.length() == 0) {
                currentIdx = "1";
            }

            String inputError = "";
            String elementSetName = request.getParameter("ElementSetName");
            log.debug("ElementSetName from request: " + elementSetName);

            //
            // client requested 'minimal' output (the old Geodan output format containing only a few fields)
            //
            if(elementSetName != null && elementSetName.equalsIgnoreCase("minimal")) {
                log.debug("minimal output format requested, reverting to original Geodan implementation");
                ResultType resulttype = ResultType.SHORT_RESULT;
                String result = request.getParameter("response");
                if (result == null || result.equals(""))  {
                    resulttype = ResultType.SHORT_RESULT;
                }
                else if (result.equalsIgnoreCase("short")) {
                    resulttype = ResultType.SHORT_RESULT;
                }
                else if (result.equalsIgnoreCase("long")) {
                    resulttype = ResultType.LONG_RESULT;
                }
                else {
                    inputError = "parameter 'response' must evaluate to either 'short' or 'long'";
                }
		        if (inputError.equals("")) {
                    Map<String, String[]> requestParameters = request.getParameterMap();
                    String getRecordsRequest = createGetRecordsRequest(requestParameters);
                    String getRecordsResponse = post(getRecordsRequest, url);
                    try {
			            getResponse(responseWriter, parse(getRecordsResponse), callback, resulttype);
                    }
                    catch(Exception x) {
                        inputError = x.getMessage();
                        getResponse(responseWriter, getException(""+inputError), callback, resulttype);
                    }
		        }
                else {
			        getResponse(responseWriter, getException(inputError), callback, resulttype);
		        }
            }
            // client did not request old Geodan output format
            else {
                log.debug("no minimal output format requested, using new implementation");
                Map<String, String[]> requestParameters = request.getParameterMap();
                String getRecordsRequest = createGetRecordsRequest(requestParameters);
                String getRecordsResponse = post(getRecordsRequest, url);
                String json = xml2json(getRecordsResponse);
                log.debug("returning JSON:\n" + json);
                System.out.println("returning JSON:\n" + json);

                // There doesn't have to be an callback. If not then there shoudn't be any ()
                if (callback != null && !callback.equals("")) {
                    responseWriter.println(callback + "(" + json + "," + currentIdx + ")");
                } else {
                    responseWriter.println(json);
                }
                responseWriter.flush();
            }
            log.debug("doGet end");
        }
        catch(Exception x) {
            log.error(x.getMessage());
            System.out.println(x.getMessage());
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    /**
     * Transforms XML into JSON.
     *
     * @param xml		string representation of xml
     * @return			xml transformed to json
     * @throws Exception  in case of exception
     */

    private String xml2json(String xml) throws Exception {

        // Use Saxon for XSLT 2.0
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        // Create a transform factory instance
        TransformerFactory tFactory = TransformerFactory.newInstance();
        // Create a transformer for the stylesheet
        Transformer transformer = tFactory.newTransformer(new StreamSource(new File(RealPath.getRealPath() + "/xml-to-json.xsl")));
        // Transform the source XML
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(os));
        return new String(os.toByteArray(), "UTF-8");
    }
    
    /**
     * POSTs the xml request to the given url and returns a String representation of the response.
     *
     * @param xml_request	the request contains the search criteria
     * @param url			the url to POST the request to
     * @return				returns a String representation of the response
     * @throws IOException in case of exception
     */
    private String post(String xml_request, String url) throws IOException {
            // Create an URL object
            log.debug("creating request to URL: " + url);
            URL csUrl = new URL(url);

            // Create an HttpURLConnection and use the URL object to POST the request
            HttpURLConnection csUrlConn = (HttpURLConnection) csUrl.openConnection();
            csUrlConn.setDoOutput(true);
            csUrlConn.setDoInput(true);
            csUrlConn.setRequestMethod("POST");
            csUrlConn.setRequestProperty("Content-Type", "text/xml");

            // Create a OutputStream to actually send the request and close the streams
            OutputStream out = csUrlConn.getOutputStream();
            OutputStreamWriter wout = new OutputStreamWriter(out, "UTF-8");
            wout.write(xml_request);
            wout.flush();
            out.close();

            // Get a InputStream and parse it in the parse method
            InputStream is = csUrlConn.getInputStream();
            String response = "";
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                String line;

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                finally {
                    is.close();
                }
                response = sb.toString();
            }
            // Close Streams and connections
            out.close();
            csUrlConn.disconnect();

            if(response != null && response.length() > 0) {
                log.debug("GetRecords response:\n" + response);
            }
            else {
                log.debug("GetRecords response empty");
            }

            // Returns response
            return response;
    }
	/**
	 * Gets exceptions in JSON format that can be used in the getResponse method
	 * @param exception    error message
	 * @return List<WMSResource>
	 */
	protected List<WMSResource> getException(String exception) {
		List<WMSResource> wmsresourcelist = new ArrayList<WMSResource>();
		WMSResource wmsresource = new WMSResource(
				"\"error\": \"" + exception + "\"}}");
		wmsresourcelist.add(wmsresource);

		return wmsresourcelist;
	}

    /**
     * Creates a CSW 2.0.2 GetRecords request operation.
     *
     * @param parameters    the search criteria. This is a list of strings that may contain the following:
     *
     *                      startPosition - as in OGC 07-006 CSW 2.0.2. Default value is 1 ;
     *                      maxRecords - as in OGC 07-006 CSW 2.0.2. Default value is 10 ;
     *                      ElementSetName - as in OGC 07-006 CSW 2.0.2. Values 'brief', 'summary' or  'full', default value is 'full' ; also the value 'minimal'
     *                                      is supported, in which case only a few fields from the metadata are returned, as in the old Geodan implementation ;
     *                      searchterms-1 .. searchterms-X - space-separated list(s) of terms to search. If none are present, no filter is created ; the index
     *                                                      number suffix relates the searchterms to a specific field ;
     *                      field-1 .. field-X - the field(s) to search in ; the index number suffix relates the field to specific searchterms ; if there are
     *                                          searchterms and/or fields without matching field and/or searchterm, they are ignored.
     *
     * @return			a GetRecords request that can be used to request data from a server.
     */
    private String createGetRecordsRequest(Map<String, String[]> parameters) {
        //
        // retrieve search parameters. If they are not present, or have incorrect values, then default values are used.
        //
        String startPosition = null;
        if(parameters.get("startPosition") != null) {
            startPosition = parameters.get("startPosition")[0];
        }
        if(startPosition == null) {
            startPosition = "1";
        }
        try {
            Integer.parseInt(startPosition);
        }
        catch(NumberFormatException x) {
            startPosition = "1";
        }
        String maxRecords = null;
        if(parameters.get("maxRecords") != null) {
            maxRecords = parameters.get("maxRecords")[0];
        }
        if(maxRecords == null) {
            maxRecords = "10";
        }
        try {
            Integer.parseInt(maxRecords);
        }
        catch(NumberFormatException x) {
            maxRecords = "10";
        }
        String elementSetName = null;
        if(parameters.get("ElementSetName") != null) {
            elementSetName = parameters.get("ElementSetName")[0];
        }
        if(elementSetName == null) {
            elementSetName = "full";
        }
        if( !(elementSetName.equals("brief")
                || elementSetName.equals("summary")
                || elementSetName.equals("full")) ) {
            elementSetName = "full";
        }

        String request =
                  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" service=\"CSW\" version=\"2.0.2\" "
                    + " maxRecords= \"" + maxRecords + "\""
                    + " startPosition = \"" + startPosition + "\""
                    + " resultType=\"results\" outputSchema=\"csw:IsoRecord\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + " xsi:schemaLocation=\"http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd\">"
                    + "<csw:Query typeNames=\"csw:Record\">"
                    + "<csw:ElementSetName>" + elementSetName +"</csw:ElementSetName>" ;

        boolean constraintAndFilterAdded = false ;
        List<String> filters = new ArrayList<String>();

        for(String key : parameters.keySet()) {
            if (key.startsWith("searchterms-")) {
                String searchtermsIndex = key.substring(key.indexOf('-') + 1);
                String fieldKey = "field-" + searchtermsIndex;
                if (parameters.containsKey(fieldKey)) {
                    if (!constraintAndFilterAdded) {
                        request += "<csw:Constraint version=\"1.1.0\">"
                                + "<Filter xmlns=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">";
                        constraintAndFilterAdded = true;
                    }
                    filters.add(createFilter(parameters.get(key)[0], parameters.get(fieldKey)[0]));
                }
            }
        }
        //
        // More than 1 searchterms/field parameter pairs are conjoined (AND) in the GetRecords request
        //
        if(filters.size() > 1) {
            request += "<And>";
        }
        for(String filter : filters) {
            request += filter;
        }
        if(filters.size() > 1) {
            request += "</And>";
        }
        if(constraintAndFilterAdded) {
            request += "</Filter>"
                    + "</csw:Constraint>";
        }
        request += "</csw:Query>"
                + "</csw:GetRecords>";

        log.debug("Created GetRecords request:\n" + request);

        return request ;
    }

    /**
     * Whether a Lucene index field or a CSW queryable is analyzed in GeoNetwork's index.
     *
     * @param field
     * @return
     */
    private boolean isNotAnalyzed(String field) {
        return field.equals("protocol")
                || field.equals("_createDate")
                || field.equals("accessConstr")
                || field.equals("changeDate")
                || field.equals("keyword")
                || field.equals("createDate")
                || field.equals("subject")
                || field.equals("modified")
                || field.equals("revisiondate")
                || field.equals("creationdate")
                || field.equals("publicationdate")
                || field.equals("topicCategory")
                || field.equals("topicCat")
                || field.equals("servicetype")
                || field.equals("OnlineResourceType")
                || field.equals("MetadataPointOfContact")
                || field.equals("metadataPOC");
    }

    /**
     * Creates a filter (or rather, the content of a <Filter> element).
     *
     * This method makes a distinction between fields that are analyzed in GeoNetwork's index, and that are not. The reason is that
     * analyzed fields are indexed lower-case, and characters like '-' and ':' are used as token separators. The non-analyzed fields
     * are indexed preserving case. GeoNetwork's CSW server does not apply the same analyzers to search terms as were applied to the
     * index field when indexing (this is a bug). To work around that, if the GetRecords filter is searching a non-analyzed field, it
     * uses a <PropertyIsEqualTo> filter element -- GeoNetwork's CSW server handles those preserving case. For analyzed fields, it
     * uses a <PropertyIsLike> element: GeoNetwork lowercases those before using them in its Lucene search.
     *
     * When this is fixed in GeoNetwork (and merged to NGR) it is recommended to remove this workaround.
     *
     * @param searchTerms
     * @param field
     * @return string representation of the content of an OGC Filter element
     */
    private String createFilter(String searchTerms, String field) {
        String filter = "";

        if(isNotAnalyzed(field)) {
            String[] wordList = null;
            if(searchTerms.length() > 0) {
                wordList = searchTerms.split("[\\s]+");
            }
            if(wordList != null) {
                //
                // More than 1 searchterm in the same field are disjoined (OR) in the GetRecords request
                //
                if(wordList.length > 1) {
                    filter += " <Or>";
                }
                for (String word : wordList) {
                    filter += "<PropertyIsEqualTo matchCase=\"false\">"
                    + "<PropertyName>" + field + "</PropertyName>"
                    + "<Literal>"
                    + word
                    + "</Literal>"
                    + "</PropertyIsEqualTo>";
                }
                if(wordList.length > 1) {
                    filter += " </Or>";
                }
            }
        }
        else {
            String cleanText = searchTerms.replaceAll("\\W", " ").trim();
            String[] wordList = null;
            if(cleanText.length() > 0) {
                wordList = cleanText.split("[\\s,-]+");
            }
            if(wordList != null) {
                //
                // More than 1 searchterm in the same field are disjoined (OR) in the GetRecords request
                //
                if(wordList.length > 1) {
                    filter += " <Or>";
                }
                for (String word : wordList) {
                    filter += "<PropertyIsLike wildCard=\"%\" singleChar=\"_\" escapeChar=\"\\\">"
                    + "<PropertyName>" + field + "</PropertyName>"
                    + "<Literal>%"
                    + word
                    + "%</Literal>"
                    + "</PropertyIsLike>";
                }
                if(wordList.length > 1) {
                    filter += " </Or>";
                }
            }
        }
        return filter;
    }

//
//  ===================================================================================================================
//  Old Geodan code below (still used if client requests ElementSetName = 'minimal') -- but it seems it does not work, I always get an error.
//

    /**
     * Gets the title of the dataset from the element "el". Because the
     * title is in a different part of the XML we use getParentNode.
     * We were not successful using XPath.
     * With getParentNode we navigate to the element "MD_Metadata" from
     * there we go to "gmd:MD_DataIdentification" where we look for the title
     *
     * @param el	the element that contains the title
     * @return		title of the element
     * @throws Exception in case of exception
     */
    protected String findTitle(Element el) throws Exception {
        String title = null;
        // CI.online
        // ......trasferoptions...transferoptions.distribution....distriinfo...metadata
        Element elem_MD_Metadata = (Element) el.getParentNode().getParentNode().getParentNode().getParentNode().getParentNode().getParentNode();
        NodeList nl_di = elem_MD_Metadata.getElementsByTagName("gmd:MD_DataIdentification");

        if (nl_di.getLength() > 0) {
            title = getValue((Element) nl_di.item(0), "gmd:title");
        }
        return title;
    }

  /**
   * Gets the description (abstract) of the dataset from the element "el". Because the
   * abstract is in a different part of the XML we use getParentNode.
   * We were not successful using XPath.
   * With getParentNode we navigate to the element "MD_Metadata" from
   * there we go to "gmd:MD_DataIdentification" where we look for the abstract
   *
   * @param el  the element that contains the title
   * @return    title of the element
   * @throws Exception in case of exception
   */
  protected String findDescription(Element el) throws Exception {
    String description = null;
    // CI.online
    // ......trasferoptions...transferoptions.distribution....distriinfo...metadata
    Element elem_MD_Metadata = (Element) el.getParentNode().getParentNode().getParentNode().getParentNode().getParentNode().getParentNode();
    NodeList nl_di = elem_MD_Metadata.getElementsByTagName("gmd:MD_DataIdentification");

    if (nl_di.getLength() > 0) {
      description = getValue((Element) nl_di.item(0), "gmd:abstract");
    }
    return description;
  }

  /**
   * Escapes text for use in Javascript
   *
   * @param text  the text to be escaped
   * @return      the escaped text
   */
  private String jsEscape (String text) {
    String result = text.replaceAll("\"", "\\\"");
    result = result.replaceAll("\n", "\\\\n");
    return result;
  }


    /**
     * Gets the value from the node "tag" from the element "el"
     * and returns it as a String.
     *
     * @param el		the element that contains the value
     * @param tag		the node from the value is get
     * @return			the value from the given tag
     * @throws Exception in case of exception
     */
    protected String getValue(Element el, String tag) throws Exception {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl != null && nl.item(0) != null) {
            String value = nl.item(0).getTextContent();
            return value.trim();
        }
        // TODO a bit better....
        throw new Exception("No value found for " + tag);
    }

    /**
     * Parses the InputStream to valid WMSResponses and returns them in a list using XPath.
     *
     * This method is essentially unchanged from the old Geodan implementation except it now parses a String.
     *
     * @param response		the String that contains the response from the GetRecords request
     * @return			a List that contains WMSResources or errors
     * @throws java.io.IOException in case of exception
     * @throws javax.xml.parsers.ParserConfigurationException in case of exception
     * @throws org.xml.sax.SAXException in case of exception
     */
    private List<WMSResource> parse(String response) throws ParserConfigurationException, IOException, SAXException {

        List<WMSResource> wmsresourcelist = new ArrayList<WMSResource>();

        //
        // Create DOM document from response
        //
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputSource source = new InputSource(new StringReader(response));
        Document xmldoc = factory.newDocumentBuilder().parse(source);

        // With XPath get the WMS data
        XPath xpath = XPathFactory.newInstance().newXPath();
        NamespaceContext nsContext = new CSWNamespaceContext();
        xpath.setNamespaceContext(nsContext);

        // The XML parse expression
        String expr = "/GetRecordsResponse/SearchResults/MD_Metadata/distributionInfo/MD_Distribution/transferOptions/MD_DigitalTransferOptions/onLine/CI_OnlineResource";
        try {
            // Run XPath
            NodeList nodes = (NodeList) xpath.evaluate(expr, xmldoc, XPathConstants.NODESET);

            // Check for nodes else generate error no records available
            if (nodes != null && nodes.getLength() > 0) {

                // Get for every node the values if no name and URL are available then generate a error. If the title has no value then the name can be the title.
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element el = (Element) nodes.item(i);
                    String title = findTitle(el);
                    String description = findDescription(el);
                    String url = getValue(el, "gmd:linkage");
                    String protocol = getValue(el, "gmd:protocol");
                    String name = getValue(el, "gmd:name");
                    if (name != null && url != null && !name.equals("") && !url.equals("")) {
                        WMSResource wmsresource = new WMSResource(title, url, protocol, name, description);
                        if (title == null) {
                            wmsresource.setTitle(name);
                        }
                        wmsresourcelist.add(wmsresource);
                    }
                    else {
                        // Because there already is an List<WMSRecords> the method "getException" will not be used
                        WMSResource wmsresource = new WMSResource(" \"records\": []}}");
                        wmsresourcelist.add(wmsresource);
                    }
                }
            }
            else {
                // Because there already is an List<WMSRecords> the method "getException" will not be used
                WMSResource wmsresource = new WMSResource(" \"records\": []}}");
                wmsresourcelist.add(wmsresource);
            }
        }
        catch (XPathExpressionException e) {
            // Because there already is an List<WMSRecords> the method "getException" will not be used
            WMSResource wmsresource = new WMSResource( "\"error\": \"There is an XPathExpressionException\"}}");
            wmsresourcelist.add(wmsresource);
        }
        catch (Exception e) {
            log.error("ERROR: "+ e.getMessage());
            e.printStackTrace();
            // Because there already is an List<WMSRecords> the method "getException" will not be used
            WMSResource wmsresource = new WMSResource("\"error\": \"The result could not be parsed\"}}");
            wmsresourcelist.add(wmsresource);
        }
        // Return the results
        return wmsresourcelist;
    }

    /**
     * Generates a JSON response and sends that back to the browser. The JSON can contain records or errors.
     *
     * @param rw		used to send a response back to the browser
     * @param wmsList	containing all the results from the request
     * @param callback	used as a header for the JSON
     * @param resulttype if LONG_RESULT then insert extra field 'description' into JSON response
     */
    private void getResponse(PrintWriter rw, List<WMSResource> wmsList, String callback, ResultType resulttype) {

        String wmsResponse = "";
        String errorResponse = "";
        AtomicReference<String> response = new AtomicReference<String>("");

        // Used to indicate that a valid response is found
        boolean responseFound = false;

        wmsResponse += " \"records\": [";
        for (int i = 0; i < wmsList.size(); i++) {
            WMSResource wms = wmsList.get(i);

            // If the name and url are null then there is an error.
            // Otherwise, a valid response was found and a valid JSON response is generated

            if (wms.getName() != null && wms.getUrl() != null && !wms.getName().equals("") && !wms.getUrl().equals("") ) {

                responseFound = true;

                wmsResponse += "{\"wmsurl\": \"" + wms.getUrl()
                        + "\", \"name\": \"" + wms.getName()
                        + "\",  \"title\": \"" + wms.getTitle();
                if (resulttype == ResultType.LONG_RESULT) {
                  // insert abstract into resulting response
                  wmsResponse += "\", \"description\": \"" + jsEscape(wms.getDescription());
                }
                wmsResponse += "\"}";
                if ((i + 1) < wmsList.size() && wmsList.get(i + 1).getError() == null) {
                    wmsResponse += ", ";
                }
                else {
                    wmsResponse += "]}}";
                }
            }
            else {
                errorResponse = wms.getError();
            }
        }

        // If a valid response is found then place the records in the
        // response else place an error in the response
        if (responseFound) {
            response.set("{ \"response\": {" + wmsResponse);
        }
        else {
            response.set("{ \"response\": {" + errorResponse);
        }

        log.debug(response.get());

        // There doesn't have to be an callback. If not then there shouldn't be any ()
        if (callback != null && !callback.equals("")) {
           rw.println(callback + "(" + response + ")");

        }
        else {
           rw.println(response.get());
        }
        rw.flush();
    }
}