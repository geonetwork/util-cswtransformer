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
 
/*
* getNGRRecords vraagt aan de CSWTransformer alle records die 'keyword' bevatten.
* de functie kan 0-6 parameters aan.
* @param keyword Zoekwoord waarmee de CSW wordt doorzocht
* @param callback De callback functie die moet worden aangeroepen als het resultaat binnenkomt, default showResponse
* @param cswUrl De url naar de CSW service, default http://nationaalgeoregister.nl:80/geonetwork/srv/en/csw
* @param ngrUrl De url naar de CSWTransformer, default 
* @param shortresponse Keuzeoptie voor een kort (true) of lang (false) JSON resultaat. Een lang JSON resultaat bevat een 
*               uitgebreide beschrijvende tekst  ('description') voor elke gevonden kaartlaag. 
*/
function getNGRRecords(field, value, callback, cswUrl, ngrUrl, elementsetname, maxRecords, currentIdx){ 
	if (callback == undefined) callback = 'showResponse';
	if (cswUrl == undefined) cswUrl = 'http://nationaalgeoregister.nl:80/geonetwork/srv/en/csw';
	if (ngrUrl == undefined) ngrUrl = 'http://delphinus.site4u.nl/CSWTransformer/CSWTransformer';
	var scriptDiv = document.getElementById("ngrZoekScript");
	var head = document.getElementsByTagName("head").item(0);
//Check of er een response van een eerder request staat en dat verwijderen
	if (scriptDiv) {
		head.removeChild(scriptDiv);
	}
//Creeer een nieuw request
	var script = document.createElement("script");
		var params = "?url="+cswUrl ;
		if(elementsetname) {
			params = params + "&ElementSetName="+elementsetname;
		}
		if(maxRecords) {
			params = params + "&maxRecords="+maxRecords;
		}
		if(callback) {
			params = params + "&callback="+callback;
		}
		if(currentIdx) {
			params = params + "&currentIdx=" + currentIdx;
		}	
		if(field && value) {
			params = params + "&field-1=" + field + "&searchterms-1=" + value;
		}
		script.setAttribute("src", ngrUrl+params);
		script.setAttribute("type", "text/javascript");
		script.setAttribute("id", "ngrZoekScript");
	head.appendChild(script);
}


