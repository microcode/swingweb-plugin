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
import org.apache.velocity.VelocityContext;

import se.microcode.cogwork.*;
import se.microcode.base.ArgumentParser;
import se.microcode.base.ArgumentResolver;

import com.opensymphony.webwork.ServletActionContext;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.*;

public class EventsMacro extends BaseMacro
{
    XStream xstream;

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

        CacheFactory cacheFactory = (CacheFactory) ContainerManager.getComponent("cacheManager");
        Cache cache = cacheFactory.getCache("se.nackswinget.confluence.plugin.courses");

        HttpServletRequest request = ServletActionContext.getRequest();
        boolean flushCache = false;

        if (request != null)
        {
            try
            {
                flushCache = Boolean.parseBoolean(request.getParameter("flush-cache"));
            }
            catch (NumberFormatException e)
            {
            }
        }

        if (flushCache)
        {
            cache.removeAll();
        }

        StringBuilder builder = new StringBuilder();
        Courses courses;

        if (args.url == null)
        {
            throw new MacroException("No URL source specified");
        }

        try
        {
            courses = (Courses)cache.get(args.url);
        }
        catch (ClassCastException e)
        {
            courses = null;
        }

        if (courses == null)
        {
            HttpResponse response;
            try
            {
                response = httpRetrievalService.get(args.url);
            }
            catch (IOException e)
            {
                return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att h&auml;mta den fr&aring;n den externa k&auml;llan{warning}");
            }

            if (response.getStatusCode() != 200)
            {
                return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att h&auml;mta den fr&aring;n den externa k&auml;llan (felkod " + response.getStatusCode() + "){warning}");
            }

            try
            {
                courses = (Courses)xstream.fromXML(response.getResponse());
            }
            catch (IOException e)
            {
                return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att h&auml;mta datan fr&aring;n den externa k&auml;llan{warning}");
            }
            catch (ConversionException e)
            {
                return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att l&auml;sa fr&aring;n den externa k&auml;llan{warning}");
            }

            cache.put(args.url, courses);
        }

