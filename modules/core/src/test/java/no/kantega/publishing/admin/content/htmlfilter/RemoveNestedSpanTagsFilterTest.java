/*
 * Copyright 2011 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.kantega.publishing.admin.content.htmlfilter;

import no.kantega.commons.exception.SystemException;
import no.kantega.commons.xmlfilter.FilterPipeline;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class RemoveNestedSpanTagsFilterTest {

    private FilterPipeline pipeline;

    @Before
    public void setup(){
        pipeline = new FilterPipeline();
        pipeline.addFilter(new RemoveNestedSpanTagsFilter());
    }

    @Test
    public void shouldRemoveSimpleNestedTag() throws SystemException {
        String htmlBefore = "<span><span>test</span></span>";
        String expectedHtmlAfter = "<span>test</span>";

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }

    @Test
    public void shouldNotRemoveNestedTagsWithDifferentClass() throws SystemException {
        String htmlBefore = "<span class=\"class1\"><span class=\"class2\">test</span></span>";
        String expectedHtmlAfter = htmlBefore;

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }

    @Test
    public void shouldRemoveNestedTagWithoutRemovingInnerTags() throws SystemException {
        String htmlBefore = "<span><span><p><span>test</span></p></span></span>";
        String expectedHtmlAfter = "<span><p><span>test</span></p></span>";

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }

    @Test
    public void shouldRemoveNestedTagWithSameStyle() throws SystemException {
        String htmlBefore = "<p><strong><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"><span style=\"text-decoration: underline;\"> </span></span></span></span></span></span></span></span></span></span></span></span></span></span></strong></p>";
        String expectedHtmlAfter = "<p><strong><span style=\"text-decoration: underline;\"> </span></strong></p>";

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }

    @Test
    public void shouldRemoveNestedTagWithMultipelSimpleSpans() throws SystemException {
        String htmlBefore = "<p><span style=\"font-size: 1em;\"><span><span>ff</span></span></span></p>";
        String expectedHtmlAfter = "<p><span style=\"font-size: 1em;\"><span>ff</span></span></p>";

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }

    @Test
    public void shouldRemoveNestedTagLeavingOneTag() throws SystemException {
        String htmlBefore = "<p><span><span>gg<span>ff</span></span></span></p>";
        String expectedHtmlAfter = "<p><span>ggff</span></p>";

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }


    /**
     * The other tests used to use FilterPipeline pipeline = SharedPipeline.getFilterPipeline();
     * Thus each test added a new RemoveNestedSpanTagsFilter to the pipeline.
     * This made shouldRemoveNestedTagWithoutRemovingInnerTags fail randomly.
     * This test confirms that the result is «<p><span>test</span></p>» when RemoveNestedSpanTagsFilter is run three times.
     */
    @Test
    public void confirmThatMultipleRemoveNestedSpanTagsFilterRemovesMuch(){
        FilterPipeline pipeline = new FilterPipeline();
        pipeline.addFilter(new RemoveNestedSpanTagsFilter());

        String htmlBefore = "<p><span style=\"font-size: 1em;\"><span><span>ff</span></span></span></p>";
        String expectedHtmlAfter = "<p><span style=\"font-size: 1em;\"><span>ff</span></span></p>";

        StringWriter sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());

        pipeline.addFilter(new RemoveNestedSpanTagsFilter());

        htmlBefore = "<p><span><span>gg<span>ff</span></span></span></p>";
        expectedHtmlAfter = "<p><span>ggff</span></p>";

        sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());

        pipeline.addFilter(new RemoveNestedSpanTagsFilter());

        htmlBefore = "<span><span><p><span>test</span></p></span></span>";
        expectedHtmlAfter = "<p><span>test</span></p>";

        sw = new StringWriter();
        pipeline.filter(new StringReader(htmlBefore), sw);

        assertEquals(expectedHtmlAfter, sw.toString());
    }

}
