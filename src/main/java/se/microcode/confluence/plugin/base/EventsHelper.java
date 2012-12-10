package se.microcode.confluence.plugin.base;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.confluence.spaces.actions.AddTeamLabelToSpaceAction;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.spring.container.ContainerManager;
import com.opensymphony.webwork.ServletActionContext;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.microcode.cogwork.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            }

            if (response.getStatusCode() != 200)
            {
                return null;
            }

            try
            {
                courses = (Courses)xstream.fromXML(response.getResponse());
            }
            catch (IOException e)
            {
                return null;
            }
            catch (ConversionException e)
            {
                return null;
            }

            cache.put(url, courses);
        }

        return courses;
    }

    public static List<Event> fetchRegistrations(String url, HttpRetrievalService httpRetrievalService)
    {
        CacheFactory cacheFactory = (CacheFactory) ContainerManager.getComponent("cacheManager");
        Cache cache = cacheFactory.getCache("se.nackswinget.confluence.plugin.courses");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        Pattern eventRegex = Pattern.compile("event_([0-9]+)");
        Pattern nameRegex = Pattern.compile("(.+?)(?:, (.+?))? & (.+)");

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

        List<Event> events = null;
        try
        {
            events = (List<Event>)cache.get(url);
        }
        catch (ClassCastException e)
        {
            events = null;
        }

        HttpResponse response;
        try
        {
            response = httpRetrievalService.get(url);
        }
        catch (IOException e)
        {
            return null;
        }

        if (response.getStatusCode() != 200)
        {
            return null;
        }

        Document dom;
        try
        {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(response.getResponse());
        }
        catch (ParserConfigurationException e)
        {
            return null;
        }
        catch (SAXException e)
        {
            return null;
        }
        catch (IOException e)
        {
            return null;
        }

        events = new ArrayList<Event>();
        try
        {
            Element doc = dom.getDocumentElement();
            Element competitions = (Element)doc.getElementsByTagName("tbody").item(0);

            Event current = null;

            NodeList rows = competitions.getElementsByTagName("tr");
            for (int i = 0, n = rows.getLength(); i != n; ++i)
            {
                Element row = (Element)rows.item(i);

                if (row.hasAttributes() && row.hasAttribute("id"))
                {
                    if (current != null)
                    {
                        events.add(current);
                    }

                    String idText = row.getAttribute("id");
                    Matcher eventMatcher = eventRegex.matcher(idText);
                    eventMatcher.find();
                    int eventId = Integer.valueOf(eventMatcher.group(1));

                    current = new Event();
                    current.eventId = eventId;
                    current.teams = new ArrayList<Team>();
                    continue;
                }

                if (current == null)
                {
                    continue;
                }

                NodeList columns = row.getElementsByTagName("td");
                if (columns.getLength() < 4)
                {
                    continue;
                }

                String teamText = columns.item(1).getChildNodes().item(0).getNodeValue();
                Matcher nameMatcher = nameRegex.matcher(teamText);
                nameMatcher.find();
                List<String> nameMatches = new ArrayList<String>();
                for (int j = 1, m = nameMatcher.groupCount(); j <= m; ++j)
                {
                    String nameMatch = nameMatcher.group(j);
                    if (nameMatch == null)
                    {
                        continue;
                    }
                    nameMatches.add(nameMatch);
                }

                List<String> clubMatches = new ArrayList<String>();
                NodeList clubs = ((Element)columns.item(2)).getElementsByTagName("span");
                for (int j = 0, m = clubs.getLength(); j != m; ++j)
                {
                    clubMatches.add(clubs.item(j).getChildNodes().item(0).getNodeValue());
                }

                Team team = new Team();
                team.classType = columns.item(0).getChildNodes().item(0).getNodeValue();
                team.names = nameMatches;
                team.clubs = clubMatches;
                team.state = columns.item(3).getChildNodes().item(0).getNodeValue();
                current.teams.add(team);
            }
        }
        catch (NullPointerException e)
        {
            return null;
        }

        cache.put(url, events);
        return events;
    }

    public static void mergeWithRegistrations(Courses courses, List<Event> events)
    {
        Map<Integer, Event> eventXref = new HashMap<Integer, Event>();
        for (Event event : events)
        {
            eventXref.put(event.eventId, event);
        }

        for (Event course : courses.events.entries)
        {
            if (eventXref.containsKey(course.eventId))
            {
                course.teams = eventXref.get(course.eventId).teams;
            }
        }
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
