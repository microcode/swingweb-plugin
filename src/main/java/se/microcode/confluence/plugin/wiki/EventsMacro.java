package se.microcode.confluence.plugin.wiki;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;

import com.atlassian.spring.container.ContainerManager;
import com.thoughtworks.xstream.XStream;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import se.microcode.cogwork.*;
import se.microcode.base.ArgumentParser;
import se.microcode.base.ArgumentResolver;

import com.opensymphony.webwork.ServletActionContext;
import se.microcode.confluence.plugin.base.*;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.*;

public class EventsMacro extends BaseMacro
{
    public RenderMode getBodyRenderMode()
    {
        return RenderMode.NO_RENDER;
    }

    public boolean hasBody()
    {
        return true;
    }

    private HttpRetrievalService httpRetrievalService;
    private WikiStyleRenderer wikiStyleRenderer;
    private XStream xstream;

    public EventsMacro(HttpRetrievalService httpRetrievalService, WikiStyleRenderer wikiStyleRenderer)
    {
        this.httpRetrievalService = httpRetrievalService;
        this.wikiStyleRenderer = wikiStyleRenderer;

        xstream = new XStream()
        {
            protected MapperWrapper wrapMapper(MapperWrapper next)
            {
                return new MapperWrapper(next)
                {
                    public boolean shouldSerializeMember(Class definedIn, String fieldName)
                    {
                        return definedIn != Object.class ? super.shouldSerializeMember(definedIn, fieldName) : false;
                    }

                };
            }
        };

        xstream.setClassLoader(getClass().getClassLoader());
        xstream.alias("cogwork", Courses.class);
        xstream.processAnnotations(Courses.class);
    }

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        EventsMacroArguments args = (EventsMacroArguments)ArgumentParser.parse(new EventsMacroArguments(), params, new ArgumentResolver() {
            @Override
            public String get(String s) {
                return ServletActionContext.getRequest().getParameter(s);
            }
        });

        if (args.url == null)
        {
            throw new MacroException("No URL source specified");
        }

        Courses courses = EventsHelper.fetchCourses(args.url, httpRetrievalService, xstream);
        if (courses == null || courses.events == null)
        {
            //throw new MacroException("Could not download events");
            return "";
        }

        if (args.registrations != null)
        {
            List<Event> registrations = EventsHelper.fetchRegistrations(args.registrations, httpRetrievalService);
            EventsHelper.mergeWithRegistrations(courses, registrations);
        }

        List<Event> events = new ArrayList<Event>(courses.events.entries);
        StringBuilder builder = new StringBuilder();

        events = EventsHelper.processLimits(events, args.limit, args.showStates);
        events = EventsHelper.processCategories(events, args.hideCategories, args.showCategories);
        events = EventsHelper.processStates(events, args.hideStates, args.showStates);
        events = EventsHelper.processLevels(events, args.minLevel, args.maxLevel);
        events = EventsHelper.processStarted(events, args.started);

        if (args.sort != SortOrder.OFF)
        {
            EventSorter sorter = EventSorter.createSorter(args.sort);
            Collections.sort(events, sorter);
        }

        switch (args.type)
        {
            case VELOCITY:
            {
                Map context = MacroUtils.defaultVelocityContext();
                context.put("events", events);

                builder.append(VelocityUtils.getRenderedContent(body, context));
            }
            break;

            case XHTML:
            {

            }
            break;

            case WIKI:
            {

            }
            break;
        }

        return builder.toString();
    }
}
