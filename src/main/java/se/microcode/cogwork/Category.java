package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.CategoryConverter;

@XStreamAlias("category")
@XStreamConverter(CategoryConverter.class)
public class Category
{
    public String categoryId;
    public int categoryIndex;
    public String name;
}
