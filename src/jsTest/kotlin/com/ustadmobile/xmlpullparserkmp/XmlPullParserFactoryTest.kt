package com.ustadmobile.xmlpullparserkmp

import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.FEATURE_PROCESS_NAMESPACES
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.START_DOCUMENT
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.START_TAG
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.TEXT
import kotlin.test.*

class XmlPullParserFactoryTest {

    lateinit var umXmlPullParser: XmlPullParser

    @BeforeTest
    fun settingUp(){
        umXmlPullParser = XmlPullParserFactory.newInstance().newPullParser()
        umXmlPullParser.setFeature(FEATURE_PROCESS_NAMESPACES, true)
    }

    @Test
    fun givenParsedXmlContent_whenFirstNextIsCalled_ShouldBeDocumentRead(){
        umXmlPullParser.setInput(XML_CONTENT)
        assertEquals(START_DOCUMENT,umXmlPullParser.next())
    }

    @Test
    fun givenParsedXhtmlContent_whenFirstNextIsCalled_ShouldBeDocumentRead(){
        umXmlPullParser.setInput(XHTML_CONTENT)
        assertEquals(START_DOCUMENT,umXmlPullParser.next())
    }

    @Test
    fun givenParsedXmlContentAfterDocumentRead_whenNextIsCalled_ShouldBeTagRead(){
        umXmlPullParser.setInput(XML_CONTENT)
        //read document
        umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.next())
    }

    @Test
    fun givenParsedXhtmlContentAfterDocumentRead_whenNextIsCalled_ShouldBeTagRead(){
        umXmlPullParser.setInput(XHTML_CONTENT)
        //read document
        umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.next())
    }

    @Test
    fun givenMetadataTagStartedEventIsEmitted_whenReadingADocument_ShouldHaveNodeName(){
        umXmlPullParser.setInput(XML_CONTENT)
        for (i in 1..6) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        assertSame("metadata", umXmlPullParser.getName())
    }


    @Test
    fun givenTextTagStartedEventIsEmitted_whenReadingADocument_ShouldBeAbleToGetTextContent(){
        umXmlPullParser.setInput(XML_CONTENT)
        for (i in 1..11) umXmlPullParser.next()
        assertEquals(TEXT,umXmlPullParser.getEventType())
        assertSame("Creative Commons - A Shared Culture", umXmlPullParser.getText())
    }

    @Test
    fun givenMetadataTagStartedEventIsEmitted_whenLookingUpNamespaceByPrefix_ShouldHaveAtLeastOne(){
        umXmlPullParser.setInput(XML_CONTENT)
        for (i in 1..6) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        val namespace = umXmlPullParser.getNamespace("dc")
        assertTrue(namespace?.startsWith("http")?:false && namespace?.contains("dc")?:false)
    }

    @Test
    fun givenNamespaceProcessingIsDisabled_whenLookingUpNamespace_ShouldProvideEmptyNamespace(){
        umXmlPullParser.setFeature(FEATURE_PROCESS_NAMESPACES, false)
        umXmlPullParser.setInput(XML_CONTENT)
        for (i in 1..6) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        val namespace = umXmlPullParser.getNamespace()
        namespace?.isEmpty()?.let { assertTrue(it) }
    }

    @Test
    fun givenMetadataTagStartedEventIsEmitted_whenReadingADocument_ShouldHaveAttributes(){
        umXmlPullParser.setInput(XML_CONTENT)
        for (i in 1..6) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        assertTrue(umXmlPullParser.getAttributeCount() > 0)
    }


    @Test
    fun givenMetadataTagStartedEventIsEmitted_whenReadingADocument_ShouldHaveNameSpace(){
        umXmlPullParser.setInput(XML_CONTENT)
        for (i in 1..6) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        assertSame(NAMESPACE_OPF,umXmlPullParser.getNamespace())
    }

    @Test
    fun givenNavTagStartedEventIsEmitted_whenReadingADocument_ShouldHaveBothTocAndIDAttributeValues(){
        umXmlPullParser.setInput(XHTML_CONTENT)
        for (i in 1..20) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        assertSame("toc",umXmlPullParser.getAttributeValue(NAMESPACE_OPS, "type"))
        assertSame("toc",umXmlPullParser.getAttributeValue(NAMESPACE_OPS, "id"))
    }


    @Test
    fun givenALinkTagStartedEventIsEmitted_whenReadingADocument_ShouldHaveHrefAttributeValue(){
        umXmlPullParser.setInput(XHTML_CONTENT)
        for (i in 1..29) umXmlPullParser.next()
        assertEquals(START_TAG,umXmlPullParser.getEventType())
        assertTrue(umXmlPullParser.getAttributeValue(null,"href")?.indexOf(".xhtml") != -1)
    }

    @Test
    fun givenTinCanFile_whenParsing_ShouldParseADocument(){
        umXmlPullParser.setInput(TINCAN_XML)
        val mutableMap = mutableMapOf<Any?,Any?>()
        var inExtensions = false
        var tagName = ""
        var extKey: String
        var extVal: String
        var evtType = umXmlPullParser.getEventType()
        do{
            var tagValue:Any? = Any()
            if (evtType == START_TAG && umXmlPullParser.getName() != null) {
                tagName = umXmlPullParser.getName()!!
                if (!inExtensions) {
                    if (tagName == "activity") {
                        tagValue = listOf(umXmlPullParser.getAttributeValue(null, "id")!!,
                            umXmlPullParser.getAttributeValue(null, "type")!!)
                    } else if (tagName == "launch" && umXmlPullParser.next() == TEXT) {
                        tagValue = umXmlPullParser.getText()
                    } else if (tagName == "name" && umXmlPullParser.next() == TEXT) {
                        tagValue = umXmlPullParser.getText()
                    } else if (tagName == "description" && umXmlPullParser.next() == TEXT) {
                        tagValue = umXmlPullParser.getText()
                    } else if (umXmlPullParser.getName() == "extensions") {
                        inExtensions = true
                    }
                } else {
                    if (tagName == "extension") {
                        extKey = umXmlPullParser.getAttributeValue(null, "key")!!
                        extVal = if (umXmlPullParser.next() == TEXT) {
                            umXmlPullParser.getText()?: ""
                        } else {
                            ""
                        }
                        mutableMap[extKey] = extVal
                    }
                }
            } else if (evtType == XmlPullParserConstants.END_TAG) {
                if (umXmlPullParser.getName() != null) {
                    if (umXmlPullParser.getName() == "activity") {
                        mutableMap["end"] = true
                    } else if (umXmlPullParser.getName() == "extensions") {
                        inExtensions = false
                    }
                }
            }
            mutableMap[tagName] = tagValue
            evtType = umXmlPullParser.next()
        } while (evtType != XmlPullParserConstants.END_DOCUMENT)

        assertEquals("True/False Question",mutableMap["name"])
    }




    companion object {

        const val NAMESPACE_OPF = "http://www.idpf.org/2007/opf"

        const val NAMESPACE_OPS = "http://www.idpf.org/2007/ops"

        const val XML_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" xml:lang=\"en\" unique-identifier=\"uid\" prefix=\"cc: http://creativecommons.org/ns#\">\n" +
                "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                "    <dc:title id=\"title\">Creative Commons - A Shared Culture</dc:title>\n" +
                "    <dc:creator>Jesse Dylan</dc:creator>\n" +
                "    <dc:identifier id=\"uid\">code.google.com.epub-samples.cc-shared-culture</dc:identifier>\n" +
                "    <dc:language>en-US</dc:language>\n" +
                "    <meta property=\"dcterms:modified\">2012-01-20T12:47:00Z</meta>\n" +
                "    <dc:publisher>Creative Commons</dc:publisher>  \n" +
                "    <dc:contributor>mgylling</dc:contributor>\n" +
                "    <dc:description>Multiple video tests (see Navigation Document (toc) for details)</dc:description>\n" +
                "    <dc:rights>This work is licensed under a Creative Commons Attribution-Noncommercial-Share Alike (CC BY-NC-SA) license.</dc:rights>               \n" +
                "  </metadata>\n" +
                "  <manifest>\n" +
                "    <item id=\"font1\" href=\"fonts/Quicksand_Light.otf\" media-type=\"application/vnd.ms-opentype\"/>\n" +
                "    <item id=\"font2\" href=\"fonts/Quicksand_Bold_Oblique.otf\" media-type=\"application/vnd.ms-opentype\"/>                       \n" +
                "  </manifest>\n" +
                "  <spine>\n" +
                "    <itemref idref=\"cover\" linear=\"no\"/>\n" +
                "    <itemref idref=\"toc\"/>\n" +
                "  </spine>\n" +
                "</package>\n"

        const val XHTML_CONTENT = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" xml:lang=\"en\" lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\"/>\n" +
                "</head>\n" +
                "<body>\n" +
                "<nav epub:type=\"toc\" id=\"toc\">\n" +
                "    <ol>\n" +
                "        <li><a href=\"Page_1.xhtml\">Page 1</a></li>\n" +
                "        <li><a href=\"Page_2.xhtml\">Page 2</a></li>\n" +
                "        <li><a href=\"Page_3.xhtml\">Page 3</a></li>\n" +
                "        <li><a href=\"Page_4.xhtml\">Page 4</a></li>\n" +
                "        <li><a href=\"Page_5.xhtml\">Page 5</a></li>\n" +
                "        <li><a href=\"Page_6.xhtml\">Page 6</a></li>\n" +
                "        <li><a href=\"Acknowledgements.xhtml\">Acknowledgements</a></li>\n" +
                "    </ol>\n" +
                "</nav>\n" +
                "</body>\n" +
                "</html>"

        const val TINCAN_XML = "<tincan xmlns=\"http://projecttincan.com/tincan.xsd\">\n" +
                "<activities>\n" +
                "<activity id=\"http://192.168.31.22:8087/177892100433350656/3196e3e6-6f56-457c-95ef-882b734ac96d\" type=\"http://adlnet.gov/expapi/activities/module\">\n" +
                "<name>True/False Question</name>\n" +
                "<description lang=\"en-US\"/>\n" +
                "<launch lang=\"en-us\">index.html</launch>\n" +
                "</activity>\n" +
                "</activities>\n" +
                "</tincan>"
    }

}