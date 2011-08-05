package se.microcode.cogwork.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import se.microcode.cogwork.Instructor;

public class InstructorConverter implements Converter
{
    public boolean canConvert(Class clazz)
    {
        return clazz.equals(Instructor.class);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context)
    {

    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        Instructor instructor = new Instructor();

        instructor.personId = reader.getAttribute("personId");
        while (reader.hasMoreChildren())
        {
            reader.moveDown();
            if (reader.getNodeName().equalsIgnoreCase("name"))
            {
                instructor.nickname = reader.getAttribute("nickname");
                instructor.name = reader.getValue();
            }
            reader.moveUp();
        }

        return instructor;
    }
}
