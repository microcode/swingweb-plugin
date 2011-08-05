package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

public class Instructors
{
    public String combinedTitle;

    @XStreamImplicit(itemFieldName="instructor")
    List<Instructor> instructors;
}
