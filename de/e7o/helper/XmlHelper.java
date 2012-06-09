package de.e7o.helper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

public abstract class XmlHelper {
	public static DocumentBuilderFactory dbf;
	public static DocumentBuilder db;
	public static Document d;
	
	static {
		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			d = db.newDocument();
		} catch (Exception e) {
		}
	}
	
	// Workaround for bad Android XML support
	// http://groups.google.com/group/android-developers/browse_thread/thread/67181fee80e5b03b
	public static String getTextContent(Node n) {
		StringBuffer buffer = new StringBuffer();
		NodeList childList = n.getChildNodes();
		Node child;
		short nodetype;
		for (int i = 0; i < childList.getLength(); i++) {
			child = childList.item(i);
			nodetype = child.getNodeType();
			if (nodetype != Node.TEXT_NODE && nodetype != Node.CDATA_SECTION_NODE) continue;
			buffer.append(child.getNodeValue());
		}
		return buffer.toString();
	}
	
	public static void setTextContent(Node n, String s) {
		n.appendChild(d.createTextNode(s));
	}
	
	// Workaround for missing tranforms in Android
	// http://markmail.org/message/2ccxjsgqpe2tfrhh
	public static String documentToString(Node n) {
		StringBuilder buffer = new StringBuilder();
		if (n == null) return "";
		if (n instanceof Document) {
			buffer.append(documentToString((n).getFirstChild()));
		} else if (n instanceof Element) {
			Element element = (Element)n;
			buffer.append("<");
			buffer.append(element.getNodeName());
			if (element.hasAttributes()) {
				NamedNodeMap map = element.getAttributes();
				for (int i = 0; i < map.getLength(); i++) {
					Node attr = map.item(i);
					buffer.append(" ");
					buffer.append(attr.getNodeName());
					buffer.append("=\"");
					buffer.append(attr.getNodeValue());
					buffer.append("\"");
				}
			}
			buffer.append(">");
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				buffer.append(documentToString(children.item(i)));
			}
			buffer.append("</");
			buffer.append(element.getNodeName());
			buffer.append(">");
		} else if (n != null && n.getNodeValue() != null) {
			buffer.append(n.getNodeValue());
		}
		return buffer.toString();
	}
	/*
	ORIGINAL CODE:
	
	import javax.xml.transform.*;
	import javax.xml.transform.dom.*;
	import javax.xml.transform.stream.*;
	
	try {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(n), new StreamResult(sw));
		return sw.toString();
	} catch (Exception e) {
		System.out.println(e.getMessage());
		return null;
	}
	*/
	
	// Workaround for missing XPath in Android
	public static NodeList findNodes(String tagName, Node n) {
		NodeListImpl nl = new NodeListImpl();
		findNodes(tagName, n, nl);
		return nl;
	}
	
	public static void findNodes(String tagName, Node n, NodeListImpl nl) {
		NodeList childs;
		String nodeName;
		int i;
		nodeName = n.getNodeName();
		i = nodeName.indexOf(':');
		if (i != -1) nodeName = nodeName.substring(i+1);
		if (nodeName.equals(tagName)) nl.addNode(n);
		if (n.hasChildNodes()) {
			childs = n.getChildNodes();
			for (i = 0; i < childs.getLength(); i++) {
				findNodes(tagName, childs.item(i), nl);
			}
		}
	}
	
	/*
	ORIGINAL CODE:
	
	import javax.xml.xpath.*;
	public XPathFactory xpf;
	public XPath xp;
	
	xpf = XPathFactory.newInstance();
	xp = xpf.newXPath();

	public NodeList xpath(String expr, Node n) {
		try {
			return (NodeList)xp.compile(expr).evaluate(n, XPathConstants.NODESET);
		} catch (Exception e) {
			System.out.println("xpath_exc="+e.toString());
			return null;
		}
	}
	*/
}