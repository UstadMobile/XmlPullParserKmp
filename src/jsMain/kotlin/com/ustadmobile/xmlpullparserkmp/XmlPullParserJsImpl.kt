package com.ustadmobile.xmlpullparserkmp

import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.END_DOCUMENT
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.END_TAG
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.START_DOCUMENT
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.START_TAG
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants.TEXT
import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.parsing.DOMParser

class XmlPullParserJsImpl: XmlPullParser {

    private lateinit var treeWalker: TreeWalker

    private val eventsStack = mutableListOf<ParserEvent>()

    private val parentNodesStack = mutableListOf<Node>()

    private var nextNode: Node? = null

    private var currentEvent: ParserEvent? = null

    private var lastParentNode: Node? = null

    private var processNsp:Boolean = false

    private var relaxed: Boolean = true

    override fun setInput(content: String) {
        treeWalker = document.createTreeWalker(DOMParser().parseFromString(content,
            "text/${if(content.startsWith("<?xml")) "xml" else "html"}"),
            NodeFilter.SHOW_ALL) { NodeFilter.FILTER_ACCEPT }
        logParserEvents()
    }

    private fun isProp(ns1: String, prop: Boolean, ns2: String): Boolean {
        if (!ns1.startsWith("http://xmlpull.org/v1/doc/"))
            return false
        return if (prop)
            ns1.substring(42) == ns2
        else
            ns1.substring(40) == ns2
    }

    private fun logParserEvents(){
        while ({ nextNode = treeWalker.nextNode(); nextNode }() != null){
            eventsStack.add(ParserEvent().apply {
                eventNode = nextNode
                eventType = START_TAG
                eventNodeDepth = parentNodesStack.size + 1
            })

            if(nextNode?.hasChildNodes() == false){
                if(nextNode?.nodeType == Node.TEXT_NODE){
                    eventsStack.add(ParserEvent().apply {
                        eventNode = nextNode
                        eventType = TEXT
                        eventNodeDepth = parentNodesStack.size + 1
                    })
                }
                eventsStack.add(ParserEvent().apply {
                    eventNode = nextNode
                    eventType = END_TAG
                    eventNodeDepth = parentNodesStack.size + 1
                })
            }

            if(parentNodesStack.isEmpty() || nextNode?.hasChildNodes() == true){
                nextNode?.let { parentNodesStack.add(it) }
            }

            val currentParentNode = parentNodesStack.last()
            if(currentParentNode.lastChild == nextNode){
                eventsStack.add(ParserEvent().apply {
                    eventNode = currentParentNode
                    eventType = END_TAG
                    eventNodeDepth = parentNodesStack.indexOf(currentParentNode) + 1
                })
                parentNodesStack.removeLastOrNull()

                if(currentParentNode.parentNode?.nextSibling != null
                    && currentParentNode.parentNode?.lastChild == currentParentNode){
                    eventsStack.add(ParserEvent().apply {
                        eventNode = currentParentNode.parentNode
                        eventType = END_TAG
                        eventNodeDepth = parentNodesStack.indexOf(currentParentNode.parentNode) + 1
                    })
                    parentNodesStack.removeLastOrNull()
                }
            }
        }

        while ({ lastParentNode = parentNodesStack.removeLastOrNull(); lastParentNode }() != null){
            eventsStack.add(ParserEvent().apply {
                eventNode = lastParentNode
                eventType = END_TAG
                eventNodeDepth = parentNodesStack.size + 1
            })
        }

        eventsStack.add(0, ParserEvent().apply {
            eventNode = treeWalker.root
            eventType = START_DOCUMENT
        })

        eventsStack.add(ParserEvent().apply {
            eventNode = treeWalker.root
            eventType = END_DOCUMENT
        })
        eventsStack.reverse()
    }

    private fun getAttributes(): List<Attr>{
        return getCurrentEventElement()?.attributes?.asList()?: listOf()
    }

    private fun getCurrentEventElement(event: ParserEvent? = null): Element?{
        val mElement = event ?: currentEvent
        mElement?.eventNode?.appendChild(document.createTextNode("text"))
        return mElement?.eventNode?.lastChild?.parentElement
    }

    private fun isStartOrEndTag():Boolean{
        return  (currentEvent?.eventType == START_TAG
                || currentEvent?.eventType == END_TAG)
    }

    private fun isNsEnabledAndStartOrEndTag(): Boolean {
        return processNsp && isStartOrEndTag()
    }


