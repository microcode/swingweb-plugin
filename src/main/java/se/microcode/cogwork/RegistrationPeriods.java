package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.TimestampConverter;

import java.util.Date;

public class RegistrationPeriods
{
    @XStreamConverter(TimestampConverter.class)
    public Date startShowing;
    @XStreamConverter(TimestampConverter.class)
    public Date startInterestReg;
    @XStreamConverter(TimestampConverter.class)
    public Date startOpenReg;
    @XStreamConverter(TimestampConverter.class)
    public Date startDirectReg;
    @XStreamConverter(TimestampConverter.class)
    public Date startLateReg;
    @XStreamConverter(TimestampConverter.class)
    public Date closeReg;
    @XStreamConverter(TimestampConverter.class)
    public Date stopShowing;
}
