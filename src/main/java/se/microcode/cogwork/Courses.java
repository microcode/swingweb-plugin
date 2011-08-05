package se.microcode.cogwork;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

public class Courses
{
    public String language;
    public String retrieved;
    public Session session;
    public Request request;

    @XStreamAlias("events")
    public EventContainer events;
}
