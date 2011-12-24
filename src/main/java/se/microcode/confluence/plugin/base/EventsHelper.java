package se.microcode.confluence.plugin.base;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.spring.container.ContainerManager;
import com.opensymphony.webwork.ServletActionContext;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import se.microcode.cogwork.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EventsHelper
{
    public static Courses fetchCourses(String url, HttpRetrievalService httpRetrievalService, XStream xstream)
    {
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

        Courses courses;

        try
        {
            courses = (Courses)cache.get(url);
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
                response = httpRetrievalService.get(url);
            }
            catch (IOException e)
            {
                return null;
                //return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att h&auml;mta den fr&aring;n den externa k&auml;llan{warning}");
            }

            if (response.getStatusCode() != 200)
            {
                return null;
                //return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att h&auml;mta den fr&aring;n den externa k&auml;llan (felkod " + response.getStatusCode() + "){warning}");
            }

            try
            {
                courses = (Courses)xstream.fromXML(response.getResponse());
            }
            catch (IOException e)
            {
                return null;
                //return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att h&auml;mta datan fr&aring;n den externa k&auml;llan{warning}");
            }
            catch (ConversionException e)
            {
                return null;
                //return wikiStyleRenderer.convertWikiToXHtml(renderContext, "{warning:title=Kunde inte visa kurslistan}Misslyckades att l&auml;sa fr&aring;n den externa k&auml;llan{warning}");
            }

            cache.put(url, courses);
        }

        return courses;
    }

    public static List<Event> processLimits(List<Event> events, int limitValue, EventState[] showStates)
    {
        if (limitValue >= 0)
        {
            long limitOffset = Long.valueOf(limitValue) * 1000;
            int stateMask = showStates != null ? buildStateMask(showStates) : 0;
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

        return events;
    }

    public static List<Event> processCategories(List<Event> events, int[] hideCategories, int[] showCategories)
    {
        if (hideCategories != null)
        {
            List<Event> newEvents = new ArrayList<Event>();

            for (Event event : events)
            {
                boolean found = false;
                for(int pin : hideCategories)
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

        if (showCategories != null)
        {
            List<Event> newEvents = new ArrayList<Event>();

            for (Event event : events)
            {
                boolean found = false;
                for(int pin : showCategories)
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

        return events;
    }

    public static List<Event> processStates(List<Event> events, EventState[] hideStates, EventState[] showStates)
    {
        if (hideStates != null)
        {
            int stateMask = buildStateMask(hideStates);
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

        if (showStates != null)
        {
            int stateMask = buildStateMask(showStates);
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

        return events;
    }

    public static List<Event> processLevels(List<Event> events, int minLevel, int maxLevel)
    {
        if (minLevel >= 0)
        {
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

        if (maxLevel >= 0)
        {
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

        return events;
    }

    public static List<Event> processStarted(List<Event> events, StartedState started)
    {
        if (started != StartedState.DONT_CARE)
        {
            boolean filter = started == StartedState.YES;
            Date now = Calendar.getInstance().getTime();

            List<Event> newEvents = new ArrayList<Event>();
            for (Event event : events)
            {
                if (event.schedule == null || event.schedule.startDate == null)
                {
                    continue;
                }

                boolean hasStarted = event.schedule.startDate.compareTo(now) >= 0;
                if ((!hasStarted && !filter) || (hasStarted && filter))
                {
                    continue;
                }

                newEvents.add(event);
            }
            events = newEvents;
        }

        return events;
    }

    public static int buildStateMask(EventState[] states)
    {
        int mask = 0;
        for (EventState state : states)
        {
            mask |= 1 << state.ordinal();
        }

        return mask;
    }
}
