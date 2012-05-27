package se.microcode.confluence.plugin.xhtml;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.WikiStyleRenderer;
import com.opensymphony.webwork.ServletActionContext;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import se.microcode.base.ArgumentParser;
import se.microcode.base.ArgumentResolver;
import se.microcode.cogwork.Courses;
import se.microcode.cogwork.Event;
import se.microcode.confluence.plugin.base.EventSorter;
import se.microcode.confluence.plugin.base.EventsHelper;
import se.microcode.confluence.plugin.base.EventsMacroArguments;
import se.microcode.confluence.plugin.base.SortOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EventsMacro implements Macro
{
    public BodyType getBodyType()
    {
        return BodyType.PLAIN_TEXT;
    }

    public OutputType getOutputType()
    {
        return OutputType.BLOCK;
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

    public String execute(Map<String,String> params, String body, ConversionContext conversionContext) throws MacroExecutionException
    {
        EventsMacroArguments args = (EventsMacroArguments)ArgumentParser.parse(new EventsMacroArguments(), params, new ArgumentResolver() {
            @Override
            public String get(String s) {
                return ServletActionContext.getRequest().getParameter(s);
            }
        });

        if (args.url == null)
        {
            throw new MacroExecutionException("No URL source specified");
        }

        Courses courses = EventsHelper.fetchCourses(args.url, httpRetrievalService, xstream);
        if (courses == null || courses.events == null)
        {
            //throw new MacroExecutionException("Could not download events");
            return "";
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