    override fun getDepth(): Int {
        return currentEvent?.eventNodeDepth ?: -1
    }

    override fun isWhitespace(): Boolean {
        if(currentEvent?.eventType == TEXT){
            return getText().isNullOrEmpty()
        }
        return false
    }

    override fun getText(): String? {
        return if(currentEvent?.eventNode?.nodeType == Node.TEXT_NODE){
            currentEvent?.eventNode?.textContent
        }else null
    }

    override fun getNamespace(): String? {
        return  if(isNsEnabledAndStartOrEndTag())
                getCurrentEventElement()?.namespaceURI
        else if (!processNsp) ""  else null
    }

    override fun getNamespace(prefix: String?): String? {
        return  getCurrentEventElement()?.lookupNamespaceURI(prefix)
    }

    override fun getName(): String? {
        return currentEvent?.eventNode?.nodeName
    }

    override fun getPrefix(): String? {
        return if(isNsEnabledAndStartOrEndTag())
            currentEvent?.eventNode?.lookupPrefix(null) else null
    }

    override fun getAttributeCount(): Int {
        val currentNode = currentEvent
        return if(currentNode != null && currentNode.eventType == START_TAG &&
            currentNode.eventNode?.nodeType == Node.ELEMENT_NODE){
            getAttributes().size
        } else  -1
    }

    override fun getEventType(): Int {
       return currentEvent?.eventType ?: -1
    }

    override fun setFeature(name: String, state: Boolean) {
        when {
            XmlPullParserConstants.FEATURE_PROCESS_NAMESPACES == name -> processNsp = state
            isProp(name, false, "relaxed") -> relaxed = state
            else -> throw Exception("unsupported feature: $name")
        }
    }

    override fun getFeature(name: String): Boolean {
        return when {
            XmlPullParserConstants.FEATURE_PROCESS_NAMESPACES == name -> processNsp
            isProp(name, false, "relaxed") -> relaxed
            else -> false
        }
    }


    override fun getNamespaceCount(depth: Int): Int {
        val namespaceSet = mutableSetOf<String>()
        eventsStack.filter { it.eventNodeDepth == depth}.forEach {
            val namespace = getCurrentEventElement(it)?.namespaceURI
            if(namespace != null){
                namespaceSet.add(namespace)
            }
        }
        return if(processNsp) namespaceSet.size else 0
    }

    override fun getNamespacePrefix(pos: Int): String? {
        val attributes = getAttributes()
        return if(attributes.isNotEmpty()) attributes[pos].prefix else null
    }

    override fun getNamespaceUri(pos: Int): String? {
        val attributes = getAttributes()
        return if(attributes.isNotEmpty()) attributes[pos].namespaceURI else null
    }

    @Suppress("RedundantNullableReturnType")
    override fun getAttributeNamespace(index: Int): String? {
        val ns = getNamespaceUri(index)
        return if(!processNsp || ns == null) "" else if(currentEvent?.eventType != START_TAG)
            throw IndexOutOfBoundsException() else ns
    }

    override fun getAttributeName(index: Int): String? {
        val attributes = getAttributes()
        return if(currentEvent?.eventType != START_TAG) throw IndexOutOfBoundsException()
        else if(attributes.isNotEmpty())
            if(processNsp) attributes[index].name else attributes[index].localName
        else null
    }

    override fun getAttributePrefix(index: Int): String? {
        val attributes = getAttributes()
        return if(currentEvent?.eventType != START_TAG) throw IndexOutOfBoundsException()
        else if(attributes.isNotEmpty() && processNsp) attributes[index].prefix else null
    }

    override fun getAttributeValue(index: Int): String? {
        val attributes = getAttributes()
        return if(currentEvent?.eventType != START_TAG) throw IndexOutOfBoundsException()
        else if(attributes.isNotEmpty()) attributes[index].value else null
    }

    override fun getAttributeValue(namespace: String?, name: String): String? {
        return when {
            currentEvent?.eventType != START_TAG -> throw IndexOutOfBoundsException()
            processNsp -> getAttributes().first {(it.name == name || it.name.contains(name))
                    && (it.namespaceURI == null || it.namespaceURI == namespace)}.value
            else -> null
        }
    }

    override fun next(): Int {
        currentEvent = eventsStack.removeLastOrNull()
        return currentEvent?.eventType ?: -1
    }
}