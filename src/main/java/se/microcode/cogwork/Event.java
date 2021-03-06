package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.TimestampConverter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XStreamAlias("event")
public class Event
{
    @XStreamAsAttribute
    public int eventId;

    public String uid;
    public String source;
    public String title;
    public String organizer;
    @XStreamConverter(TimestampConverter.class)
    public Date created;
    public String author;
    public String category;
    public String place;
    public Pricing pricing;
    public RegistrationPeriods registrationPeriods;
    public Registration registration;
    public Schedule schedule;

    public List<Category> categories;
    public List<Level> requirements;
    Instructors instructors;

    public List<Team> teams;

    public String longdescription;

    public int getEventId()
    {
        return eventId;
    }

    public String getTitle()
    {
        return title;
    }

    public List<String> getInstructors()
    {
        List<String> result = new ArrayList<String>();
        if (instructors != null)
        {
            for (Instructor instructor : instructors.instructors)
            {
                result.add(instructor.name);
            }
        }
        return result;
    }

    public String parseShortDate(Date date)
    {
        DateFormat format = new SimpleDateFormat("d/M");
        return format.format(date);
    }

    public String parseDate(Date date)
    {
        StringBuilder builder = new StringBuilder();

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);

        builder.append(day).append(day < 3 ? ":a " : ":e ");
        switch (month)
        {
            case 0: builder.append("Januari"); break;
            case 1: builder.append("Februari"); break;
            case 2: builder.append("Mars"); break;
            case 3: builder.append("April"); break;
            case 4: builder.append("Maj"); break;
            case 5: builder.append("Juni"); break;
            case 6: builder.append("Juli"); break;
            case 7: builder.append("Augusti"); break;
            case 8: builder.append("September"); break;
            case 9: builder.append("Oktober"); break;
            case 10: builder.append("November"); break;
            case 11: builder.append("December"); break;
        }

