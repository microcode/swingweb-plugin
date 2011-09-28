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
    public static final int STATE_UNOPENED = 0;
    public static final int STATE_INTEREST = 1;
    public static final int STATE_DIRECT = 2;
    public static final int STATE_OPEN = 3;
    public static final int STATE_LATE = 4;
    public static final int STATE_CLOSED = 5;
    public static final int STATE_HIDDEN = 6;

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
    public Schedule schedule;

    public List<Category> categories;
    public List<Level> requirements;
    Instructors instructors;

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

        builder.append(day + (day < 3 ? ":a " : ":e "));
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

    public String getShortCourseDate()
    {
        do
        {
            if (schedule == null)
            {
                break;
            }

            if (schedule.startDate == null)
            {
                break;
            }

            return parseShortDate(schedule.startDate);
        }
        while (false);

        return "";
    }

    public String getCourseDate()
    {
        StringBuilder builder = new StringBuilder();

        do
        {
            if (schedule == null)
            {
                break;
            }

            if (schedule.startDate == null)
            {
                break;
            }

            return parseDate(schedule.startDate);
        }
        while (false);

        return builder.toString();
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

    public int getEventState()
    {
        Date now = Calendar.getInstance().getTime();

        if (registrationPeriods == null)
        {
            return STATE_HIDDEN;
        }

        Date lateReg = registrationPeriods.startLateReg != null ? registrationPeriods.startLateReg : now;
        Date closeReg = registrationPeriods.closeReg != null ? registrationPeriods.closeReg : lateReg;
        Date startShowing = registrationPeriods.startShowing != null ? registrationPeriods.startShowing : now;
        Date stopShowing = registrationPeriods.stopShowing != null ? registrationPeriods.stopShowing : closeReg;

        if (now.compareTo(stopShowing) >= 0)
        {
            return STATE_CLOSED;
        }

        if (now.compareTo(closeReg) >= 0)
        {
            return STATE_CLOSED;
        }

        if (registrationPeriods.startLateReg != null && now.compareTo(registrationPeriods.startLateReg) >= 0)
        {
            return STATE_LATE;
        }

        if (registrationPeriods.startOpenReg != null && now.compareTo(registrationPeriods.startOpenReg) >= 0)
        {
            return STATE_OPEN;
        }

        if (registrationPeriods.startDirectReg != null && now.compareTo(registrationPeriods.startDirectReg) >= 0)
        {
            return STATE_DIRECT;
        }

        if (registrationPeriods.startInterestReg != null && now.compareTo(registrationPeriods.startInterestReg) >= 0)
        {
            return STATE_INTEREST;
        }

        if (now.compareTo(startShowing) < 0)
        {
            return STATE_HIDDEN;
        }

        if (registrationPeriods.startOpenReg == null && registrationPeriods.startInterestReg == null && registrationPeriods.startShowing != null && now.compareTo(registrationPeriods.startShowing) >= 0)
        {
            return STATE_OPEN;
        }

        return STATE_UNOPENED;
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
        switch (getEventState())
        {
            case STATE_CLOSED:
            case STATE_HIDDEN:
            case STATE_UNOPENED:
                return "http://www.swingweb.org/tools/reg/?org=nsw;eventId=" + getEventId() + ";info=1";
        }

        return "http://www.swingweb.org/tools/reg/?org=nsw;eventId=" + getEventId() + ";info=0";
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

