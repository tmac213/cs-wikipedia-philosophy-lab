package com.flatironschool.javacs;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.select.Elements;

public class WikiPhilosophy {
	
    static class WPIterable implements Iterable<Node> {
        Node root;

        public WPIterable(Node root) {
            this.root = root;
        }
        
        public Iterator<Node> iterator() {
            return new WPIterator(root);
        }
    }

    static class WPIterator implements Iterator<Node> {
        Deque<Node> nodeStack = new ArrayDeque<>();
        
        WPIterator(Node root) {
            nodeStack.push(root);
        }

        public boolean hasNext() {
            return !nodeStack.isEmpty();
        }

        public Node next() {
            Node ret = nodeStack.pop();
            nodeStack.addAll(ret.childNodes());
            return ret;
        }
    }

	final static WikiFetcher wf = new WikiFetcher();

    final static String PHILOSOPHY_URL = "https://en.wikipedia.org/wiki/Philosophy";
	
	/**
	 * Tests a conjecture about Wikipedia and Philosophy.
	 * 
	 * https://en.wikipedia.org/wiki/Wikipedia:Getting_to_Philosophy
	 * 
	 * 1. Clicking on the first non-parenthesized, non-italicized link
     * 2. Ignoring external links, links to the current page, or red links
     * 3. Stopping when reaching "Philosophy", a page with no links or a page
     *    that does not exist, or when a loop occurs
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String startURL = "https://en.wikipedia.org/wiki/Java_(programming_language)";
        try {
            if (testConjecture(startURL)) {
                System.out.println("Success");
            } else {
                System.out.println("Failure");
            }
        } catch (IOException e) {
            System.err.println("Error while fetching Wikipedia data");
        }
	}

    public static void p(String s) {
        System.out.println(s);
    }

    static boolean testConjecture(String url) throws IOException {
        Set<String> visitedPages = new HashSet<>();
        while (url != null && !visitedPages.contains(url)) {
            visitedPages.add(url);
            System.out.println("Fetching " + url);
            Elements paragraphs = wf.fetchWikipedia(url);
            String nextPage = getNextPage(paragraphs);
            if (nextPage == null) {
                p("null url");
                return false;  // no more valid links to explore
            } else if (nextPage.equals(PHILOSOPHY_URL)) {
                p("found philosophy");
                return true;  // reached the philosophy page
            }
            url = nextPage;
        }
        
        return false;  // revisiting a page
    }

    static String getNextPage(Elements paragraphs) {
        String ret = null;
        Element currentElement;
        while (ret == null && !paragraphs.isEmpty()) {
            currentElement = paragraphs.remove(0);
            ret = getFirstValidLink(currentElement);
            p(String.format("got %s", ret));
        }

        return ret;
    }

    static String getFirstValidLink(Element paragraph) {
        int numParenthesesOpen = 0;

        for (Node node : new WPIterable(paragraph)) {
            if (node instanceof TextNode) {
                p("checking " + ((TextNode)node).text());
                numParenthesesOpen += countParenthesesOpen(((TextNode)node).text());
                p("count is now " + numParenthesesOpen);
            }
            if (node instanceof Element && numParenthesesOpen == 0 && isValidLink((Element) node)) {
                return node.attr("abs:href");
            }
        }

        return null;  // could not find valid link
    }

    static int countParenthesesOpen(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
            }
        }

        return count;
    }

    static boolean isValidLink(Element e) {
        if (!isLink(e)) {
            p(e.toString() + " is not link");
            return false;
        }
        if (isItalic(e)) {
            p(e.toString() + " is italic");
            return false;
        }
        if (isExternalLink(e)) {
            p(e.toString() + " is an external link");
            return false;
        }
        return true;
        //return isLink(e) && !isItalic(e) && !isExternalLink(e);
    }

    static boolean isLink(Element e) {
        return e.tagName().equals("a");
    }

    static boolean isItalic(Element e) {
        while (e != null) {
            if (e.tagName().equals("i")) {
                return true;
            }
            e = e.parent();
        }
        return false;
    }

    static boolean isExternalLink(Element e) {
        return !e.attr("abs:href").contains("wikipedia.org");
    }

}