        return builder.toString();
    }

    public String parseLongDate(Date date)
    {
        String base = parseDate(date);

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        return base + " " + Integer.toString(year);
    }

    public String getShortCourseDate()
    {
        if (schedule == null) {
            return "";
        }

        if (schedule.startDate == null) {
            return "";
        }

        return parseShortDate(schedule.startDate);

    }

    public String getCourseDate()
    {
        StringBuilder builder = new StringBuilder();

        if (schedule == null) {
            return builder.toString();
        }

        if (schedule.startDate == null) {
            return builder.toString();
        }

        return parseDate(schedule.startDate);
    }

    public String getLongCourseDate()
    {
        StringBuilder builder = new StringBuilder();

        if (schedule == null)
        {
            return builder.toString();
        }

        if (schedule.startDate == null)
        {
            return builder.toString();
        }

        return parseLongDate(schedule.startDate);
    }

    public String getStartDate()
    {
        if (registrationPeriods != null)
        {
            if (registrationPeriods.startDirectReg != null)
            {
                return parseShortDate(registrationPeriods.startDirectReg);
            }
            else if (registrationPeriods.startOpenReg != null)
            {
                return parseShortDate(registrationPeriods.startOpenReg);
            }
            else if (registrationPeriods.startInterestReg != null)
            {
                return parseShortDate(registrationPeriods.startInterestReg);
            }
            else
            {
                return parseShortDate(registrationPeriods.startShowing);
            }
        }
        return "";
    }

    public String getDirectDate(int offset)
    {
        if (registrationPeriods != null)
        {
            if (registrationPeriods.startDirectReg != null)
            {
                return parseShortDate(new Date(registrationPeriods.startDirectReg.getTime() + offset));
            }
            else if (registrationPeriods.startLateReg != null)
            {
                return parseShortDate(new Date(registrationPeriods.startLateReg.getTime() + offset));
            }
            else if (registrationPeriods.stopShowing != null)
            {
                return parseShortDate(new Date(registrationPeriods.stopShowing.getTime() + offset));
            }
        }

        return "";
    }

    public String getLateDate(int offset)
     {
         if (registrationPeriods != null)
         {
             if (registrationPeriods.startLateReg != null)
             {
                 return parseShortDate(new Date(registrationPeriods.startLateReg.getTime() + offset));
             }
             else if (registrationPeriods.stopShowing != null)
             {
                 return parseShortDate(new Date(registrationPeriods.stopShowing.getTime() + offset));
             }
         }

         return "";
     }

    public String getEndDate()
    {
        if (registrationPeriods != null)
        {
            if (registrationPeriods.startLateReg != null)
            {
                return parseShortDate(registrationPeriods.startLateReg);
            }
            else if (registrationPeriods.closeReg != null)
            {
                return parseShortDate(registrationPeriods.closeReg);
            }
            else
            {
                return parseShortDate(registrationPeriods.stopShowing);
            }
        }

        return "";
    }

    public String getDayAndTime()
    {
        if (schedule != null)
        {
            return schedule.dayAndTime;
        }
        return null;
    }

    public int getOccasions()
    {
        if (schedule != null)
        {
            return schedule.occasions;
        }
        return 1;
    }

    public String getPrice()
    {
        Pattern pricePattern = Pattern.compile(".*Pris: *(.*)\\n?.*");
        String desc = longdescription.replaceAll("<br */?>", "\n");

        Matcher m = pricePattern.matcher(desc);
        if (m.find())
        {
            return m.group(1).trim();
        }

        if (pricing != null)
        {
            return Integer.toString(pricing.value) + ":-";
        }

        return "";
    }

    public int getEventLevel()
    {
        if (requirements != null)
        {
            return requirements.get(0).minValue;
        }

        return 0;
    }

    public EventState getEventState()
    {
        Date now = Calendar.getInstance().getTime();

        if (registrationPeriods == null)
        {
            return EventState.HIDDEN;
        }

        Date lateReg = registrationPeriods.startLateReg != null ? registrationPeriods.startLateReg : now;
        Date closeReg = registrationPeriods.closeReg != null ? registrationPeriods.closeReg : lateReg;
        Date startShowing = registrationPeriods.startShowing != null ? registrationPeriods.startShowing : now;
        Date stopShowing = registrationPeriods.stopShowing != null ? registrationPeriods.stopShowing : closeReg;

        if (now.compareTo(stopShowing) >= 0)
        {
            return EventState.CLOSED;
        }

        if (now.compareTo(closeReg) >= 0)
        {
            return EventState.CLOSED;
        }

        if (registrationPeriods.startLateReg != null && now.compareTo(registrationPeriods.startLateReg) >= 0)
        {
            return EventState.LATE;
        }

        if (registrationPeriods.startOpenReg != null && now.compareTo(registrationPeriods.startOpenReg) >= 0)
        {
            return EventState.OPEN;
        }

        if (registrationPeriods.startDirectReg != null && now.compareTo(registrationPeriods.startDirectReg) >= 0)
        {
            return EventState.DIRECT;
        }

        if (registrationPeriods.startInterestReg != null && now.compareTo(registrationPeriods.startInterestReg) >= 0)
        {
            return EventState.INTEREST;
        }

        if (now.compareTo(startShowing) < 0)
        {
            return EventState.HIDDEN;
        }

        if (registrationPeriods.startShowing != null && now.compareTo(registrationPeriods.startShowing) >= 0)
        {
            if (registrationPeriods.startOpenReg == null && registrationPeriods.startInterestReg == null)
            {
                return EventState.OPEN;
            }
        }

        return EventState.UNOPENED;
    }

    public List<Team> getTeams()
    {
        if (teams != null)
        {
            return teams;
        }
        return new ArrayList<Team>();
    }

    public int getTeamCount()
    {
        return teams != null ? teams.size() : 0;
    }

    public String getLongDescription()
    {
        Pattern pricePattern = Pattern.compile("Pris:(.*)");
        Pattern linkPattern = Pattern.compile("Link:(.*)");

        String desc = longdescription.replaceAll("<br */?>", "\n");
        desc = desc.replaceAll("Pris: *(.*)\\n?", "");
        desc = desc.replaceAll("Link: *(.*)\\n?", "");
        desc = desc.replaceAll("\\n\\n", "\n");

        return desc.trim();
    }

    public String getEventLink()
    {
        if (registration != null) {
            if (registration.url != null) {
                return registration.url;
            }
        }

        return "#";
    }

    public String getCustomLink()
    {
        Pattern linkPattern = Pattern.compile(".*Link: *(.*)\\n?.*");
        String desc = longdescription.replaceAll("<br */?>", "\n");

        Matcher m;

        m = linkPattern.matcher(desc);
        if (m.find())
        {
            return m.group(1).trim();
        }

        return getEventLink();
    }
}

