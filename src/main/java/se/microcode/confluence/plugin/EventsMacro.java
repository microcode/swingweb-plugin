package se.microcode.confluence.plugin;

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

    public enum CompareKey
    {
        SHOW_DATE,
        DIRECT_DATE,
        LATE_DATE,
        HIDE_DATE
    };

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

    private static final String TYPE_PARAM = "type";
    private static final String URL_PARAM = "url";
    private static final String HIDECATEGORIES_PARAM = "hide_categories";
    private static final String SHOWCATEGORIES_PARAM = "show_categories";
    private static final String HIDESTATES_PARAM = "hide_states";
    private static final String SHOWSTATES_PARAM = "show_states";
    private static final String MINLEVEL_PARAM = "minLevel";
    private static final String MAXLEVEL_PARAM = "maxLevel";
    private static final String LIMIT_PARAM = "limit";
    private static final String SORT_PARAM ="sort";
    private static final String STARTED_PARAM = "started";

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        String typeParam = (String)params.get(TYPE_PARAM);
        String urlParam = (String)params.get(URL_PARAM);
        String hideCategoriesParam = (String)params.get(HIDECATEGORIES_PARAM);
        String showCategoriesParam = (String)params.get(SHOWCATEGORIES_PARAM);
        String hideStatesParam = (String)params.get(HIDESTATES_PARAM);
        String showStatesParam = (String)params.get(SHOWSTATES_PARAM);
        String minLevelParam = (String)params.get(MINLEVEL_PARAM);
        String maxLevelParam = (String)params.get(MAXLEVEL_PARAM);
        String limitParam = (String)params.get(LIMIT_PARAM);
        String sortParam = (String)params.get(SORT_PARAM);
        String startedParam = (String)params.get(STARTED_PARAM);

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

        if (urlParam == null)
        {
            throw new MacroException("No URL source specified");
        }

        try
        {
            courses = (Courses)cache.get(urlParam);
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
                response = httpRetrievalService.get(urlParam);
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

            cache.put(urlParam, courses);
        }

        List<Event> events = new ArrayList<Event>(courses.events.entries);
        {
            if (limitParam != null)
            {
                long limitOffset = Long.valueOf(limitParam) * 1000;
                int stateMask = showStatesParam != null ? buildStateMask(showStatesParam.split(";")) : 0;
                Date limit = new Date(Calendar.getInstance().getTimeInMillis() + limitOffset);

                List<Event> newEvents = new ArrayList<Event>();

                for (Event event : events)
                {
                    RegistrationPeriods periods = event.registrationPeriods;
                    int state = event.getEventState();

                    if (periods == null)
                    {
                        continue;
                    }

                    boolean filtered = false;
                    if ((stateMask & (1 << state)) != 0)
                    {
                        switch (state)
                        {
                            case Event.STATE_HIDDEN:
                            case Event.STATE_OPEN:
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

                            case Event.STATE_DIRECT:
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

            if (hideCategoriesParam != null)
            {
                List<Event> newEvents = new ArrayList<Event>();

                String[] categories = hideCategoriesParam.split(";");
                for (Event event : events)
                {
                    boolean found = false;
                    for(String pin : categories)
                    {
                        for (Category haystack : event.categories)
                        {
                            if (haystack.categoryId.equalsIgnoreCase(pin))
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

            if (showCategoriesParam != null)
            {
                List<Event> newEvents = new ArrayList<Event>();

                String[] categories = showCategoriesParam.split(";");
                for (Event event : events)
                {
                    boolean found = false;
                    for(String pin : categories)
                    {
                        for (Category haystack : event.categories)
                        {
                            if (haystack.categoryId.equalsIgnoreCase(pin))
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

            if (hideStatesParam != null)
            {
                int stateMask = buildStateMask(hideStatesParam.split(";"));
                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    if ((stateMask & (1 << event.getEventState())) != 0)
                    {
                        continue;
                    }

                    newEvents.add(event);
                }

                events = newEvents;
            }

            if (showStatesParam != null)
            {
                int stateMask = buildStateMask(showStatesParam.split(";"));
                List<Event> newEvents = new ArrayList<Event>();
                for (Event event : events)
                {
                    int state = event.getEventState();

                    if ((stateMask & (1 << state)) == 0)
                    {
                        continue;
                    }

                    newEvents.add(event);
                }

                events = newEvents;
            }

            if (minLevelParam != null)
            {
                int minLevel = Integer.valueOf(minLevelParam);
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

            if (maxLevelParam != null)
            {
                int maxLevel = Integer.valueOf(maxLevelParam);
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

            if (startedParam != null)
            {
                boolean filter = "yes".equalsIgnoreCase(startedParam);
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

        if (sortParam != null)
        {
            Sorter sorter = null;

            if ("show".equalsIgnoreCase(sortParam))
            {
                sorter = new Sorter(CompareKey.SHOW_DATE);
            }
            else if ("direct".equalsIgnoreCase(sortParam))
            {
                sorter = new Sorter(CompareKey.DIRECT_DATE);
            }
            else if ("late".equalsIgnoreCase(sortParam))
            {
                sorter = new Sorter(CompareKey.LATE_DATE);
            }
            else if ("hide".equalsIgnoreCase(sortParam))
            {
                sorter = new Sorter(CompareKey.HIDE_DATE);
            }
            else
            {
                throw new MacroException("Invalid sort key");
            }

            Collections.sort(events, sorter);
        }

        if ("velocity".equalsIgnoreCase(typeParam))
        {
            Map context = MacroUtils.defaultVelocityContext();
            context.put("events", events);

            builder.append(VelocityUtils.getRenderedContent(body, context));
        }
        else if ("wiki".equalsIgnoreCase(typeParam) || typeParam == null)
        {
        }
        else
        {
        }

        return builder.toString();
    }

    private int buildStateMask(String[] states)
    {
        int mask = 0;
        for (String state : states)
        {
            if (state.equalsIgnoreCase("unopened"))
            {
                mask |= 1 << Event.STATE_UNOPENED;
            }
            else if (state.equalsIgnoreCase("interest"))
            {
                mask |= 1 << Event.STATE_INTEREST;
            }
            else if (state.equalsIgnoreCase("direct"))
            {
                mask |= 1 << Event.STATE_DIRECT;
            }
            else if (state.equalsIgnoreCase("open"))
            {
                mask |= 1 << Event.STATE_OPEN;
            }
            else if (state.equalsIgnoreCase("late"))
            {
                mask |= 1 << Event.STATE_LATE;
            }
            else if (state.equalsIgnoreCase("closed"))
            {
                mask |= 1 << Event.STATE_CLOSED;
            }
            else if (state.equalsIgnoreCase("hidden"))
            {
                mask |= 1 << Event.STATE_HIDDEN;
            }
        }

        return mask;
    }

    class Sorter implements Comparator<Event>
    {
        private CompareKey key;

        public Sorter(CompareKey key)
        {
            this.key = key;
        }

        public int compare(Event o1, Event o2)
        {
            Date d1 = getDate(o1, key);
            Date d2 = getDate(o2, key);

            return d1.compareTo(d2);
        }

        private Date getDate(Event o1, CompareKey key)
        {
            if (o1.registrationPeriods == null)
            {
                return Calendar.getInstance().getTime();
            }

            RegistrationPeriods periods = o1.registrationPeriods;

            switch (key)
            {
                case SHOW_DATE:
                {
                    return periods.startShowing;
                }

                case DIRECT_DATE:
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

                case LATE_DATE:
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

                case HIDE_DATE:
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
