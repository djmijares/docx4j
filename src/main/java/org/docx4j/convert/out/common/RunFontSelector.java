package org.docx4j.convert.out.common;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.docx4j.dml.TextFont;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.model.properties.Property;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.ThemePart;
import org.docx4j.wml.CTLanguage;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.STHint;
import org.docx4j.wml.STTheme;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * Apply the appropriate font to the characters in the run,
 * following the rules specified in
 * http://webapp.docx4java.org/OnlineDemo/ecma376/WordML/rFonts.html
 * and http://msdn.microsoft.com/en-us/library/ff533743.aspx
 * ([MS-OI29500] 2.1.87)
 * 
 * See also http://blogs.msdn.com/b/officeinteroperability/archive/2013/04/22/office-open-xml-themes-schemes-and-fonts.aspx
 * 
 * @author jharrop
 *
 */
public class RunFontSelector {

	private WordprocessingMLPackage wordMLPackage;
	private Logger log;
	
	protected RunFontSelector(WordprocessingMLPackage wordMLPackage, Logger logger) {
		this.wordMLPackage = wordMLPackage;
		this.log = logger;
		
		if (wordMLPackage.getMainDocumentPart().getDocumentSettingsPart()!=null) {
			themeFontLang = wordMLPackage.getMainDocumentPart().getDocumentSettingsPart().getContents().getThemeFontLang();
		}
		
	}
	
	CTLanguage themeFontLang = null;
	
	public final static String CSS_NAME = "font-family"; 
	public final static String FO_NAME  = "font-family"; 

	public String getCssName() {
		return CSS_NAME;
	}
	
	
	private ThemePart getThemePart() {
		return wordMLPackage.getMainDocumentPart().getThemePart();
	}
	
    public DocumentFragment fontSelector(RPr rPr, String text) {
    	// Do we need boolean major arg??
    	
    	// TODO use effective rPr, but don't inherit theme val
    	
    	/* eg
    	 * 
				<w:r>
				  <w:rPr>
				    <w:rFonts w:ascii="Courier New" w:cs="Times New Roman" />
				  </w:rPr>
				  <w:t>English العربية</w:t>
				</w:r>
				
    	 */

		Document document = getDocument();
		
		// No rPr, so don't set the font
		if (rPr==null) {
		    Element	span = document.createElement("span");
	    	document.appendChild(span);   
	    	span.setTextContent(text);
			return result(document);
		}
		
		RFonts rFonts = rPr.getRFonts();
		
    	
    	/* If the run has the cs element ("[ISO/IEC-29500-1] §17.3.2.7; cs") 
    	 * or the rtl element ("[ISO/IEC-29500-1] §17.3.2.30; rtl"), 
    	 * then the cs (or cstheme if defined) font is used, 
    	 * regardless of the Unicode character values of the run’s content.
    	 */
    	if (rPr.getCs()!=null || rPr.getRtl()!=null ) {
    		
    		// use the cs (or cstheme if defined) font is used
    		if (rFonts==null) {
    			// TODO inherit from styles
    			
    			// What to do?  This'll do for now..
    		    Element	span = document.createElement("span");
    	    	document.appendChild(span);   
    	    	span.setTextContent(text);
    			return result(document);
    			
    			
    		} else if (rFonts.getCstheme()!=null) {
    			
    			String fontName = null; 
    			if (getThemePart()!=null) {
    				
    				fontName = getThemePart().getFont(rFonts.getCstheme(), themeFontLang);
    			}
    			if (fontName==null) {
    				fontName = rFonts.getCs();
    			}
    			if (fontName==null) {
    				// then what?
    			}    		
    			
    		    Element	span = document.createElement("span");
    	    	document.appendChild(span);   
    	    	span.setTextContent(text);
    	    	span.setAttribute("style", getCssProperty(fontName));
    			return result(document);
    			
    		} else if (rFonts.getCs()!=null) {

    			String fontName =rFonts.getCs();
    					
    		    Element	span = document.createElement("span");
    	    	document.appendChild(span);   
    	    	span.setTextContent(text);
    	    	span.setAttribute("style", getCssProperty(fontName));
    			return result(document);
    			
    		} else {
    			// No CS value.
    			// What to do?
    		}
    	}

		String eastAsia = null;
		String ascii = null;
		String hAnsi = null;
		STHint hint = null;

		if (rFonts!=null) {
			
			hint = rFonts.getHint(); 
			
			if (rFonts.getEastAsiaTheme()!=null) {
				eastAsia = getThemePart().getFont(rFonts.getEastAsiaTheme(), themeFontLang);
			} else {
				// No theme, so 
	    		eastAsia = rFonts.getEastAsia();
			}
			
			if (rFonts.getAsciiTheme()!=null) {
				ascii = getThemePart().getFont(rFonts.getAsciiTheme(), themeFontLang);
			} else {
				// No theme, so 
				ascii = rFonts.getAscii();
			}
			
			if (rFonts.getHAnsiTheme()!=null) {
				hAnsi = getThemePart().getFont(rFonts.getHAnsiTheme(), themeFontLang);
			} else {
				// No theme, so 
				hAnsi = rFonts.getHAnsi();
			}
    	}    	
		
    	/*
    	 * If the eastAsia (or eastAsiaTheme if defined) attribute’s value is “Times New Roman”
    	 * and the ascii (or asciiTheme if defined) and hAnsi (orhAnsiTheme if defined) attributes are equal, 
    	 * then the ascii (or asciiTheme if defined) font is used.
    	 */
		if (("Times New Roman").equals(eastAsia)) {
		
    		if (ascii!=null
    				&& ascii.equals(hAnsi)) {
    			// use ascii
    			
    		    Element	span = document.createElement("span");
    	    	document.appendChild(span);   
    	    	span.setTextContent(text);
    	    	span.setAttribute("style", getCssProperty(ascii));
    			return result(document);
    			
    		}
		}
    		    	
    	/* Otherwise, the following table is used. For all ranges not listed in the following table, 
    	 * the hAnsi (or hAnsiTheme if defined) font shall be used.
    	 */
		
		String langEastAsia = null;
		if (rPr.getLang()!=null) {
			langEastAsia = rPr.getLang().getEastAsia();
		}
		
		return unicodeRangeToFont( document,  text,  hint,  langEastAsia,
	    		 eastAsia,  ascii,  hAnsi );
//		return result(document);
    }
    	
    
    	
