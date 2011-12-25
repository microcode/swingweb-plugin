package se.microcode.confluence.plugin.base;

import se.microcode.cogwork.Event;
import se.microcode.cogwork.RegistrationPeriods;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

class ShowSorter extends EventSorter
{
    @Override
    public Date getDate(Event o)
    {
        RegistrationPeriods periods = o.registrationPeriods;
        if (periods != null)
        {
            return periods.startShowing;
        }

        return Calendar.getInstance().getTime();
    }
}

class DirectSorter extends EventSorter
{
    @Override
    public Date getDate(Event o)
    {
        RegistrationPeriods periods = o.registrationPeriods;
        if (periods != null)
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

        return Calendar.getInstance().getTime();
    }
}

class LateSorter extends EventSorter
{
    @Override
    public Date getDate(Event o)
    {
        RegistrationPeriods periods = o.registrationPeriods;
        if (periods != null)
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

        return Calendar.getInstance().getTime();
    }
}

class HideSorter extends EventSorter
{
    @Override
    public Date getDate(Event o)
    {
        RegistrationPeriods periods = o.registrationPeriods;
        if (periods != null)
        {
            if (periods.stopShowing != null)
            {
                return periods.stopShowing;
            }
        }

        return Calendar.getInstance().getTime();
    }
}

abstract public class EventSorter implements Comparator<Event>
{
    public int compare(Event o1, Event o2)
    {
        Date d1 = getDate(o1);
        Date d2 = getDate(o2);

        return d1.compareTo(d2);
    }

    abstract public Date getDate(Event o);

    public static EventSorter createSorter(SortOrder key)
    {
        switch (key)
        {
            case SHOW:
                return new ShowSorter();
            case DIRECT:
                return new DirectSorter();
            case LATE:
                return new LateSorter();
            case HIDE:
                return new HideSorter();

        }
        return null;
    }
}
