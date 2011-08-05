package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.PricingConverter;

@XStreamConverter(PricingConverter.class)
public class Pricing
{
    public String currency;
    public int value;
}
