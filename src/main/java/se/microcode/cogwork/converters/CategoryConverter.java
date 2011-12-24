package se.microcode.cogwork.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import se.microcode.cogwork.Category;

public class CategoryConverter implements Converter
{
    public boolean canConvert(Class clazz)
    {
        return clazz.equals(Category.class);
    }

    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context)
    {
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        Category category = new Category();

        category.categoryId = reader.getAttribute("categoryId");
        int categoryOffset = category.categoryId.lastIndexOf('_');
        if (categoryOffset >= 0)
        {
            category.categoryIndex = Integer.valueOf(category.categoryId.substring(categoryOffset+1));
        }
        category.name = reader.getValue();

        return category;
    }
}