    private DocumentFragment unicodeRangeToFont(Document document, String text, STHint hint, String langEastAsia,
    		String eastAsia, String ascii, String hAnsi ) {
    	
    	DocumentFragment df = document.createDocumentFragment();
    	
    	// See http://stackoverflow.com/questions/196830/what-is-the-easiest-best-most-correct-way-to-iterate-through-the-characters-of-a
    	// and http://stackoverflow.com/questions/8894258/fastest-way-to-iterate-over-all-the-chars-in-a-string
    	
    	// The ranges specified at http://msdn.microsoft.com/en-us/library/ff533743.aspx
    	// are from 0000-FFFF, so here we'll assume there are no characters outside 
    	// Unicode Basic Multilingual Plane...
    	
    	char currentRangeLower='\u0000';
    	char currentRangeUpper='\u0000';
    	
    	StringBuilder sb = new StringBuilder(1024); 
    	Element	span = document.createElement("span");
    	
    	boolean spanReusable = true;
    	
    	for (int i = 0; i < text.length(); i++){
    		
    	    char c = text.charAt(i);        
    	    if (spanReusable && 
    	    		c>=currentRangeLower && c<=currentRangeUpper) {
    	    	// Add it to existing
    	    	sb.append(c);
    	    } else {
    	    	
    	    	// Populate previous span
    	    	if (sb.length()>0) {
        	    	df.appendChild(span);   
        	    	span.setTextContent(sb.toString()); 
        	    	log.info("span: " + sb.toString()); 
        	    	sb.setLength(0);
    	    	}
    	    	
    	    	// Create new span
    		    span = document.createElement("span");
    		    spanReusable = true; 
    		    // .. Basic Latin
        	    if (c>='\u0000' && c<='\u007F') 
        	    {
        	    	span.setAttribute("style", getCssProperty(ascii)); // TODO ascii or implementationDefault
        	    	sb.append(c);
        	    	
        	    	currentRangeLower = '\u0000';
        	    	currentRangeUpper = '\u007F';
        	    } else 
    		    // ..  Latin-1 Supplement
        	    if (c>='\u00A0' && c<='\u00FF') 
        	    {
        	    	/* hAnsi (or hAnsiTheme if defined), with the following exceptions:
						If hint is eastAsia, the following characters use eastAsia (or eastAsiaTheme if defined): A1, A4, A7 – A8, AA, AD, AF, B0 – B4, B6 – BA, BC – BF, D7, F7
						If hint is eastAsia and the language of the run is either Chinese Traditional or Chinese Simplified, the following characters use eastAsia (or eastAsiaTheme if defined): E0 – E1, E8 – EA, EC – ED, F2 – F3, F9 – FA, FC
						*/
        	    	if (hint == STHint.EAST_ASIA) {
        	    		if (langEastAsia.equals("zh") ) {
        	    			// the following characters use eastAsia (or eastAsiaTheme if defined): E0 – E1, E8 – EA, EC – ED, F2 – F3, F9 – FA, FC
        	    			if ( (c>='\u00E0' && c<='\u00E1')         	    					
        	    					|| (c>='\u00E8' && c<='\u00EA')         	    					
        	    					|| (c>='\u00EC' && c<='\u00ED')         	    					
        	    					|| (c>='\u00F2' && c<='\u00F3')         	    					
        	    					|| (c>='\u00F9' && c<='\u00FA') 
        	    					|| c=='\u00FC') {
        	    				span.setAttribute("style", getCssProperty(eastAsia));	
        	    				spanReusable = false;
        	    			} else {
        	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
        	    			}
        	    			
        	    		} else // A1, A4, A7 – A8, AA, AD, AF, B0 – B4, B6 – BA, BC – BF, D7, F7
        	    			if ( c=='\u00A1' || c=='\u00A4' 
    	    					|| (c>='\u00A7' && c<='\u00A8')         	    					
    	    					|| c=='\u00AA' || c=='\u00AD' || c=='\u00AF'          	    					
    	    					|| (c>='\u00B0' && c<='\u00B4')         	    					
    	    					|| (c>='\u00B6' && c<='\u00BA') 
    	    					|| (c>='\u00BC' && c<='\u00BF') 
    	    					|| c=='\u00D7' || c=='\u00F7' ) {
        	    				
	    	    				span.setAttribute("style", getCssProperty(eastAsia));			
	    	    				spanReusable = false;
	    	    			}  else {
	    	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
	    	    			}
        	    	} else {
	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
        	    	}
        	    	sb.append(c);
        	    	
        	    	currentRangeLower = '\u0000';
        	    	currentRangeUpper = '\u007F';
        	    } else 
			    // ..  Latin Extended-A, Latin Extended-B, IPA Extensions
	    	    if (c>='\u0100' && c<='\u02AF') 
	    	    {
	    	    	/* hAnsi (or hAnsiTheme if defined), with the following exception:
						If hint is eastAsia, and the language of the run is either Chinese Traditional or Chinese Simplified, 
						or the character set of the eastAsia (or eastAsiaTheme if defined) font is Chinese5 or GB2312 
						then eastAsia (or eastAsiaTheme if defined) font is used.
						*/
        	    	if (hint == STHint.EAST_ASIA) {
	    	    		if ("zh".equals(langEastAsia) ) {
	    	    				span.setAttribute("style", getCssProperty(eastAsia));	
	    	    				spanReusable = false;
	    	    			
	    	    		// else TODO: "or the character set of the eastAsia (or eastAsiaTheme if defined) font is Chinese5 or GB2312" 
	    	    		// fetch the character set!?
	    	    			
	    	    		} else {
		    				span.setAttribute("style", getCssProperty(hAnsi));			
		    				spanReusable = false;
		    			} 
	    	    	} else {
	    	    		// Usual case
	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
	    	    	}
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u0100';
	    	    	currentRangeUpper = '\u02AF';
        	    } else 
	    	    if (c>='\u02B0' && c<='\u04FF') 
	    	    {
        	    	if (hint == STHint.EAST_ASIA) {
	    	    		span.setAttribute("style", getCssProperty(eastAsia));	
	    	    	} else {
	    	    		// Usual case
	    				// TODO .. do what???      	    		
	    	    	}
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u02B0';
	    	    	currentRangeUpper = '\u04FF';
	    	    }
	    	    else if (c>='\u0590' && c<='\u07BF') 
	    	    {
        	    	span.setAttribute("style", getCssProperty(ascii)); // TODO ascii or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u0590';
	    	    	currentRangeUpper = '\u07BF';
	    	    }
	    	    else if (c>='\u1100' && c<='\u11FF') 
	    	    {
        	    	span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u1100';
	    	    	currentRangeUpper = '\u11FF';
	    	    } else if (c>='\u1E00' && c<='\u1EFF') 
	    	    {
        	    	if (hint == STHint.EAST_ASIA) {
	    	    		if ("zh".equals(langEastAsia) ) {
	    	    				span.setAttribute("style", getCssProperty(eastAsia));	
	    	    		} else {
		    				span.setAttribute("style", getCssProperty(hAnsi));			
		    			} 
	    	    	} else {
	    	    		// Usual case
	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
	    	    	}
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u1E00';
	    	    	currentRangeUpper = '\u1EFF';
	    	    }
	    	    else if (c>='\u2000' && c<='\u2EFF') 
	    	    {
        	    	if (hint == STHint.EAST_ASIA) {
	        	    	span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	} else {
	    	    		// Usual case
	    				// TODO .. do what???      	    			    	    		
	    	    	}
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u2000';
	    	    	currentRangeUpper = '\u2EFF';
	    	    }
	    	    else if (c>='\u2F00' && c<='\uDFFF') 
	    	    {
	        	    span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\u2F00';
	    	    	currentRangeUpper = '\uDFFF';
	    	    }
	    	    else if (c>='\uE000' && c<='\uF8FF') 
	    	    {
        	    	if (hint == STHint.EAST_ASIA) {
	        	    	span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	} else {
	    	    		// Usual case
	    				// TODO .. do what???      	    			    	    		
	    	    	}
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\uE000';
	    	    	currentRangeUpper = '\uF8FF';
	    	    }
	    	    else if (c>='\uF900' && c<='\uFAFF') 
	    	    {
	        	    span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\uF900';
	    	    	currentRangeUpper = '\uFAFF';
        	    } else 
    		    // ..  Alphabetic Presentation Forms
        	    if (c>='\uFB00' && c<='\uFB4F') 
        	    {
        	    	/* hAnsi (or hAnsiTheme if defined), with the following exceptions:
        	    	 * 
								If the hint is eastAsia then eastAsia (or eastAsiaTheme if defined) is used for characters in the range FB00 – FB1C.
								For the range FB1D – FB4F, ascii (or asciiTheme if defined) is used.
						*/
        	    	if (hint == STHint.EAST_ASIA) {
    	    			if ( c>='\uFB00' && c<='\uFB1C') {
    	    				span.setAttribute("style", getCssProperty(eastAsia));	
    	    				spanReusable = false;
    	    			} else {
    	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
    	    			}
        	    			
        	    	} else if ( c>='\uFB1D' && c<='\uFB4F') {
        	    				
	    				span.setAttribute("style", getCssProperty(ascii));			
	    				spanReusable = false;
	    				
        	    	} else {
	    				span.setAttribute("style", getCssProperty(hAnsi));			        	    		
        	    	}
        	    	sb.append(c);
        	    	
        	    	currentRangeLower = '\uFB00';
        	    	currentRangeUpper = '\uFB4F';
	    	    } else if (c>='\uFB50' && c<='\uFDFF') {
	        	    span.setAttribute("style", getCssProperty(ascii)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\uFB50';
	    	    	currentRangeUpper = '\uFDFF';	
	    	    } else if (c>='\uFE30' && c<='\uFE6F') {
	        	    span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\uFE30';
	    	    	currentRangeUpper = '\uFE6F';	
	    	    } else if (c>='\uFE70' && c<='\uFEFE') {
	        	    span.setAttribute("style", getCssProperty(ascii)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\uFE70';
	    	    	currentRangeUpper = '\uFEFE';	
	    	    } else if (c>='\uFF00' && c<='\uFFEF') {
	        	    span.setAttribute("style", getCssProperty(eastAsia)); // TODO  or implementationDefault
	    	    	sb.append(c);
	    	    	
	    	    	currentRangeLower = '\uFF00';
	    	    	currentRangeUpper = '\uFFEF';	
	    	    }
    	    }
    	} 
    	
    	// Handle final span
    	if (sb.length()>0) {
	    	df.appendChild(span);   
	    	span.setTextContent(sb.toString());  
    	}
    	return df;
    }
	
    		
    	

    private DocumentFragment result(Document document) {
    	
		DocumentFragment docfrag = document.createDocumentFragment();
		docfrag.appendChild(document.getDocumentElement());
		return docfrag;
    }
    
    private Document getDocument() {

    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();        
		try {
			return factory.newDocumentBuilder().newDocument();
					
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage(), e);
		}			
		return null;
    	
    }
	
	public String getCssProperty(String fontName) {
		
//		Throwable t = new Throwable();
//		t.printStackTrace();
		
		String font = getPhysicalFont(fontName);
		
		if (font!=null) {					
			return Property.composeCss(CSS_NAME, font );
		} else {
			log.warn("No mapping from " + font);
			return Property.CSS_NULL;
		}
		
	}

	private String getPhysicalFont(String fontName) {
		
		log.debug("looking for: " + fontName);

		PhysicalFont pf = wordMLPackage.getFontMapper().getFontMappings().get(fontName);
		if (pf!=null) {
			log.debug("Font '" + fontName + "' maps to " + pf.getName() );
			return pf.getName();
		} else {
			log.warn("Font '" + fontName + "' is not mapped to a physical font. " );			
			return null;
		}		
	}	
	

}
