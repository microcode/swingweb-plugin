package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import se.microcode.cogwork.converters.InstructorConverter;

@XStreamAlias("instructor")
@XStreamConverter(InstructorConverter.class)
public class Instructor
{
    public String personId;
    public String nickname;
    public String name;
}
