package se.microcode.cogwork.converters;

import com.atlassian.renderer.v2.macro.MacroException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import se.microcode.cogwork.Level;

public class LevelConverter implements Converter
{
    public boolean canConvert(Class clazz)
    {
        return clazz.equals(Level.class);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context)
    {
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        Level level = new Level();

        level.minValue = Integer.valueOf(reader.getAttribute("minValue"));
        level.categoryId = reader.getAttribute("categoryId");

        level.name = reader.getValue();

        return level;
    }
}
