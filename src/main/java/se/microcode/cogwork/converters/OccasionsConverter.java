package se.microcode.cogwork.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class OccasionsConverter implements Converter
{
    public boolean canConvert(Class clazz)
    {
        return clazz.equals(Integer.class);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context)
    {

    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        return new Integer(reader.getAttribute("cardinality"));
    }
}
