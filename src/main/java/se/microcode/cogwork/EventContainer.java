package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

public class EventContainer
{
    public Search search;

    @XStreamImplicit(itemFieldName = "event")
    public List<Event> entries;
}
