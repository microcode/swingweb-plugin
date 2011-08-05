package se.microcode.cogwork.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import se.microcode.cogwork.Pricing;

public class PricingConverter implements Converter
{
    public boolean canConvert(Class clazz)
    {
        return clazz.equals(Pricing.class);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context)
    {

    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        Pricing pricing = new Pricing();
        while (reader.hasMoreChildren())
        {
            reader.moveDown();
            if (reader.getNodeName().equalsIgnoreCase("base"))
            {
                pricing.currency = reader.getAttribute("currency");
                pricing.value = Integer.valueOf(reader.getValue());
            }
            reader.moveUp();
        }
        return pricing;
    }
}
