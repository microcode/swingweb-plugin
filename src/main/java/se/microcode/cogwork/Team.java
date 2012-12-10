package se.microcode.cogwork;

import java.util.List;

public class Team
{
    public String classType;
    public List<String> names;
    public List<String> clubs;
    public String state;

    public String getClassType()
    {
        return classType;
    }

    public List<String> getNames()
    {
        return names;
    }

    public int getNameCount()
    {
        return names.size();
    }

    public List<String> getClubs()
    {
        return clubs;
    }

    public int getClubCount()
    {
        return clubs.size();
    }

    public String getState()
    {
        return state;
    }
}
