package ninja.mspp.core.io.xml;

import java.io.File;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;


public abstract class XmlHandler extends DefaultHandler {
	private StringBuilder builder;

	public XmlHandler() {
		this.builder = new StringBuilder();
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		this.builder.append(ch, start, length);
	}

	public String getString() {
		return this.builder.toString();
	}

	public void clear() {
		this.builder.setLength(0);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
		this.clear();
		
		Properties properties = new Properties();	
		for (int i = 0; i < attributes.getLength(); i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);
			properties.put(key, value);
		}
		
		onStartElement(qName, properties);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) {
		onEndElement(qName, this.getString());
		this.clear();
	}
	
	@Override
	public void endDocument() {
		onEndDocument();
	}
	
	@Override
	public void startDocument() {
		onStatDocument();
	}
	
	public void parse(File file) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(file, this);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected abstract void onStatDocument();
	protected abstract void onEndDocument();
	protected abstract void onStartElement(String tag, Properties properties);
	protected abstract void onEndElement(String tag, String content);
}
