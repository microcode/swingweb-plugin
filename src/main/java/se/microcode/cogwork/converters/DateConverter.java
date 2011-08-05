package se.microcode.cogwork.converters;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter implements Converter
{
    public boolean canConvert(Class clazz)
    {
        return clazz.equals(Date.class);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context)
    {

    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        Date date = null;
        try
        {
            date = formatter.parse(reader.getValue());
        }
        catch (ParseException e)
        {
            throw new ConversionException(e.getMessage(), e);
        }
        return date;
    }
}
