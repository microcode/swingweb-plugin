package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.DateConverter;
import se.microcode.cogwork.converters.OccasionsConverter;

import java.util.Date;

public class Schedule
{
    @XStreamConverter(DateConverter.class)
    public Date startDate;
    public String dayAndTime;
    @XStreamConverter(OccasionsConverter.class)
    public int occasions;
}