        List<Event> events = new ArrayList<Event>(courses.events.entries);
        {
            if (args.limit >= 0)
            {
                long limitOffset = Long.valueOf(args.limit) * 1000;
                int stateMask = args.showStates != null ? buildStateMask(args.showStates) : 0;
                Date limit = new Date(Calendar.getInstance().getTimeInMillis() + limitOffset);

                List<Event> newEvents = new ArrayList<Event>();

                for (Event event : events)
                {
                    RegistrationPeriods periods = event.registrationPeriods;
                    EventState state = event.getEventState();

                    if (periods == null)
                    {
                        continue;
                    }

                    boolean filtered = false;
                    if ((stateMask & (1 << state.ordinal())) != 0)
                    {
                        switch (state)
                        {
                            case HIDDEN:
                            case OPEN:
                            {
                                if (periods.startDirectReg != null)
                                {
                                    if (periods.startDirectReg.compareTo(limit) > 0)
                                    {
                                        filtered = true;
                                    }
                                }
                                else if (periods.startLateReg != null)
                                {
                                    if (periods.startLateReg.compareTo(limit) > 0)
                                    {
                                        filtered = true;
                                    }
                                }
                            }
                            break;

                            case DIRECT:
                            {
                                if ((periods.startLateReg != null) && (periods.startLateReg.compareTo(limit) > 0))
                                {
                                    filtered = true;
                                }
                            }
                            break;
                        }
                    }

                    if (!filtered)
                    {
                        newEvents.add(event);
                    }
                }

                events = newEvents;
            }

            if (args.hideCategories != null)
            {
                List<Event> newEvents = new ArrayList<Event>();

                for (Event event : events)
                {
                    boolean found = false;
                    for(int pin : args.hideCategories)
                    {
                        for (Category haystack : event.categories)
                        {
                            if (haystack.categoryIndex == pin)
                            {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found)
                    {
                        newEvents.add(event);
                    }
                }

                events = newEvents;
            }

            if (args.showCategories != null)
            {
                List<Event> newEvents = new ArrayList<Event>();

                for (Event event : events)
                {
                    boolean found = false;
                    for(int pin : args.showCategories)
                    {
                        for (Category haystack : event.categories)
                        {
                            if (haystack.categoryIndex == pin)
                            {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (found)
                    {
                        newEvents.add(event);
                    }
                }

                events = newEvents;
            }

            if (args.hideStates != null)
            {
                int stateMask = buildStateMask(args.hideStates);
                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    if ((stateMask & (1 << event.getEventState().ordinal())) != 0)
                    {
                        continue;
                    }

                    newEvents.add(event);
                }

                events = newEvents;
            }

            if (args.showStates != null)
            {
                int stateMask = buildStateMask(args.showStates);
                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    int state = event.getEventState().ordinal();

                    if ((stateMask & (1 << state)) == 0)
                    {
                        continue;
                    }

                    newEvents.add(event);
                }

                events = newEvents;
            }

            if (args.minLevel >= 0)
            {
                int minLevel = args.minLevel;
                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    if (event.requirements == null)
                    {
                        continue;
                    }

                    if (event.requirements.get(0).minValue < minLevel)
                    {
                        continue;
                    }

                    newEvents.add(event);
                }

                events = newEvents;
            }

            if (args.maxLevel >= 0)
            {
                int maxLevel = Integer.valueOf(args.maxLevel);
                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    if (event.requirements == null)
                    {
                        continue;
                    }

                    if (event.requirements.get(0).minValue > maxLevel)
                    {
                        continue;
                    }

                    newEvents.add(event);
                }

                events = newEvents;
            }

            if (args.started != StartedState.DONT_CARE)
            {
                boolean filter = args.started == StartedState.YES;
                Date now = Calendar.getInstance().getTime();

                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    if (event.schedule == null || event.schedule.startDate == null)
                    {
                        continue;
                    }

                    boolean started = event.schedule.startDate.compareTo(now) >= 0;
                    if ((!started && !filter) || (started && filter))
                    {
                        continue;
                    }

                    newEvents.add(event);
                }
                events = newEvents;

            }
        }

        if (args.sort != SortOrder.OFF)
        {
            Sorter sorter = new Sorter(args.sort);
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

    private int buildStateMask(EventState[] states)
    {
        int mask = 0;
        for (EventState state : states)
        {
            mask |= 1 << state.ordinal();
        }

        return mask;
    }

    class Sorter implements Comparator<Event>
    {
        private SortOrder key;

        public Sorter(SortOrder key)
        {
            this.key = key;
        }

        public int compare(Event o1, Event o2)
        {
            Date d1 = getDate(o1, key);
            Date d2 = getDate(o2, key);

            return d1.compareTo(d2);
        }

        private Date getDate(Event o1, SortOrder key)
        {
            if (o1.registrationPeriods == null)
            {
                return Calendar.getInstance().getTime();
            }

            RegistrationPeriods periods = o1.registrationPeriods;

            switch (key)
            {
                case SHOW:
                {
                    return periods.startShowing;
                }

                case DIRECT:
                {
                    if (periods.startDirectReg != null)
                    {
                        return periods.startDirectReg;
                    }
                    else if (periods.stopShowing != null)
                    {
                        return periods.stopShowing;
                    }
                }
                break;

                case LATE:
                {
                    if (periods.startLateReg != null)
                    {
                        return periods.startLateReg;
                    }
                    else if (periods.stopShowing != null)
                    {
                        return periods.stopShowing;
                    }
                }
                break;

                case HIDE:
                {
                    if (periods.stopShowing != null)
                    {
                        return periods.stopShowing;
                    }
                }
                break;
            }

            return Calendar.getInstance().getTime();
        }
    }
}
