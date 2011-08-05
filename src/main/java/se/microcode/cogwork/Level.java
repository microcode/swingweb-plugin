package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.LevelConverter;

@XStreamAlias("level")
@XStreamConverter(LevelConverter.class)
public class Level
{
    public int minValue;
    public String categoryId;
    public String name;
}
