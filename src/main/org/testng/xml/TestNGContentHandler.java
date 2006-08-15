package org.testng.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.TestNG;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Suite definition parser utility.
 * 
 * @author Cedric Beust
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class TestNGContentHandler extends DefaultHandler {
  private XmlSuite m_currentSuite = null;
  private XmlTest m_currentTest = null;
  private List<String> m_currentDefines = null;
  private List<String> m_currentRuns = null;
  private List<String> m_currentGroups = null;
  private List<XmlClass> m_currentClasses = null;
  private List<XmlPackage> m_currentPackages = null;
  private XmlPackage m_currentPackage = null;
  private List<XmlSuite> m_suites = new ArrayList<XmlSuite>();
  private List<String> m_currentIncludedGroups = null;
  private List<String> m_currentExcludedGroups = null;
  private Map<String, String> m_currentTestParameters = null;
  private Map<String, String> m_currentSuiteParameters = null;
  private ArrayList<String> m_currentMetaGroup = null;
  private String m_currentMetaGroupName;
  private boolean m_inTest = false;
  private XmlClass m_currentClass = null;
  private ArrayList<String> m_currentIncludedMethods = null;
  private ArrayList<String> m_currentExcludedMethods = null;
  private ArrayList<XmlMethodSelector> m_currentSelectors = null;
  private XmlMethodSelector m_currentSelector = null;
  private String m_currentLanguage = null;
  private String m_currentExpression = null;
  
  private String m_fileName;

  public TestNGContentHandler(String fileName) {
    m_fileName = fileName;
  }
  
  static private void ppp(String s) {
    System.out.println("[TestNGContentHandler] " + s);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public InputSource resolveEntity(String systemId, String publicId)
      throws IOException, SAXException {
    InputSource result = null;
    if (Parser.DEPRECATED_TESTNG_DTD_URL.equals(publicId)
        || Parser.TESTNG_DTD_URL.equals(publicId)) {
      InputStream is = getClass().getClassLoader().getResourceAsStream(
          Parser.TESTNG_DTD);
      if (null == is) {
        is = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(Parser.TESTNG_DTD);
        if (null == is) {
          System.out.println("WARNING: couldn't find in classpath " + publicId
              + "\n" + "Fetching it from the Web site.");
          result = super.resolveEntity(systemId, publicId);
        }
        else {
          result = new InputSource(is);
        }
      }
      else {
        result = new InputSource(is);
      }
    }
    else {
      result = super.resolveEntity(systemId, publicId);
    }

    return result;
  }

  /**
   * Parse <suite>
   */
  private void xmlSuite(boolean start, Attributes attributes) {
    if (start) {
      String name = attributes.getValue("name");
      m_currentSuite = new XmlSuite();
      m_currentSuite.setFileName(m_fileName);
      m_currentSuite.setName(name);
      m_currentSuiteParameters = new HashMap<String, String>();
      
      String verbose = attributes.getValue("verbose");
      if (null != verbose) {
        m_currentSuite.setVerbose(new Integer(verbose));
      }
      String parallel = attributes.getValue("parallel");
      if (null != parallel) {
        m_currentSuite.setParallel(new Boolean(parallel).booleanValue());
      }
      String threadCount = attributes.getValue("thread-count");
      if (null != threadCount) {
        m_currentSuite.setThreadCount(Integer.parseInt(threadCount));
      }
      String annotations = attributes.getValue("annotations");
      if (null != annotations) {
        m_currentSuite.setAnnotations(annotations);
      }
      if (TestNG.isJdk14()) {
        m_currentSuite.setAnnotations(XmlSuite.JAVADOC);
      }
    }
    else {
      m_currentSuite.setParameters(m_currentSuiteParameters);
      m_suites.add(m_currentSuite);
      m_currentSuiteParameters = null;
    }
  }

  /**
   * Parse <define>
   */
  private void xmlDefine(boolean start, Attributes attributes) {
    if (start) {
      String name = attributes.getValue("name");
      m_currentDefines = new ArrayList<String>();
      m_currentMetaGroup = new ArrayList<String>();
      m_currentMetaGroupName = name;
    }
    else {
      m_currentTest.addMetaGroup(m_currentMetaGroupName, m_currentMetaGroup);
      m_currentDefines = null;
    }
  }

  /**
   * Parse <script>
   */
  private void xmlScript(boolean start, Attributes attributes) {
    if (start) {
//      ppp("OPEN SCRIPT");
      m_currentLanguage = attributes.getValue("language");
      m_currentExpression = new String();
    }
    else {
//      ppp("CLOSE SCRIPT:@@" + m_currentExpression + "@@");
      m_currentSelector.setExpression(m_currentExpression);
      m_currentSelector.setLanguage(m_currentLanguage);
      if (m_inTest) {
        m_currentTest.setBeanShellExpression(m_currentExpression);
      }
      else {
        m_currentSuite.setBeanShellExpression(m_currentExpression);
      }
      m_currentLanguage = null;
      m_currentExpression = null;
    }
  }

  /**
   * Parse <test>
   */
  private void xmlTest(boolean start, Attributes attributes) {
    if (start) {
      String name = attributes.getValue("name");
      m_currentTest = new XmlTest(m_currentSuite);
      m_currentTestParameters = new HashMap<String, String>();
      m_currentTest.setName(name);
      String verbose = attributes.getValue("verbose");
      if (null != verbose) {
        m_currentTest.setVerbose(Integer.parseInt(verbose));
      }
      String jUnit = attributes.getValue("junit");
      if (null != jUnit) {
        m_currentTest.setJUnit(new Boolean(jUnit).booleanValue());
      }
      String parallel = attributes.getValue("parallel");
      if (null != parallel) {
        m_currentTest.setParallel(new Boolean(parallel).booleanValue());
      }
      String annotations = attributes.getValue("annotations");
      if (null != annotations) {
        m_currentTest.setAnnotations(annotations);
      }
      m_inTest = true;
    }
    else {
      if (null != m_currentTestParameters && m_currentTestParameters.size() > 0) {
        m_currentTest.setParameters(m_currentTestParameters);
      }
      if (null != m_currentClasses) {
        m_currentTest.setXmlClasses(m_currentClasses);
      }
      m_currentClasses = null;
      m_currentTest = null;
      m_currentTestParameters = null;
      m_inTest = false;
    }
  }

  /**
   * Parse <classes>
   */
  public void xmlClasses(boolean start, Attributes attributes) {
    if (start) {
      m_currentClasses = new ArrayList<XmlClass>();
    }
    else {
      m_currentTest.setClassNames(m_currentClasses);
      m_currentClasses = null;
    }
  }

  /**
   * Parse <packages>
   */
  public void xmlPackages(boolean start, Attributes attributes) {
    if (start) {
      m_currentPackages = new ArrayList<XmlPackage>();
    }
    else {
      if (null != m_currentPackages) {
        if(m_inTest) {
          m_currentTest.setXmlPackages(m_currentPackages);
        }
        else {
          m_currentSuite.setXmlPackages(m_currentPackages);
        }
      }
      
      m_currentPackages = null;
      m_currentPackage = null;
    }
  }

  /**
   * Parse <method-selectors>
   */
  public void xmlMethodSelectors(boolean start, Attributes attributes) {
    if (start) {
      m_currentSelectors = new ArrayList<XmlMethodSelector>();
    }
    else {
      if (m_inTest) {
        m_currentTest.setMethodSelectors(m_currentSelectors);
      }
      else {
        m_currentSuite.setMethodSelectors(m_currentSelectors);
      }
      
      m_currentSelectors = null;
    }
  }

  /**
   * Parse <selector-class>
   */
  public void xmlSelectorClass(boolean start, Attributes attributes) {
    if (start) {
      m_currentSelector.setName(attributes.getValue("class-name"));
      m_currentSelector.setPriority(new Integer(attributes.getValue("priority")));
    }
    else {
      // do nothing
    }
  }
  
  /**
   * Parse <method-selector>
   */
  public void xmlMethodSelector(boolean start, Attributes attributes) {
    if (start) {
      m_currentSelector = new XmlMethodSelector();
    }
    else {
      m_currentSelectors.add(m_currentSelector);
      m_currentSelector = null;
    }
  }

  private void xmlMethod(boolean start, Attributes attributes) {
    if (start) {
      m_currentIncludedMethods = new ArrayList<String>();
      m_currentExcludedMethods = new ArrayList<String>();
    }
    else {
      m_currentClass.setIncludedMethods(m_currentIncludedMethods);
      m_currentClass.setExcludedMethods(m_currentExcludedMethods);
      m_currentIncludedMethods = null;
      m_currentExcludedMethods = null;
    }
  }

  /**
   * Parse <run>
   */
  public void xmlRun(boolean start, Attributes attributes) {
    if (start) {
      m_currentRuns = new ArrayList<String>();
    }
    else {
      m_currentTest.setIncludedGroups(m_currentIncludedGroups);
      m_currentTest.setExcludedGroups(m_currentExcludedGroups);
    }
  }

  /**
   * NOTE: I only invoke xml*methods (e.g. xmlSuite()) if I am acting on both
   * the start and the end of the tag. This way I can keep the treatment of
   * this tag in one place. If I am only doing something when the tag opens,
   * the code is inlined below in the startElement() method.
   */
  @Override
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {
    String name = attributes.getValue("name");

    // ppp("START ELEMENT uri:" + uri + " sName:" + localName + " qName:" + qName +
    // " " + attributes);
    if ("suite".equals(qName)) {
      xmlSuite(true, attributes);
    }
    else if ("test".equals(qName)) {
      xmlTest(true, attributes);
    }
    else if ("script".equals(qName)) {
      xmlScript(true, attributes);
    }
    else if ("method-selector".equals(qName)) {
      xmlMethodSelector(true, attributes);
    }
    else if ("method-selectors".equals(qName)) {
      xmlMethodSelectors(true, attributes);
    }
    else if ("selector-class".equals(qName)) {
      xmlSelectorClass(true, attributes);
    }
    else if ("classes".equals(qName)) {
      xmlClasses(true, attributes);
    }
    else if ("packages".equals(qName)) {
      xmlPackages(true, attributes);
    }
    else if ("class".equals(qName)) {
      // If m_currentClasses is null, the XML is invalid and SAX
      // will complain, but in the meantime, dodge the NPE so SAX
      // can finish parsing the file.
      if (null != m_currentClasses) {
        m_currentClass = new XmlClass(name);
        m_currentClasses.add(m_currentClass);
      }
    }
    else if ("package".equals(qName)) {
      if (null != m_currentPackages) {
        m_currentPackage = new XmlPackage();
        m_currentPackage.setName(name);
        m_currentPackages.add(m_currentPackage);
      }
    }
    else if ("define".equals(qName)) {
      xmlDefine(true, attributes);
    }
    else if ("run".equals(qName)) {
      xmlRun(true, attributes);
    }
    else if ("groups".equals(qName)) {
      m_currentIncludedGroups = new ArrayList<String>();
      m_currentExcludedGroups = new ArrayList<String>();
    }
    else if ("methods".equals(qName)) {
      xmlMethod(true, attributes);
    }
    else if ("include".equals(qName)) {
      if (null != m_currentIncludedMethods) {
        m_currentIncludedMethods.add(name);
      }
      else if (null != m_currentDefines) {
        m_currentMetaGroup.add(name);
      }
      else if (null != m_currentRuns) {
        m_currentIncludedGroups.add(name);
      }
      else if (null != m_currentPackage) {
        m_currentPackage.getInclude().add(name);
      }
    }
    else if ("exclude".equals(qName)) {
      if (null != m_currentExcludedMethods) {
        m_currentExcludedMethods.add(name);
      }
      else if (null != m_currentRuns) {
        m_currentExcludedGroups.add(name);
      }
      else if (null != m_currentPackage) {
        m_currentPackage.getExclude().add(name);
      }
    }
    else if ("parameter".equals(qName)) {
      String value = attributes.getValue("value");
      if (m_inTest) {
        m_currentTestParameters.put(name, value);
      }
      else {
        m_currentSuiteParameters.put(name, value);
      }
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
    if ("suite".equals(qName)) {
      xmlSuite(false, null);
    }
    else if ("test".equals(qName)) {
      xmlTest(false, null);
    }
    else if ("define".equals(qName)) {
      xmlDefine(false, null);
    }
    else if ("run".equals(qName)) {
      xmlRun(false, null);
    }
    else if ("methods".equals(qName)) {
      xmlMethod(false, null);
    }
    else if ("classes".equals(qName)) {
      xmlClasses(false, null);
    }
    else if ("classes".equals(qName)) {
      xmlPackages(false, null);
    }
    else if ("method-selector".equals(qName)) {
      xmlMethodSelector(false, null);
    }
    else if ("method-selectors".equals(qName)) {
      xmlMethodSelectors(false, null);
    }
    else if ("selector-class".equals(qName)) {
      xmlSelectorClass(false, null);
    }
    else if ("script".equals(qName)) {
      xmlScript(false, null);
    }
    else if ("packages".equals(qName)) {
      xmlPackages(false, null);
    }
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    throw e;
  }

  private boolean areWhiteSpaces(char[] ch, int start, int length) {
    for (int i = start; i < start + length; i++) {
      char c = ch[i];
      if (c != '\n' && c != '\t' && c != ' ') return false;
    }    
    
    return true;
  }
  
  @Override
  public void characters(char ch[], int start, int length) {
    if (null != m_currentLanguage && ! areWhiteSpaces(ch, start, length)) {
      m_currentExpression += new String(ch, start, length);
    }
  }

  public XmlSuite getSuite() {
    return m_currentSuite;
  }
}