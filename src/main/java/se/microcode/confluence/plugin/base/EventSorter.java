package se.microcode.confluence.plugin.base;

import se.microcode.cogwork.Event;
import se.microcode.cogwork.RegistrationPeriods;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public class EventSorter implements Comparator<Event>
{
    private SortOrder key;

    public EventSorter(SortOrder key)
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
